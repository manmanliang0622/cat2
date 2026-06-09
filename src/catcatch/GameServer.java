package catcatch;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class GameServer {
    private static final int DEFAULT_PORT = 5050;
    private static final int GAME_SECONDS = 45;
    private static final int RETURN_SECONDS = 10;
    private static final Random RANDOM = new Random();

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Player> playersById = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4, r -> {
        Thread t = new Thread(r, "catcatch-room-task");
        t.setDaemon(true);
        return t;
    });
    private final AtomicLong idCounter = new AtomicLong(1);

    // ── Entry points ──────────────────────────────────────────────────────────

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        if (args.length > 0) port = Integer.parseInt(args[0]);
        System.out.println("CatCatch server starting on port " + port + " …");
        ServerSocket ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(new InetSocketAddress("0.0.0.0", port));
        new GameServer().accept(ss);
    }

    public static EmbeddedServer startEmbedded(int port) throws IOException {
        ServerSocket ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(new InetSocketAddress("0.0.0.0", port));
        GameServer server = new GameServer();
        Thread t = new Thread(() -> {
            try { server.accept(ss); } catch (IOException ignored) {}
        }, "catcatch-server");
        t.setDaemon(true);
        t.start();
        return new EmbeddedServer(ss, server);
    }

    public record EmbeddedServer(ServerSocket socket, GameServer server) {
        public void stop() { try { socket.close(); } catch (IOException ignored) {} }
        /** 傳回目前所有可被搜尋到的房間資訊（供 UDP Discovery 使用）。 */
        public List<RoomInfo> getAvailableRooms() { return server.getAvailableRooms(); }
    }

    /** 傳回所有非空、非遊戲中（或遊戲中）的房間資訊，供 RoomDiscoveryServer 使用。 */
    List<RoomInfo> getAvailableRooms() {
        List<RoomInfo> result = new ArrayList<>();
        synchronized (rooms) {
            for (Room r : rooms.values()) {
                synchronized (r) {
                    if (r.players.isEmpty()) continue;
                    String hostName = (r.hostId != null && r.players.containsKey(r.hostId))
                            ? r.players.get(r.hostId).name : "未知";
                    result.add(new RoomInfo(r.code, hostName, "",
                            DEFAULT_PORT, r.players.size(),
                            r.status == RoomStatus.PLAYING));
                }
            }
        }
        return result;
    }

    // ── Accept loop ───────────────────────────────────────────────────────────

    private void accept(ServerSocket ss) throws IOException {
        try {
            while (!ss.isClosed()) {
                try {
                    Socket s = ss.accept();
                    s.setTcpNoDelay(true);
                    new ClientSession(s).start();
                } catch (IOException e) {
                    if (ss.isClosed()) break;
                }
            }
        } finally {
            try { ss.close(); } catch (IOException ignored) {}
        }
    }

    // ── Join / action helpers ─────────────────────────────────────────────────

    private JoinResult joinRoom(Protocol.Message msg, ClientSession session) {
        synchronized (rooms) {
            String name = msg.get("name").isBlank() ? "玩家" : msg.get("name");
            boolean create = "1".equals(msg.get("create"));
            String reqRoom = msg.get("room").toUpperCase();

            if (!create && reqRoom.isBlank())
                return new JoinResult(Protocol.encode("ERROR", "message", "請先輸入房號。"), null, null);

            Room room = create ? createRoom() : rooms.get(reqRoom);
            if (room == null)
                return new JoinResult(Protocol.encode("ERROR", "message", "找不到這個房間，請確認房號是否正確。"), null, null);

            synchronized (room) {
                if (room.status == RoomStatus.PLAYING)
                    return new JoinResult(Protocol.encode("ERROR", "message", "這個房間正在遊戲中。"), null, null);

                Player player = new Player(nextId("p"), name, room.code, session);
                room.players.put(player.id, player);
                playersById.put(player.id, player);
                if (room.hostId == null) room.hostId = player.id;
                room.message = name + " 已加入房間。";
                return new JoinResult(Protocol.encode("JOINED",
                    "playerId", player.id, "room", room.code), room, player);
            }
        }
    }

    private ActionResult handleAction(Protocol.Message msg) {
        String playerId = msg.get("playerId");
        if (playerId.isBlank())
            return new ActionResult(Protocol.encode("ERROR", "message", "缺少玩家身分。"), null);

        Player player = playersById.get(playerId);
        if (player == null)
            return new ActionResult(Protocol.encode("ERROR", "message", "找不到此玩家，請重新加入。"), null);

        Room room = rooms.get(player.roomCode);
        if (room == null) {
            playersById.remove(playerId);
            return new ActionResult(Protocol.encode("ERROR", "message", "房間不存在。"), null);
        }

        synchronized (room) {
            String response = switch (msg.type()) {
                case "READY"     -> onReady(room, player, "1".equals(msg.get("value")));
                case "START"     -> onStart(room, player);
                case "CLICK"     -> onClick(room, player, msg.get("id"));
                case "PLAY_AGAIN"-> onPlayAgain(room, player);
                case "BACK_LOBBY"-> onBackLobby(room);
                case "LEAVE"     -> onLeave(room, player);
                default          -> Protocol.encode("ERROR", "message", "未知指令。");
            };
            return new ActionResult(response, room);
        }
    }

    private void handleDisconnect(String playerId) {
        if (playerId == null || playerId.isBlank()) return;
        Player player = playersById.get(playerId);
        if (player == null) return;
        Room room = rooms.get(player.roomCode);
        if (room == null) { playersById.remove(playerId); return; }
        synchronized (room) { removePlayer(room, player); }
        broadcastState(room);
    }

    // ── Room operations ───────────────────────────────────────────────────────

    private Room createRoom() {
        String code;
        do { code = randomCode(); } while (rooms.containsKey(code));
        Room r = new Room(code);
        rooms.put(code, r);
        return r;
    }

    private String onReady(Room room, Player player, boolean ready) {
        if (room.status != RoomStatus.LOBBY)
            return Protocol.encode("ERROR", "message", "只有在大廳才能切換準備狀態。");
        player.ready = ready;
        room.message = player.name + (ready ? " 準備好了！" : " 取消準備。");
        return Protocol.encode("OK", "message", room.message);
    }

    private String onStart(Room room, Player player) {
        if (!player.id.equals(room.hostId))
            return Protocol.encode("ERROR", "message", "只有房主可以開始遊戲。");
        if (room.status != RoomStatus.LOBBY)
            return Protocol.encode("ERROR", "message", "遊戲狀態不正確，請稍候。");
        if (room.players.size() < 1 || room.players.values().stream().anyMatch(p -> !p.ready))
            return Protocol.encode("ERROR", "message", "請等所有玩家都準備完成。");
        startCountdown(room);
        return Protocol.encode("OK", "message", room.message);
    }

    /**
     * 開始 3 秒倒數（COUNTDOWN 狀態）。
     * 倒數期間不生成貓咪、不計算分數；3 秒後自動呼叫 startGame()。
     */
    private void startCountdown(Room room) {
        room.status = RoomStatus.COUNTDOWN;
        room.countdownEndMillis = System.currentTimeMillis() + 3_000L;
        room.message = "遊戲即將開始！";
        room.countdownTask = scheduler.schedule(() -> {
            synchronized (room) {
                if (room.status == RoomStatus.COUNTDOWN) startGame(room);
            }
            broadcastState(room);
        }, 3, TimeUnit.SECONDS);
    }

    private String onClick(Room room, Player player, String entityId) {
        // 倒數期間靜默忽略點擊，不扣分也不報錯
        if (room.status == RoomStatus.COUNTDOWN)
            return Protocol.encode("OK", "message", "");
        if (room.status != RoomStatus.PLAYING)
            return Protocol.encode("ERROR", "message", "遊戲尚未開始。");
        Entity entity = room.entities.remove(entityId);
        if (entity == null) return Protocol.encode("OK", "message", "");
        if (entity.kind == EntityKind.DOG) {
            player.score -= 15;
            room.message = player.name + " 點到地雷 (狗)，扣 15 分！";
        } else if (entity.variant.equals(room.targetVariant)) {
            player.score += 10;
            room.message = player.name + " 抓到目標貓咪，加 10 分！";
        } else {
            player.score -= 5;
            room.message = player.name + " 點錯小貓，扣 5 分。";
        }
        return Protocol.encode("OK", "message", room.message);
    }

    private String onPlayAgain(Room room, Player player) {
        room.replayVotes.add(player.id);
        if (room.replayVotes.containsAll(room.players.keySet()))
            returnLobby(room, true, "全員同意再來一局！");
        else
            room.message = player.name + " 想再玩一局。(" + room.replayVotes.size() + "/" + room.players.size() + ")";
        return Protocol.encode("OK", "message", room.message);
    }

    private String onBackLobby(Room room) {
        returnLobby(room, false, "已返回房間。");
        return Protocol.encode("OK", "message", room.message);
    }

    private String onLeave(Room room, Player player) {
        removePlayer(room, player);
        return Protocol.encode("OK", "message", "已離開房間。");
    }

    private void startGame(Room room) {
        room.shutdownAll();
        room.status = RoomStatus.PLAYING;
        room.entities.clear();
        room.targetVariant = randomVariant(null);
        room.gameEndMillis = System.currentTimeMillis() + GAME_SECONDS * 1_000L;
        room.returnAtMillis = 0L;
        room.replayVotes.clear();
        for (Player p : room.players.values()) { p.score = 0; }
        room.message = "遊戲開始！";

        room.tickTask = scheduler.scheduleAtFixedRate(() -> tick(room), 0, 150, TimeUnit.MILLISECONDS);
        room.spawnTask = scheduler.scheduleAtFixedRate(() -> {
            synchronized (room) { if (room.status == RoomStatus.PLAYING) spawnWave(room); }
            broadcastState(room);
        }, 0, 850, TimeUnit.MILLISECONDS);
        room.targetTask = scheduler.scheduleAtFixedRate(() -> {
            synchronized (room) {
                if (room.status == RoomStatus.PLAYING) {
                    room.targetVariant = randomVariant(room.targetVariant);
                    room.message = "目標已切換！快看上方提示。";
                }
            }
            broadcastState(room);
        }, 5, 5, TimeUnit.SECONDS);
    }

    private void tick(Room room) {
        synchronized (room) {
            long now = System.currentTimeMillis();
            if (room.status == RoomStatus.PLAYING) {
                room.entities.values().removeIf(e -> e.expireAtMillis <= now);
                if (now >= room.gameEndMillis) finishGame(room, "時間到，計算分數中…");
            } else if (room.status == RoomStatus.FINISHED && now >= room.returnAtMillis) {
                returnLobby(room, false, "倒數結束，已自動回到房間。");
            } else if (room.status == RoomStatus.LOBBY) {
                room.shutdownTick();
            }
        }
        broadcastState(room);
    }

    private void spawnWave(Room room) {
        int elapsed = GAME_SECONDS - (int)Math.max(0,
            Math.ceil((room.gameEndMillis - System.currentTimeMillis()) / 1_000.0));
        int cats = Math.min(8, 4 + elapsed / 10);
        int dogs = (4 + elapsed / 10) >= 6 ? 2 : 1;
        int total = cats + dogs;

        // 網格分區生成座標，確保貓咪均勻分散、不重疊
        List<double[]> positions = generateGridPositions(total, room.entities);
        int posIdx = 0;

        for (int i = 0; i < cats; i++) {
            String variant = RANDOM.nextDouble() < 0.35 ? room.targetVariant : randomVariant(room.targetVariant);
            String id = nextId("c");
            double[] pos = posIdx < positions.size() ? positions.get(posIdx++) : fallbackPos();
            room.entities.put(id, new Entity(id, EntityKind.CAT, variant,
                pos[0], pos[1],
                100 + RANDOM.nextDouble() * 30,
                System.currentTimeMillis() + 2800 + RANDOM.nextInt(1000)));
        }
        for (int i = 0; i < dogs; i++) {
            String id = nextId("d");
            double[] pos = posIdx < positions.size() ? positions.get(posIdx++) : fallbackPos();
            room.entities.put(id, new Entity(id, EntityKind.DOG, "",
                pos[0], pos[1],
                105 + RANDOM.nextDouble() * 25,
                System.currentTimeMillis() + 2800 + RANDOM.nextInt(1000)));
        }
    }

    /**
     * 網格分區 + 格內隨機偏移 + 最小距離檢查，
     * 生成均勻分散的正規化座標（0.0~1.0）。
     *
     * 步驟：
     *   1. 將畫面切成 COLS × ROWS 個格子並打亂順序
     *   2. 依序取格，在格內加小偏移作為候選位置
     *   3. 若候選點與現有實體距離 < MIN_DIST 則跳過此格
     *   4. 格子用完後若還不夠，以寬鬆隨機補足
     */
    private List<double[]> generateGridPositions(int count, Map<String, Entity> existing) {
        final int    COLS     = 5;    // 欄數（橫向）
        final int    ROWS     = 3;    // 列數（縱向）
        final double MARGIN_X = 0.07; // 左右邊界留白（正規化）
        final double MARGIN_Y = 0.08; // 上下邊界留白
        final double CELL_W   = (1.0 - 2 * MARGIN_X) / COLS;
        final double CELL_H   = (1.0 - 2 * MARGIN_Y) / ROWS;
        // 格內最大抖動比例（讓位置看起來自然，不完全置中）
        final double JITTER   = 0.32;
        // 最小距離（正規化；約等於畫面寬度的 13%，通常 >120px）
        final double MIN_DIST = 0.13;

        // 收集場上現有實體的位置
        List<double[]> occupied = new ArrayList<>();
        for (Entity e : existing.values()) occupied.add(new double[]{e.x(), e.y()});

        // 建立格子清單並打亂
        List<int[]> cells = new ArrayList<>();
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                cells.add(new int[]{r, c});
        Collections.shuffle(cells, RANDOM);

        List<double[]> result = new ArrayList<>();
        for (int[] cell : cells) {
            if (result.size() >= count) break;

            // 格子中心 + 隨機偏移
            double cx = MARGIN_X + (cell[1] + 0.5) * CELL_W;
            double cy = MARGIN_Y + (cell[0] + 0.5) * CELL_H;
            double x  = cx + (RANDOM.nextDouble() * 2 - 1) * CELL_W * JITTER;
            double y  = cy + (RANDOM.nextDouble() * 2 - 1) * CELL_H * JITTER;

            // 夾在安全邊界內
            x = Math.max(MARGIN_X, Math.min(1.0 - MARGIN_X, x));
            y = Math.max(MARGIN_Y, Math.min(1.0 - MARGIN_Y, y));

            // 距離檢查：不能與現有位置太近
            if (tooClose(x, y, occupied, MIN_DIST)) continue;

            result.add(new double[]{x, y});
            occupied.add(new double[]{x, y});
        }

        // 格子不足時用較寬鬆的隨機補足（最多嘗試 40 次）
        for (int tries = 0; result.size() < count && tries < 40; tries++) {
            double[] p = fallbackPos();
            if (!tooClose(p[0], p[1], occupied, MIN_DIST * 0.65)) {
                result.add(p);
                occupied.add(p);
            }
        }

        return result;
    }

    /** 判斷 (x, y) 是否與 occupied 中任何一點距離 < minDist。 */
    private static boolean tooClose(double x, double y, List<double[]> occupied, double minDist) {
        for (double[] o : occupied) {
            double dx = x - o[0], dy = y - o[1];
            if (dx * dx + dy * dy < minDist * minDist) return true;
        }
        return false;
    }

    /** 在邊界內完全隨機的備用位置（格子用完時才呼叫）。 */
    private double[] fallbackPos() {
        return new double[]{0.07 + RANDOM.nextDouble() * 0.86,
                            0.08 + RANDOM.nextDouble() * 0.84};
    }

    private void finishGame(Room room, String msg) {
        room.shutdownPlay();
        room.status = RoomStatus.FINISHED;
        room.entities.clear();
        room.returnAtMillis = System.currentTimeMillis() + RETURN_SECONDS * 1_000L;
        room.message = msg;
    }

    private void returnLobby(Room room, boolean ready, String msg) {
        room.shutdownAll();
        room.status = RoomStatus.LOBBY;
        room.entities.clear();
        room.replayVotes.clear();
        room.returnAtMillis = 0L;
        room.targetVariant = "GRAY";
        for (Player p : room.players.values()) { p.ready = ready; p.score = 0; }
        room.message = msg;
    }

    private void removePlayer(Room room, Player player) {
        room.players.remove(player.id);
        room.replayVotes.remove(player.id);
        playersById.remove(player.id);
        player.session = null;

        if (player.id.equals(room.hostId))
            room.hostId = room.players.keySet().stream().findFirst().orElse(null);

        if (room.players.isEmpty()) {
            room.shutdownAll();
            rooms.remove(room.code);
            return;
        }
        if (room.status == RoomStatus.PLAYING && room.players.size() == 1)
            finishGame(room, "其他玩家離線，提前結算。");
        else if (room.status == RoomStatus.COUNTDOWN)
            // 倒數期間有玩家離線，取消倒數並返回大廳
            returnLobby(room, false, player.name + " 離線，倒數取消。");
        else
            room.message = player.name + " 已離開房間。";
    }

    // ── Broadcast ─────────────────────────────────────────────────────────────

    private void broadcastState(Room room) {
        List<Delivery> deliveries = new ArrayList<>();
        synchronized (room) {
            if (room.players.isEmpty()) return;
            for (Player p : room.players.values()) {
                if (p.session == null || !p.session.isOpen()) continue;
                deliveries.add(new Delivery(p.session, buildState(room, p)));
            }
        }
        for (Delivery d : deliveries) d.session.send(d.payload);
    }

    private String buildState(Room room, Player self) {
        List<Player> ordered = room.players.values().stream()
            .sorted(Comparator.comparingInt((Player p) -> p.score).reversed().thenComparing(p -> p.name))
            .toList();

        List<String[]> pRows = new ArrayList<>();
        for (Player p : ordered)
            pRows.add(new String[]{ p.id, p.name, Integer.toString(p.score),
                p.ready ? "1" : "0", p.session != null && p.session.isOpen() ? "1" : "0" });

        List<String[]> eRows = new ArrayList<>();
        for (Entity e : room.entities.values())
            eRows.add(new String[]{ e.id, e.kind.name(), e.variant,
                Double.toString(e.x), Double.toString(e.y), Double.toString(e.size) });

        int remaining = 0, returnSec = 0, countdownSec = 0;
        if (room.status == RoomStatus.PLAYING)
            remaining = (int)Math.max(0, Math.ceil((room.gameEndMillis - System.currentTimeMillis()) / 1_000.0));
        else if (room.status == RoomStatus.FINISHED)
            returnSec = (int)Math.max(0, Math.ceil((room.returnAtMillis - System.currentTimeMillis()) / 1_000.0));
        else if (room.status == RoomStatus.COUNTDOWN)
            countdownSec = (int)Math.max(0, Math.ceil((room.countdownEndMillis - System.currentTimeMillis()) / 1_000.0));

        return Protocol.encode("STATE",
            "selfId", self.id,
            "room", room.code,
            "status", room.status.name().toLowerCase(),
            "hostId", room.hostId == null ? "" : room.hostId,
            "target", room.targetVariant,
            "remaining", Integer.toString(remaining),
            "returnSeconds", Integer.toString(returnSec),
            "countdownSeconds", Integer.toString(countdownSec),
            "message", room.message,
            "players", Protocol.encodeList(pRows),
            "objects", Protocol.encodeList(eRows));
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private String randomCode() {
        String alpha = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) sb.append(alpha.charAt(RANDOM.nextInt(alpha.length())));
        return sb.toString();
    }

    private String randomVariant(String exclude) {
        List<String> v = List.of("GRAY", "GOLD", "SNOW");
        String c;
        do { c = v.get(RANDOM.nextInt(v.size())); } while (c.equals(exclude) && v.size() > 1);
        return c;
    }

    private String nextId(String prefix) { return prefix + Long.toString(idCounter.getAndIncrement(), 36); }

    // ── Inner types ───────────────────────────────────────────────────────────

    private final class ClientSession implements Runnable {
        private final Socket socket;
        private final BufferedReader reader;
        private final BufferedWriter writer;
        private final Object writerLock = new Object();
        private volatile boolean open = true;
        private volatile String playerId;

        ClientSession(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        void start() {
            Thread t = new Thread(this, "catcatch-client-" + socket.getPort());
            t.setDaemon(true);
            t.start();
        }

        @Override
        public void run() {
            try {
                String first = reader.readLine();
                if (first == null) return;
                Protocol.Message joinMsg = Protocol.parse(first);
                if (!"JOIN".equals(joinMsg.type())) {
                    send(Protocol.encode("ERROR", "message", "第一個封包必須是 JOIN。"));
                    return;
                }
                JoinResult jr = joinRoom(joinMsg, this);
                send(jr.response);
                if (jr.player == null || jr.room == null) return;
                playerId = jr.player.id;
                broadcastState(jr.room);

                String line;
                while (open && (line = reader.readLine()) != null) {
                    Protocol.Message msg = Protocol.parse(line);
                    ActionResult ar = handleAction(msg);
                    if (ar.response != null && !ar.response.isBlank()) send(ar.response);
                    if (ar.room != null) broadcastState(ar.room);
                }
            } catch (IOException ignored) {
            } finally {
                open = false;
                handleDisconnect(playerId);
                close();
            }
        }

        void send(String payload) {
            if (!open || payload == null || payload.isBlank()) return;
            synchronized (writerLock) {
                try { writer.write(payload); writer.newLine(); writer.flush(); }
                catch (IOException e) { open = false; }
            }
        }

        boolean isOpen() { return open && !socket.isClosed(); }

        private void close() {
            try { reader.close(); } catch (IOException ignored) {}
            try { writer.close(); } catch (IOException ignored) {}
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private static final class Room {
        final String code;
        final Map<String, Player> players = new LinkedHashMap<>();
        final Map<String, Entity> entities = new HashMap<>();
        final Set<String> replayVotes = ConcurrentHashMap.newKeySet();
        RoomStatus status = RoomStatus.LOBBY;
        String hostId, targetVariant = "GRAY", message = "等待玩家準備。";
        long gameEndMillis, returnAtMillis, countdownEndMillis;
        ScheduledFuture<?> tickTask, spawnTask, targetTask, countdownTask;

        Room(String code) { this.code = code; }

        void shutdownPlay() {
            if (spawnTask  != null) { spawnTask.cancel(false);  spawnTask  = null; }
            if (targetTask != null) { targetTask.cancel(false); targetTask = null; }
        }
        void shutdownTick()      { if (tickTask     != null) { tickTask.cancel(false);     tickTask     = null; } }
        void shutdownCountdown() { if (countdownTask != null) { countdownTask.cancel(false); countdownTask = null; } }
        void shutdownAll()       { shutdownPlay(); shutdownTick(); shutdownCountdown(); }
    }

    private enum RoomStatus { LOBBY, COUNTDOWN, PLAYING, FINISHED }
    private enum EntityKind  { CAT, DOG }

    private record Entity(String id, EntityKind kind, String variant,
                           double x, double y, double size, long expireAtMillis) {}

    private static final class Player {
        final String id, name, roomCode;
        boolean ready;
        int score;
        ClientSession session;
        Player(String id, String name, String roomCode, ClientSession session) {
            this.id = id; this.name = name; this.roomCode = roomCode; this.session = session;
        }
    }

    private record JoinResult(String response, Room room, Player player) {}
    private record ActionResult(String response, Room room) {}
    private record Delivery(ClientSession session, String payload) {}
}
