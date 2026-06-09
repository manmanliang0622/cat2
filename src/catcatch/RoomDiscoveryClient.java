package catcatch;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/**
 * 玩家端 UDP Discovery 客戶端。
 *
 * 向區域網路廣播 "DISCOVER_CATCATCH"，
 * 收集 2 秒內所有房主回傳的 RoomInfo，
 * 最後透過 JavaFX Platform.runLater 回傳結果。
 *
 * 使用方式：
 *   RoomDiscoveryClient.searchAsync(
 *       rooms -> updateListView(rooms),
 *       err   -> showError(err));
 */
public class RoomDiscoveryClient {

    private static final int TIMEOUT_MS = 2500; // 等待回應的時間（毫秒）

    /**
     * 非同步搜尋區域網路房間。
     * 搜尋在背景執行緒進行，結果在 FX 執行緒回傳。
     *
     * @param onResult 搜尋完成時呼叫（FX thread），傳入找到的房間清單（可能空）
     * @param onError  發生嚴重錯誤時呼叫（FX thread）
     */
    public static void searchAsync(Consumer<List<RoomInfo>> onResult,
                                   Consumer<String>         onError) {
        Thread t = new Thread(() -> {
            List<RoomInfo> found = new ArrayList<>();
            Set<String>    seen  = new HashSet<>();

            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                socket.setSoTimeout(TIMEOUT_MS);

                // ── 發送廣播 ──────────────────────────────────────────────
                byte[] req = RoomDiscoveryServer.DISCOVER_MAGIC.getBytes(StandardCharsets.UTF_8);

                // 同時廣播到 255.255.255.255 以及每個網卡的子網廣播位址
                Set<InetAddress> targets = getBroadcastAddresses();
                targets.add(InetAddress.getByName("255.255.255.255"));

                for (InetAddress target : targets) {
                    try {
                        socket.send(new DatagramPacket(req, req.length,
                                target, RoomDiscoveryServer.DISCOVERY_PORT));
                    } catch (Exception ignored) {}
                }

                // ── 收集回應直到逾時 ──────────────────────────────────────
                byte[] buf      = new byte[512];
                long   deadline = System.currentTimeMillis() + TIMEOUT_MS;

                while (System.currentTimeMillis() < deadline) {
                    try {
                        DatagramPacket resp = new DatagramPacket(buf, buf.length);
                        socket.receive(resp);

                        String text = new String(resp.getData(), 0, resp.getLength(),
                                StandardCharsets.UTF_8).trim();
                        RoomInfo info = RoomDiscoveryServer.parseResponse(text, resp.getAddress());

                        if (info != null) {
                            // 用 ip:port:code 去重複（同一台主機可能有多個網卡都回應）
                            String key = info.ip() + ":" + info.port() + ":" + info.roomCode();
                            if (seen.add(key)) found.add(info);
                        }
                    } catch (SocketTimeoutException e) {
                        break; // 等待逾時，結束收集
                    }
                }

            } catch (Exception e) {
                String msg = "搜尋房間時發生錯誤：" + e.getMessage();
                javafx.application.Platform.runLater(() -> onError.accept(msg));
                return;
            }

            List<RoomInfo> result = Collections.unmodifiableList(found);
            javafx.application.Platform.runLater(() -> onResult.accept(result));

        }, "catcatch-discovery-client");
        t.setDaemon(true);
        t.start();
    }

    // ── 工具：取得所有子網廣播位址 ───────────────────────────────────────────

    private static Set<InetAddress> getBroadcastAddresses() {
        Set<InetAddress> addrs = new HashSet<>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress bcast = ia.getBroadcast();
                    if (bcast != null) addrs.add(bcast);
                }
            }
        } catch (Exception ignored) {}
        return addrs;
    }
}
