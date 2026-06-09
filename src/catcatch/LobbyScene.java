package catcatch;

import java.io.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.*;

public class LobbyScene {

    public static Scene build(CatCatchApp app, GameClient client, boolean isHost) {
        Theme t = CatCatchApp.theme;

        BorderPane root = new BorderPane();
        root.setStyle(t.rootStyle());

        // Pastel background layer
        Pane decoBg = new Pane();
        decoBg.setMouseTransparent(true);
        KawaiiDeco.addBackground(decoBg, t, CatCatchApp.WIDTH, CatCatchApp.HEIGHT);

        StackPane fullRoot = new StackPane(decoBg, root);

        root.setPadding(new Insets(26));
        root.setStyle("-fx-background-color:transparent;");

        // ── Top bar ───────────────────────────────────────────────────────────
        HBox topBar = new HBox(14);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = MainMenuScene.iconBtn("返回主選單", "home", 15, 145, t);
        backBtn.setOnAction(e -> { client.close(); app.goMainMenu(); });

        Label roomTitle = new Label("房間大廳");
        roomTitle.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:" + t.accentDark +
                           ";-fx-letter-spacing:1;");

        Label msgLabel = new Label("等待玩家...");
        msgLabel.setStyle("-fx-font-size:12px;-fx-text-fill:" + t.muted + ";");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().addAll(backBtn, roomTitle, spacer, msgLabel);
        BorderPane.setMargin(topBar, new Insets(0, 0, 14, 0));
        root.setTop(topBar);

        // ── Centre ────────────────────────────────────────────────────────────
        HBox centre = new HBox(16);
        centre.setAlignment(Pos.TOP_CENTER);

        // ── Left card: room info ──────────────────────────────────────────────
        VBox infoCard = new VBox(14);
        infoCard.setStyle(t.cardStyle());
        infoCard.setPadding(new Insets(24));
        infoCard.setPrefWidth(290);

        HBox infoTitle = sectionTitle("房間資訊", "crown", t);

        Label codeHead = smallLabel("房間號碼", t);
        Label codeLabel = new Label("......");
        codeLabel.setStyle("-fx-font-size:36px;-fx-font-weight:bold;-fx-letter-spacing:8;" +
                           "-fx-text-fill:" + t.accentDark + ";");

        Button copyBtn = MainMenuScene.iconBtn("複製房號", "coin", 14, 200, t);
        copyBtn.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(codeLabel.getText());
            Clipboard.getSystemClipboard().setContent(cc);
            copyBtn.setText("已複製！");
            Platform.runLater(() -> copyBtn.setText("複製房號"));
        });

        Label ipHead = smallLabel("主機 IP（分享給其他玩家）", t);
        ipHead.setWrapText(true);
        Label ipLabel = new Label(isHost ? CatCatchApp.getLocalIP() : "（加入者不需提供 IP）");
        ipLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + t.accentDark + ";");
        ipLabel.setWrapText(true);
        Label portLabel = new Label("連接埠：" + CatCatchApp.SERVER_PORT);
        portLabel.setStyle(t.labelStyle(11, false));

        Button readyBtn = MainMenuScene.primaryBtn("準備完成", 200, t);
        final boolean[] ready = {false};
        readyBtn.setOnAction(e -> {
            ready[0] = !ready[0];
            client.sendReady(ready[0]);
            readyBtn.setText(ready[0] ? "已準備（點擊取消）" : "準備完成");
        });

        Button startBtn = new Button("開始遊戲");
        startBtn.setPrefWidth(200);
        startBtn.setDisable(true);
        startBtn.setStyle(t.primaryBtn());
        startBtn.setOnMouseEntered(e -> startBtn.setStyle(t.primaryBtnHover()));
        startBtn.setOnMouseExited(e  -> startBtn.setStyle(t.primaryBtn()));
        startBtn.setOnAction(e -> client.sendStart());
        startBtn.setVisible(isHost); startBtn.setManaged(isHost);

        infoCard.getChildren().addAll(infoTitle, sep(t), codeHead, codeLabel, copyBtn);
        if (isHost) infoCard.getChildren().addAll(ipHead, ipLabel, portLabel);
        infoCard.getChildren().addAll(sep(t), readyBtn, startBtn);

        // ── Right card: player list ───────────────────────────────────────────
        VBox playersCard = new VBox(10);
        playersCard.setStyle(t.cardStyle());
        playersCard.setPadding(new Insets(24));
        playersCard.setPrefWidth(320);

        HBox playersTitle = sectionTitle("玩家列表", "medal", t);
        VBox playerRows = new VBox(9);
        playersCard.getChildren().addAll(playersTitle, sep(t), playerRows);

        centre.getChildren().addAll(infoCard, playersCard);
        HBox.setHgrow(playersCard, Priority.ALWAYS);

        // ── Bottom: rules card ────────────────────────────────────────────────
        VBox rulesCard = new VBox(10);
        rulesCard.setStyle(t.cardStyle());
        rulesCard.setPadding(new Insets(16, 24, 16, 24));

        HBox rulesTitle = sectionTitle("遊戲說明", "cat", t);

        // Score badges row
        HBox scoreRow = new HBox(24);
        scoreRow.setAlignment(Pos.CENTER_LEFT);
        Object[][] scoreInfo = {
            {"+10 分", "點到目標色貓咪", t.mint, "#2A8060"},
            {" -5 分",  "點到其他貓咪",   t.gold, "#907010"},
            {"-15 分", "點到地雷狗",      "#FFCDD2", "#C03030"},
            {"45 秒",  "每局時間",        t.sky,  "#2060A0"},
        };
        for (Object[] si : scoreInfo) {
            Label badge = new Label((String) si[0]);
            badge.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + si[3] + ";" +
                           "-fx-background-color:" + si[2] + ";-fx-background-radius:12;" +
                           "-fx-border-color:" + si[3] + ";-fx-border-radius:12;-fx-border-width:1.5;" +
                           "-fx-padding:3 10;");
            Label desc = new Label((String) si[1]);
            desc.setStyle("-fx-font-size:11px;-fx-text-fill:" + t.muted + ";");
            VBox cell = new VBox(4, badge, desc);
            cell.setAlignment(Pos.CENTER);
            scoreRow.getChildren().add(cell);
        }

        // Image legend
        HBox legendRow = new HBox(18);
        legendRow.setAlignment(Pos.CENTER_LEFT);
        String[][] legends = {
            {"assets/橘貓去背.png",     "橘色貓", "+10", t.mint,  "#2A8060"},
            {"assets/蘋果貓貓去背.png", "蘋果貓", "+10", t.mint,  "#2A8060"},
            {"assets/茄子貓貓去背.png", "茄子貓", "+10", t.mint,  "#2A8060"},
            {"assets/狗狗地雷去背.png", "地雷狗", "-15", "#FFCDD2","#C03030"},
        };
        for (String[] lg : legends) {
            VBox item = new VBox(5);
            item.setAlignment(Pos.CENTER);
            item.setStyle("-fx-background-color:" + lg[3] + ";-fx-background-radius:16;" +
                          "-fx-border-color:" + lg[4] + ";-fx-border-radius:16;-fx-border-width:1.5;" +
                          "-fx-padding:8 12;");

            ImageView iv = new ImageView();
            iv.setFitWidth(48); iv.setFitHeight(48); iv.setPreserveRatio(true);
            try (FileInputStream fis = new FileInputStream(new File(lg[0]))) {
                Image img = new Image(fis);
                if (!img.isError()) iv.setImage(img);
            } catch (Exception ignored) {}

            Label nameLbl = new Label(lg[1]);
            nameLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:" + lg[4] + ";");
            Label scoreLbl = new Label(lg[2]);
            scoreLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + lg[4] + ";");
            item.getChildren().addAll(iv, nameLbl, scoreLbl);
            legendRow.getChildren().add(item);
        }

        Label hint = new Label("每 5 秒目標顏色切換，記得看上方提示！");
        hint.setStyle("-fx-font-size:11px;-fx-text-fill:" + t.muted + ";-fx-font-style:italic;");

        rulesCard.getChildren().addAll(rulesTitle, sep(t), scoreRow, legendRow, hint);
        BorderPane.setMargin(rulesCard, new Insets(12, 0, 0, 0));

        VBox mainContent = new VBox(0, centre, rulesCard);
        VBox.setVgrow(centre, Priority.ALWAYS);
        root.setCenter(mainContent);

        // ── State listener ────────────────────────────────────────────────────
        // 確保只導覽一次（防止多個 STATE 封包重複觸發）
        final boolean[] navigated = {false};

        client.setListener(new GameClient.Listener() {
            @Override public void onConnected(String id) {}
            @Override public void onState(GameClient.GameState state) {
                codeLabel.setText(state.roomCode() != null ? state.roomCode() : "......");
                msgLabel.setText(state.message() != null ? state.message() : "");

                playerRows.getChildren().clear();
                for (GameClient.RemotePlayer p : state.players()) {
                    boolean isSelf = p.id().equals(state.selfId());
                    boolean isH    = p.id().equals(state.hostId());
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(9, 14, 9, 14));
                    row.setStyle(
                        "-fx-background-color:" + (isSelf ? t.accent + "30" : t.bg + "C0") + ";" +
                        "-fx-background-radius:16;" +
                        (isSelf ? "-fx-effect:dropshadow(gaussian,rgba(200,130,155,0.18),10,0,0,3);" : ""));

                    Label nameLbl = new Label(p.name() + (isSelf ? " (你)" : "") + (isH ? " 房主" : ""));
                    nameLbl.setStyle("-fx-font-size:14px;-fx-text-fill:" + t.text + ";" +
                                     (isSelf ? "-fx-font-weight:bold;" : ""));
                    Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);

                    Label badge = new Label(p.ready() ? "✿ 已準備" : "等待中");
                    badge.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 14;" +
                        (p.ready()
                         ? "-fx-background-color:" + t.mint + ";-fx-text-fill:#1E6040;" +
                           "-fx-effect:dropshadow(gaussian,rgba(80,180,130,0.22),8,0,0,2);"
                         : "-fx-background-color:" + t.bg + ";-fx-text-fill:" + t.muted + ";"));

                    row.getChildren().addAll(nameLbl, sp2, badge);
                    playerRows.getChildren().add(row);
                }
                if (isHost) {
                    // 倒數期間或遊戲中不可再按開始
                    boolean lobbyAndAllReady = "lobby".equals(state.status()) &&
                        !state.players().isEmpty() &&
                        state.players().stream().allMatch(GameClient.RemotePlayer::ready);
                    startBtn.setDisable(!lobbyAndAllReady);
                }

                // Server 進入倒數或直接進入遊戲時，導覽到 GameScene
                String st = state.status();
                if (!navigated[0] && ("countdown".equals(st) || "playing".equals(st))) {
                    navigated[0] = true;
                    CatCatchApp.audio.playStart();
                    app.goGame(client);
                }
            }
            @Override public void onError(String msg) { msgLabel.setText(msg); }
            @Override public void onDisconnected(String r) {
                Platform.runLater(() -> { client.close(); app.goMainMenu(); });
            }
        });

        Scene _scene = new Scene(fullRoot);
        decoBg.prefWidthProperty().bind(_scene.widthProperty());
        decoBg.prefHeightProperty().bind(_scene.heightProperty());
        return _scene;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static HBox sectionTitle(String text, String iconName, Theme t) {
        ImageView iv = Icons.get(iconName, 20);
        Label l = new Label(text);
        l.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" + t.text + ";");
        HBox box = new HBox(8, iv, l);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static Label smallLabel(String text, Theme t) {
        Label l = new Label(text);
        l.setStyle(t.labelStyle(12, true));
        return l;
    }

    private static Separator sep(Theme t) {
        Separator s = new Separator();
        s.setStyle("-fx-opacity:0.30;");
        return s;
    }
}
