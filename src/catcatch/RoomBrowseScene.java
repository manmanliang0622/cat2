package catcatch;

import java.util.List;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.util.Duration;

/**
 * 搜尋附近房間的 JavaFX 場景。
 *
 * 玩家進入此畫面後會自動發起 UDP 廣播搜尋，
 * 結果顯示在列表中，點擊即可加入（不需手動輸入 IP）。
 */
public class RoomBrowseScene {

    /**
     * @param app      主應用程式（用於場景切換）
     * @param playerName 玩家名稱（從主選單帶入）
     */
    public static Scene build(CatCatchApp app, String playerName) {
        Theme t = CatCatchApp.theme;

        // ── 根節點 ────────────────────────────────────────────────────────────
        StackPane fullRoot = new StackPane();
        fullRoot.setStyle(t.rootStyle());

        Pane decoBg = new Pane();
        decoBg.setMouseTransparent(true);
        KawaiiDeco.addBackground(decoBg, t, CatCatchApp.WIDTH, CatCatchApp.HEIGHT);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:transparent;");
        root.setPadding(new Insets(28));

        fullRoot.getChildren().addAll(decoBg, root);

        // ── 頂部標題列 ────────────────────────────────────────────────────────
        HBox topBar = new HBox(14);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = MainMenuScene.iconBtn("返回", "home", 15, 120, t);
        backBtn.setOnAction(e -> app.goMainMenu());

        Label title = new Label("搜尋附近房間");
        title.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:" + t.accentDark + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 狀態訊息（搜尋中 / 找到 N 個 / 錯誤）
        Label statusLabel = new Label("正在搜尋…");
        statusLabel.setStyle("-fx-font-size:13px;-fx-text-fill:" + t.muted + ";");

        topBar.getChildren().addAll(backBtn, title, spacer, statusLabel);
        BorderPane.setMargin(topBar, new Insets(0, 0, 18, 0));
        root.setTop(topBar);

        // ── 主內容卡片 ────────────────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setStyle(t.cardStyle());
        card.setPadding(new Insets(0));
        card.setMaxWidth(700);
        card.setMinWidth(480);

        // 列表容器
        VBox roomList = new VBox(0);

        // 「空白」提示（搜尋完成但沒找到房間時顯示）
        VBox emptyBox = buildEmptyBox(t);
        emptyBox.setVisible(false);
        emptyBox.setManaged(false);

        // 「搜尋中」動畫提示
        HBox loadingBox = buildLoadingBox(t);

        ScrollPane scroll = new ScrollPane(roomList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;-fx-border-color:transparent;");
        scroll.setMinHeight(300);
        scroll.setPrefHeight(420);

        card.getChildren().addAll(loadingBox, emptyBox, scroll);

        // ── 底部按鈕列 ────────────────────────────────────────────────────────
        HBox bottomBar = new HBox(14);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(16, 0, 0, 0));

        // 手動輸入 IP 備用方案
        Button manualBtn = MainMenuScene.iconBtn("手動輸入 IP", "gear", 14, 180, t);
        manualBtn.setOnAction(e -> showManualJoinDialog(app, playerName, t));

        Button refreshBtn = MainMenuScene.primaryBtn("重新搜尋", 140, t);

        Region bot = new Region();
        HBox.setHgrow(bot, Priority.ALWAYS);
        bottomBar.getChildren().addAll(manualBtn, bot, refreshBtn);

        // ── 中央佈局 ──────────────────────────────────────────────────────────
        VBox centre = new VBox(0, card, bottomBar);
        VBox.setVgrow(card, Priority.ALWAYS);
        root.setCenter(centre);

        // ── 搜尋邏輯 ──────────────────────────────────────────────────────────
        final RoomInfo[] selected = {null};

        Runnable doSearch = () -> {
            selected[0] = null;
            roomList.getChildren().clear();
            emptyBox.setVisible(false);
            emptyBox.setManaged(false);
            loadingBox.setVisible(true);
            loadingBox.setManaged(true);
            statusLabel.setText("正在搜尋…");
            refreshBtn.setDisable(true);

            RoomDiscoveryClient.searchAsync(
                rooms -> {
                    loadingBox.setVisible(false);
                    loadingBox.setManaged(false);
                    refreshBtn.setDisable(false);

                    if (rooms.isEmpty()) {
                        statusLabel.setText("找不到房間");
                        emptyBox.setVisible(true);
                        emptyBox.setManaged(true);
                    } else {
                        statusLabel.setText("找到 " + rooms.size() + " 個房間");
                        populateRoomList(roomList, rooms, t, r -> selected[0] = r, app, playerName);
                    }
                },
                err -> {
                    loadingBox.setVisible(false);
                    loadingBox.setManaged(false);
                    refreshBtn.setDisable(false);
                    statusLabel.setText("搜尋失敗");
                    showError(emptyBox, err, t);
                }
            );
        };

        refreshBtn.setOnAction(e -> doSearch.run());

        // 場景建立後立即搜尋
        Platform.runLater(doSearch);

        // ── 建立場景 ──────────────────────────────────────────────────────────
        Scene scene = new Scene(fullRoot);
        decoBg.prefWidthProperty().bind(scene.widthProperty());
        decoBg.prefHeightProperty().bind(scene.heightProperty());
        return scene;
    }

    // ── 填入房間列表 ──────────────────────────────────────────────────────────

    private static void populateRoomList(VBox list, List<RoomInfo> rooms, Theme t,
                                          java.util.function.Consumer<RoomInfo> onSelect,
                                          CatCatchApp app, String playerName) {
        list.getChildren().clear();
        final RoomInfo[] cur = {null};

        for (int i = 0; i < rooms.size(); i++) {
            RoomInfo info = rooms.get(i);
            boolean lastRow = (i == rooms.size() - 1);

            HBox row = buildRoomRow(info, t);

            // 點擊高亮 + 記錄選取
            row.setOnMouseClicked(e -> {
                // 清除其他行的高亮
                list.getChildren().forEach(n -> n.setStyle(roomRowStyle(false, t)));
                row.setStyle(roomRowStyle(true, t));
                cur[0] = info;
                onSelect.accept(info);

                // 雙擊直接加入
                if (e.getClickCount() >= 2) joinRoom(app, playerName, info, null, null);
            });

            // 最右邊的「加入」按鈕
            Button joinBtn = makeJoinBtn(app, playerName, info, t);
            HBox.setMargin(joinBtn, new Insets(0, 16, 0, 0));

            Region rsp = new Region(); HBox.setHgrow(rsp, Priority.ALWAYS);
            row.getChildren().addAll(rsp, joinBtn);

            if (!lastRow) {
                // 分隔線
                Separator sep = new Separator();
                sep.setStyle("-fx-opacity:0.20;");
                list.getChildren().addAll(row, sep);
            } else {
                list.getChildren().add(row);
            }
        }
    }

    private static HBox buildRoomRow(RoomInfo info, Theme t) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16, 20, 16, 20));
        row.setStyle(roomRowStyle(false, t));

        // 房號徽章
        Label codeBadge = new Label(info.roomCode());
        codeBadge.setStyle(
            "-fx-font-size:18px;-fx-font-weight:bold;-fx-letter-spacing:3;" +
            "-fx-text-fill:" + t.accentDark + ";" +
            "-fx-background-color:" + t.accent + "40;" +
            "-fx-background-radius:10;-fx-padding:6 14;");

        // 房主名稱 + 狀態
        VBox info2 = new VBox(4);
        Label hostLbl = new Label("🏠 " + info.hostName());
        hostLbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + t.text + ";");

        Label statusBadge = new Label(info.statusText());
        statusBadge.setStyle(
            "-fx-font-size:11px;-fx-font-weight:bold;" +
            "-fx-background-radius:8;-fx-padding:2 10;" +
            (info.playing()
                ? "-fx-background-color:#FFCDD2;-fx-text-fill:#C03030;"
                : "-fx-background-color:" + t.mint + ";-fx-text-fill:#1E6040;"));

        // IP 小字
        Label ipLbl = new Label(info.ip() + ":" + info.port());
        ipLbl.setStyle("-fx-font-size:10px;-fx-text-fill:" + t.muted + ";");

        info2.getChildren().addAll(hostLbl, statusBadge, ipLbl);
        row.getChildren().addAll(codeBadge, info2);

        // 滑鼠懸停效果
        row.setOnMouseEntered(e -> { if (!row.getStyle().contains("border")) row.setStyle(roomRowHover(t)); });
        row.setOnMouseExited(e  -> row.setStyle(roomRowStyle(false, t)));

        return row;
    }

    private static Button makeJoinBtn(CatCatchApp app, String playerName, RoomInfo info, Theme t) {
        Button btn = new Button(info.playing() ? "觀看（不支援）" : "加入");
        btn.setPrefWidth(88);
        btn.setDisable(info.playing());
        btn.setStyle(t.primaryBtn());
        btn.setOnMouseEntered(e -> { if (!btn.isDisabled()) btn.setStyle(t.primaryBtnHover()); });
        btn.setOnMouseExited(e  -> { if (!btn.isDisabled()) btn.setStyle(t.primaryBtn()); });
        btn.setOnAction(e -> joinRoom(app, playerName, info, btn, null));
        return btn;
    }

    private static void joinRoom(CatCatchApp app, String playerName, RoomInfo info,
                                  Button btn, Label err) {
        if (btn != null) { btn.setDisable(true); btn.setText("連線中…"); }

        new Thread(() -> {
            final GameClient[] ref = {null};
            GameClient.Listener listener = new GameClient.Listener() {
                @Override public void onConnected(String id) {
                    Platform.runLater(() -> app.goLobby(ref[0], false));
                }
                @Override public void onState(GameClient.GameState s) {}
                @Override public void onError(String m) {
                    Platform.runLater(() -> {
                        if (btn != null) { btn.setDisable(false); btn.setText("加入"); }
                        showToast("連線失敗：" + m);
                    });
                }
                @Override public void onDisconnected(String r) {}
            };
            ref[0] = new GameClient(listener);
            try {
                ref[0].connectAndJoin(info.ip(), info.port(), playerName, info.roomCode(), false);
            } catch (Exception ex) {
                String msg = describeError(ex.getMessage());
                Platform.runLater(() -> {
                    if (btn != null) { btn.setDisable(false); btn.setText("加入"); }
                    showToast(msg);
                });
            }
        }, "room-join-thread").start();
    }

    // ── 輔助元件 ──────────────────────────────────────────────────────────────

    private static HBox buildLoadingBox(Theme t) {
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(32, 32);
        Label lbl = new Label("正在搜尋附近的房間…");
        lbl.setStyle("-fx-font-size:14px;-fx-text-fill:" + t.muted + ";");
        HBox box = new HBox(14, pi, lbl);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        return box;
    }

    private static VBox buildEmptyBox(Theme t) {
        Label icon = new Label("🐱");
        icon.setStyle("-fx-font-size:48px;");
        Label msg  = new Label("目前沒有找到任何房間");
        msg.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:" + t.text + ";");
        Label hint = new Label(
            "請確認以下事項後點擊「重新搜尋」：\n\n" +
            "• 房主已點「建立房間」並進入大廳\n" +
            "• 同一 Wi-Fi：確認連到相同路由器\n" +
            "• ZeroTier：雙方都已加入同一個 ZeroTier Network，\n" +
            "  且 ZeroTier 顯示為 OK（綠燈）\n\n" +
            "若仍搜尋不到，請點右下角「手動輸入 IP」，\n" +
            "輸入房主的 ZeroTier IP（10.x.x.x 格式）。");
        hint.setStyle("-fx-font-size:12px;-fx-text-fill:" + t.muted + ";-fx-text-alignment:left;");
        hint.setTextAlignment(javafx.scene.text.TextAlignment.LEFT);
        VBox box = new VBox(12, icon, msg, hint);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(48));
        return box;
    }

    private static void showError(VBox emptyBox, String err, Theme t) {
        emptyBox.setVisible(true);
        emptyBox.setManaged(true);
        if (!emptyBox.getChildren().isEmpty())
            ((Label) emptyBox.getChildren().get(1)).setText(err);
    }

    /** 手動輸入 IP 的備用對話框。 */
    private static void showManualJoinDialog(CatCatchApp app, String playerName, Theme t) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("手動輸入主機 IP");
        dlg.setHeaderText("輸入房主的 IP 位址與房號");

        TextField ipField   = new TextField(); ipField.setPromptText("192.168.x.x");
        TextField codeField = new TextField(); codeField.setPromptText("4 位數字房號");
        codeField.setTextFormatter(new TextFormatter<>(c -> {
            String s = c.getControlNewText();
            return s.matches("\\d{0,4}") ? c : null;
        }));

        VBox content = new VBox(10,
            new Label("主機 IP："), ipField,
            new Label("房間號碼："), codeField);
        content.setPadding(new Insets(16));
        dlg.getDialogPane().setContent(content);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            String ip   = ipField.getText().trim();
            String code = codeField.getText().trim();
            if (ip.isEmpty() || code.length() != 4) return;
            RoomInfo fake = new RoomInfo(code, "手動", ip, CatCatchApp.SERVER_PORT, 0, false);
            joinRoom(app, playerName, fake, null, null);
        });
    }

    private static void showToast(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.setTitle("連線失敗");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static String describeError(String raw) {
        if (raw == null) return "無法連線，請稍後再試。";
        if (raw.contains("refused"))   return "房主已關閉伺服器，請重新搜尋。";
        if (raw.contains("timed out") || raw.contains("timeout"))
                                        return "連線逾時，可能是防火牆阻擋或不在同一網路。";
        if (raw.contains("找不到"))    return raw;
        return "連線失敗：" + raw;
    }

    // ── 樣式工具 ──────────────────────────────────────────────────────────────

    private static String roomRowStyle(boolean selected, Theme t) {
        String bg = selected ? t.accent + "28" : "transparent";
        String border = selected
                ? "-fx-border-color:" + t.accentDark + ";-fx-border-width:0 0 0 3;"
                : "-fx-border-color:transparent;-fx-border-width:0 0 0 3;";
        return "-fx-background-color:" + bg + ";" + border + "-fx-cursor:hand;";
    }

    private static String roomRowHover(Theme t) {
        return "-fx-background-color:" + t.accent + "18;-fx-border-color:transparent;" +
               "-fx-border-width:0 0 0 3;-fx-cursor:hand;";
    }
}
