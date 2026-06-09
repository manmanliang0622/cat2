package catcatch;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/**
 * 玩家端 UDP Discovery 客戶端。
 *
 * 同時使用兩種策略搜尋房間：
 *   1. UDP 廣播（適合同一 Wi-Fi / LAN）
 *   2. ZeroTier 子網路逐 IP 掃描（廣播在 ZeroTier 不可靠，改直送每台主機）
 *
 * 兩種策略並行發出，統一在同一個 socket 收集 TIMEOUT_MS 內的所有回應。
 */
public class RoomDiscoveryClient {

    private static final int TIMEOUT_MS   = 3000; // 等待回應時間（毫秒）

    public static void searchAsync(Consumer<List<RoomInfo>> onResult,
                                   Consumer<String>         onError) {
        Thread t = new Thread(() -> {
            List<RoomInfo> found = new ArrayList<>();
            Set<String>    seen  = new HashSet<>();

            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                socket.setSoTimeout(TIMEOUT_MS);

                byte[] req = RoomDiscoveryServer.DISCOVER_MAGIC.getBytes(StandardCharsets.UTF_8);

                // ── 策略 1：廣播（同一 Wi-Fi / LAN）────────────────────────
                Set<InetAddress> broadcasts = getBroadcastAddresses();
                broadcasts.add(InetAddress.getByName("255.255.255.255"));
                for (InetAddress target : broadcasts) {
                    trySend(socket, req, target);
                }

                // ── 策略 2：ZeroTier 子網路逐 IP 掃描 ────────────────────
                // ZeroTier 是 L3 overlay，廣播封包不一定能跨越，
                // 改為對 ZeroTier 介面的 /24 子網路裡每個 IP 直接送出。
                sendToZeroTierSubnets(socket, req);

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
                            String key = info.ip() + ":" + info.port() + ":" + info.roomCode();
                            if (seen.add(key)) found.add(info);
                        }
                    } catch (SocketTimeoutException e) {
                        break;
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

    // ── ZeroTier 子網路掃描 ───────────────────────────────────────────────────

    /**
     * 偵測 ZeroTier 虛擬網路介面，對其 /8~/24 子網路的每個 IP 送出 UDP 封包。
     *
     * ZeroTier 介面識別方式：
     *   macOS/Linux：介面名稱以 "zt" 開頭（例如 zt3jn3abcd）
     *   Windows：顯示名稱包含 "ZeroTier"
     *
     * 對 /24 範圍（最多 254 台主機）逐一送出，UDP send 不等待回應，非常快速。
     */
    private static void sendToZeroTierSubnets(DatagramSocket socket, byte[] req) {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                if (!isZeroTier(ni)) continue;

                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    if (!(ia.getAddress() instanceof Inet4Address)) continue;
                    byte[] base = ia.getAddress().getAddress();
                    int prefix  = ia.getNetworkPrefixLength();

                    // 只掃 /8 ~ /24，超過 /24 的子網路太大不掃
                    if (prefix > 24 || prefix < 8) continue;

                    // 固定掃同 /24 區段（base.base.base.1 ~ .254）
                    for (int i = 1; i <= 254; i++) {
                        byte[] target = base.clone();
                        target[3] = (byte) i;
                        trySend(socket, req, InetAddress.getByAddress(target));
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    /** 判斷是否為 ZeroTier 虛擬介面。 */
    private static boolean isZeroTier(NetworkInterface ni) {
        String name    = ni.getName() == null ? "" : ni.getName().toLowerCase();
        String display = ni.getDisplayName() == null ? "" : ni.getDisplayName().toLowerCase();
        return name.startsWith("zt")          // macOS / Linux
            || display.contains("zerotier")   // Windows
            || display.contains("zero tier");
    }

    // ── 廣播位址工具 ─────────────────────────────────────────────────────────

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

    private static void trySend(DatagramSocket socket, byte[] data, InetAddress target) {
        try {
            socket.send(new DatagramPacket(data, data.length,
                    target, RoomDiscoveryServer.DISCOVERY_PORT));
        } catch (Exception ignored) {}
    }
}
