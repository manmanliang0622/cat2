package catcatch;

import java.io.File;
import java.io.FileInputStream;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Enumeration;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class CatCatchApp extends Application {

    // Dynamically set from actual screen size in start()
    static int WIDTH  = 1100;
    static int HEIGHT = 700;
    static final int SERVER_PORT = 5050;

    static Theme   theme        = Theme.PINK;
    static boolean soundEnabled = true;
    static boolean musicEnabled = true;
    static float   volume       = 0.7f;
    static final SynthAudio audio = new SynthAudio();
    static GameServer.EmbeddedServer embeddedServer  = null;
    static RoomDiscoveryServer       discoveryServer = null;

    static CatCatchApp instance;
    private Stage stage;

    /** 全域 CSS 樣式表路徑（讓每個 Scene 都能掛上）。 */
    static String globalCss = null;

    @Override
    public void init() {
        instance = this;
        // 在 JavaFX 啟動的最早期載入字體，讓 CSS font-family 能解析到名稱
        loadJfFont();
        // 準備 CSS 路徑（相對路徑轉換為 URI 字串）
        File cssFile = new File("assets/style.css");
        if (cssFile.exists()) globalCss = cssFile.toURI().toString();
    }

    /** 載入 jf-openhuninn 字體至 JavaFX 字體系統（多尺寸確保各情境都已預熱）。 */
    private static void loadJfFont() {
        String path = "assets/fonts/jf-openhuninn.ttf";
        int[] sizes = {12, 14, 16, 18, 20, 24, 28, 32, 48, 72, 100, 138};
        for (int sz : sizes) {
            try (FileInputStream fis = new FileInputStream(path)) {
                Font.loadFont(fis, sz);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        // Read actual screen dimensions and go fullscreen
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        WIDTH  = (int) screen.getWidth();
        HEIGHT = (int) screen.getHeight();

        stage.setTitle("抓小貓 ─ 多人連線版");
        stage.setResizable(true);

        // Show window first, then set fullscreen so it centres correctly
        goMainMenu();
        stage.show();

        // Maximize to fill the screen without the macOS fullscreen mode
        // (which hides the menu bar and requires an extra click to exit)
        stage.setX(screen.getMinX());
        stage.setY(screen.getMinY());
        stage.setWidth(WIDTH);
        stage.setHeight(HEIGHT);

        stage.setOnCloseRequest(e -> { audio.shutdown(); stopServer(); stopDiscovery(); Platform.exit(); System.exit(0); });

        // F key toggles true fullscreen
        stage.getScene().setOnKeyPressed(ke -> {
            if (ke.getCode() == javafx.scene.input.KeyCode.F) {
                stage.setFullScreen(!stage.isFullScreen());
            }
            if (ke.getCode() == javafx.scene.input.KeyCode.ESCAPE && stage.isFullScreen()) {
                stage.setFullScreen(false);
            }
        });
    }

    void goMainMenu() {
        stopServer();
        stopDiscovery();
        setScene(MainMenuScene.build(this));
        audio.startBgm();
    }

    /** 導覽到 UDP 搜尋房間場景。 */
    void goRoomBrowse(String playerName) {
        setScene(RoomBrowseScene.build(this, playerName));
    }

    /** 啟動 UDP Discovery 伺服器（房主建立房間後呼叫）。 */
    static void startDiscovery() {
        stopDiscovery();
        if (embeddedServer == null) return;
        try {
            RoomDiscoveryServer ds = new RoomDiscoveryServer(
                    embeddedServer::getAvailableRooms, SERVER_PORT);
            ds.start();
            discoveryServer = ds;
        } catch (Exception e) {
            System.err.println("[Discovery] 無法啟動 UDP 搜尋伺服器：" + e.getMessage());
        }
    }

    /** 停止 UDP Discovery 伺服器。 */
    static void stopDiscovery() {
        if (discoveryServer != null) { discoveryServer.stop(); discoveryServer = null; }
    }

    void goSettings() { setScene(SettingsScene.build(this)); }

    void goLobby(GameClient client, boolean isHost) {
        setScene(LobbyScene.build(this, client, isHost));
    }

    void goGame(GameClient client) { setScene(GameScene.build(this, client)); }

    void goResult(GameClient.GameState state, GameClient client, boolean isHost) {
        setScene(ResultScene.build(this, state, client, isHost));
    }

    private void setScene(Scene s) {
        // 掛上全域字體 CSS（若尚未掛上）
        if (globalCss != null && !s.getStylesheets().contains(globalCss))
            s.getStylesheets().add(0, globalCss); // index 0 = 最低優先權，讓 inline style 仍可覆蓋
        stage.setScene(s);
        // Re-attach F-key listener after scene change
        s.setOnKeyPressed(ke -> {
            if (ke.getCode() == javafx.scene.input.KeyCode.F) {
                stage.setFullScreen(!stage.isFullScreen());
            }
            if (ke.getCode() == javafx.scene.input.KeyCode.ESCAPE && stage.isFullScreen()) {
                stage.setFullScreen(false);
            }
        });
    }

    static void stopServer() {
        stopDiscovery();
        if (embeddedServer != null) { embeddedServer.stop(); embeddedServer = null; }
    }

    static String getLocalIP() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                if (isZeroTierIface(ni)) continue; // 跳過 ZeroTier，留給 getZeroTierIP()
                Enumeration<java.net.InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress a = addrs.nextElement();
                    if (a instanceof Inet4Address && !a.isLoopbackAddress()) return a.getHostAddress();
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    /** 傳回 ZeroTier 虛擬網路的 IP，若未安裝 / 未啟動則回傳 null。 */
    static String getZeroTierIP() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                if (!isZeroTierIface(ni)) continue;
                Enumeration<java.net.InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress a = addrs.nextElement();
                    if (a instanceof Inet4Address && !a.isLoopbackAddress()) return a.getHostAddress();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 判斷是否為 ZeroTier 虛擬網路介面。
     *   zt*    → Linux / 舊版 macOS ZeroTier
     *   feth*  → 新版 macOS ZeroTier（fake ethernet）
     *   顯示名稱含 "zerotier" → Windows
     */
    static boolean isZeroTierIface(NetworkInterface ni) {
        String name = ni.getName() == null ? "" : ni.getName().toLowerCase();
        String disp = ni.getDisplayName() == null ? "" : ni.getDisplayName().toLowerCase();
        return name.startsWith("zt")
            || name.startsWith("feth")      // macOS ZeroTier 新版介面
            || disp.contains("zerotier")
            || disp.contains("zero tier");
    }

    public static void main(String[] args) { launch(args); }
}
