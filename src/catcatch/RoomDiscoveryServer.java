package catcatch;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;

/**
 * 房主端 UDP Discovery 伺服器。
 *
 * 監聽 UDP port 5051，收到 "DISCOVER_CATCATCH" 廣播後，
 * 回傳目前所有 LOBBY 中房間的資訊給詢問方。
 *
 * 使用方式：
 *   RoomDiscoveryServer ds = new RoomDiscoveryServer(server::getAvailableRooms, SERVER_PORT);
 *   ds.start();
 *   // ... 結束時 ...
 *   ds.stop();
 */
public class RoomDiscoveryServer {

    /** UDP 搜尋用埠號（與遊戲 TCP 埠 5050 不同）。 */
    public static final int DISCOVERY_PORT = 5051;

    // 封包魔術字串
    static final String DISCOVER_MAGIC  = "DISCOVER_CATCATCH";
    static final String RESPONSE_MAGIC  = "CATCATCH_ROOM";
    // 回應格式：CATCATCH_ROOM|code|hostName|gamePort|playerCount|playing(0/1)
    static final char   SEP             = '|';

    private final Supplier<List<RoomInfo>> roomsSupplier;
    private final int gamePort;

    private volatile boolean running;
    private DatagramSocket socket;

    public RoomDiscoveryServer(Supplier<List<RoomInfo>> roomsSupplier, int gamePort) {
        this.roomsSupplier = roomsSupplier;
        this.gamePort      = gamePort;
    }

    /** 在背景執行緒啟動 UDP 監聽。 */
    public void start() throws SocketException {
        socket = new DatagramSocket(DISCOVERY_PORT);
        socket.setBroadcast(true);
        running = true;

        Thread t = new Thread(this::loop, "catcatch-discovery-srv");
        t.setDaemon(true);
        t.start();
    }

    /** 停止監聽並關閉 socket。 */
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) socket.close();
    }

    // ── 主要監聽迴圈 ──────────────────────────────────────────────────────────

    private void loop() {
        byte[] buf = new byte[256];
        while (running) {
            try {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                socket.receive(pkt);

                String msg = new String(pkt.getData(), 0, pkt.getLength(), StandardCharsets.UTF_8).trim();
                if (!DISCOVER_MAGIC.equals(msg)) continue;

                // 回傳所有可用房間
                for (RoomInfo info : roomsSupplier.get()) {
                    if (info.playing()) continue; // 遊戲中的房間不開放搜尋加入
                    byte[] resp = buildResponse(info).getBytes(StandardCharsets.UTF_8);
                    socket.send(new DatagramPacket(resp, resp.length,
                            pkt.getAddress(), pkt.getPort()));
                }
            } catch (Exception e) {
                if (running) System.err.println("[Discovery] 錯誤：" + e.getMessage());
            }
        }
    }

    // ── 封包格式工具 ──────────────────────────────────────────────────────────

    private String buildResponse(RoomInfo info) {
        // 簡單的 pipe-分隔格式；host name 中的 '|' 字元用底線替換
        return RESPONSE_MAGIC + SEP
                + sanitize(info.roomCode()) + SEP
                + sanitize(info.hostName()) + SEP
                + gamePort + SEP
                + info.playerCount() + SEP
                + (info.playing() ? "1" : "0");
    }

    /** 解析 discovery 回應封包，失敗時傳回 null。 */
    static RoomInfo parseResponse(String raw, InetAddress from) {
        if (raw == null || !raw.startsWith(RESPONSE_MAGIC)) return null;
        String[] parts = raw.split("\\" + SEP, -1);
        // 格式：CATCATCH_ROOM | code | host | port | players | playing
        if (parts.length < 6) return null;
        try {
            String code    = parts[1].trim();
            String host    = parts[2].trim();
            int    port    = Integer.parseInt(parts[3].trim());
            int    players = Integer.parseInt(parts[4].trim());
            boolean playing = "1".equals(parts[5].trim());
            if (code.isEmpty()) return null;
            return new RoomInfo(code, host.isEmpty() ? "未知" : host,
                    from.getHostAddress(), port, players, playing);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String sanitize(String s) {
        return s == null ? "" : s.replace("|", "_").replace("\n", "").replace("\r", "");
    }
}
