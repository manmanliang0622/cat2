package catcatch;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.util.Duration;

public class ResultScene {

    public static Scene build(CatCatchApp app, GameClient.GameState finalState,
                               GameClient client, boolean isHost) {
        Theme t = CatCatchApp.theme;

        // ── Full-page root with deco background ───────────────────────────────
        StackPane fullRoot = new StackPane();
        fullRoot.setStyle(t.rootStyle());

        Pane decoBg = new Pane();
        decoBg.setMouseTransparent(true);
        KawaiiDeco.addBackground(decoBg, t, CatCatchApp.WIDTH, CatCatchApp.HEIGHT);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:transparent;");
        root.setPadding(new Insets(30));

        fullRoot.getChildren().addAll(decoBg, root);

        // ── Title area ────────────────────────────────────────────────────────
        VBox titleBox = new VBox(10);
        titleBox.setAlignment(Pos.CENTER);

        // Trophy icon in a candy circle
        StackPane trophyPane = new StackPane();
        trophyPane.setPrefSize(84, 84);
        Circle trBg = new Circle(40);
        trBg.setFill(Color.web(t.gold, 0.28));
        trBg.setEffect(new javafx.scene.effect.DropShadow(16,
                Color.web(t.gold, 0.35)));
        ImageView trophyIv = Icons.get("trophy", 54);
        trophyPane.getChildren().addAll(trBg, trophyIv);

        // Bounce animation for trophy
        ScaleTransition bounce = new ScaleTransition(Duration.millis(500), trophyPane);
        bounce.setFromX(0.2); bounce.setFromY(0.2); bounce.setToX(1.1); bounce.setToY(1.1);
        bounce.setAutoReverse(true); bounce.setCycleCount(2);
        bounce.setInterpolator(Interpolator.EASE_OUT); bounce.play();

        Label title = new Label("遊戲結束！");
        title.setStyle("-fx-font-size:34px;-fx-font-weight:bold;-fx-text-fill:" + t.accentDark + ";" +
                       "-fx-effect:dropshadow(gaussian,rgba(230,130,160,0.25),4,0,1,2);");

        // Find self rank for star rating
        GameClient.RemotePlayer selfPlayer = finalState.self();
        int selfRank = -1;
        for (int i = 0; i < finalState.players().size(); i++) {
            if (finalState.players().get(i).id().equals(finalState.selfId())) {
                selfRank = i + 1; break;
            }
        }
        javafx.scene.layout.HBox stars = KawaiiDeco.starRating(selfRank);

        titleBox.getChildren().addAll(trophyPane, title, stars);
        BorderPane.setMargin(titleBox, new Insets(0, 0, 18, 0));
        root.setTop(titleBox);

        // ── Rank list ─────────────────────────────────────────────────────────
        VBox rankList = new VBox(10);
        rankList.setAlignment(Pos.CENTER);
        rankList.setMaxWidth(560);

        // 前三名背景色（無硬框線，純陰影）
        String[] rowBg  = {t.gold + "45", t.lavender + "42", t.sky + "42"};
        String[] rowShadow = {
            "rgba(200,165,0,0.20)", "rgba(150,130,190,0.18)", "rgba(80,160,210,0.18)"};

        for (int i = 0; i < finalState.players().size(); i++) {
            GameClient.RemotePlayer p = finalState.players().get(i);
            boolean isSelf = p.id().equals(finalState.selfId());

            HBox row = new HBox(14);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(14, 22, 14, 22));
            String rowStyle = (i < 3)
                ? "-fx-background-color:" + rowBg[i] + ";-fx-background-radius:22;" +
                  "-fx-effect:dropshadow(gaussian," + rowShadow[i] + ",14,0.06,0,5);"
                : "-fx-background-color:" + t.panelBg + ";-fx-background-radius:16;" +
                  "-fx-effect:dropshadow(gaussian,rgba(180,130,150,0.10),8,0,0,3);";
            if (isSelf)
                rowStyle += "-fx-border-color:" + t.accent + "CC;-fx-border-radius:22;-fx-border-width:2;";
            row.setStyle(rowStyle);

            // Rank badge
            javafx.scene.Node badge;
            if (i == 0) {
                StackPane s0 = new StackPane(Icons.get("crown", 38));
                s0.setPrefSize(44, 44); badge = s0;
            } else if (i < 3) {
                StackPane sN = new StackPane(Icons.get("medal", 34));
                sN.setPrefSize(44, 44); badge = sN;
            } else {
                StackPane sp = new StackPane();
                sp.setPrefSize(40, 40);
                Circle c = new Circle(18);
                c.setFill(Color.web(t.muted, 0.15));
                c.setStroke(Color.web(t.muted, 0.40));
                c.setStrokeWidth(1.5);
                Label n = new Label((i + 1) + ".");
                n.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + t.muted + ";");
                sp.getChildren().addAll(c, n);
                badge = sp;
            }

            Label nameLbl = new Label(p.name() + (isSelf ? "  (你)" : ""));
            nameLbl.setStyle("-fx-font-size:16px;-fx-text-fill:" + t.text + ";" +
                             (isSelf ? "-fx-font-weight:bold;" : ""));
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            Label scoreLbl = new Label(p.score() + " 分");
            scoreLbl.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:" + t.accentDark + ";");

            row.getChildren().addAll(badge, nameLbl, sp, scoreLbl);
            rankList.getChildren().add(row);

            // Staggered entrance
            row.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(320), row);
            ft.setDelay(Duration.millis(i * 90 + 300));
            ft.setFromValue(0); ft.setToValue(1); ft.play();
            TranslateTransition tt = new TranslateTransition(Duration.millis(320), row);
            tt.setDelay(Duration.millis(i * 90 + 300));
            tt.setFromY(20); tt.setToY(0);
            tt.setInterpolator(Interpolator.EASE_OUT); tt.play();
        }

        ScrollPane scroll = new ScrollPane(rankList);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        scroll.setMaxHeight(320);

        String summaryTxt = "你的排名：第 " + selfRank + " 名";
        if (selfPlayer != null) summaryTxt += "  ·  最終分數：" + selfPlayer.score() + " 分";
        Label summary = new Label(summaryTxt);
        summary.setStyle("-fx-font-size:13px;-fx-text-fill:" + t.muted + ";");

        VBox centre = new VBox(12, scroll, summary);
        centre.setAlignment(Pos.CENTER);
        root.setCenter(centre);

        // ── Bottom buttons (kawaii candy style) ───────────────────────────────
        Label countdownLabel = new Label("10 秒後自動返回大廳");
        countdownLabel.setStyle("-fx-font-size:12px;-fx-text-fill:" + t.muted + ";");

        Button playAgainBtn = MainMenuScene.primaryIconBtn("再來一局！", "music", 16, 200, t);
        Button backLobbyBtn = MainMenuScene.iconBtn("返回大廳",    "home",  16, 200, t);

        playAgainBtn.setOnAction(e -> {
            playAgainBtn.setDisable(true);
            playAgainBtn.setText("等待其他玩家...");
            client.sendPlayAgain();
        });
        backLobbyBtn.setOnAction(e -> client.sendBackLobby());

        HBox btnRow = new HBox(18, playAgainBtn, backLobbyBtn);
        btnRow.setAlignment(Pos.CENTER);

        VBox bottom = new VBox(10, btnRow, countdownLabel);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(14, 0, 0, 0));
        root.setBottom(bottom);

        // ── Countdown ─────────────────────────────────────────────────────────
        int[] cd = {finalState.returnSeconds() > 0 ? finalState.returnSeconds() : 10};
        Timeline timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            cd[0]--;
            countdownLabel.setText(cd[0] + " 秒後自動返回大廳");
            if (cd[0] <= 0) client.sendBackLobby();
        }));
        timer.setCycleCount(Timeline.INDEFINITE); timer.play();

        client.setListener(new GameClient.Listener() {
            @Override public void onConnected(String id) {}
            @Override public void onState(GameClient.GameState state) {
                if (state.message() != null && state.message().contains("想再玩"))
                    countdownLabel.setText(state.message());
                if ("lobby".equals(state.status())) { timer.stop(); app.goLobby(client, state.isHost()); }
            }
            @Override public void onError(String msg) { countdownLabel.setText(msg); }
            @Override public void onDisconnected(String r) {
                timer.stop();
                Platform.runLater(() -> { client.close(); app.goMainMenu(); });
            }
        });

        Scene _scene = new Scene(fullRoot);
        decoBg.prefWidthProperty().bind(_scene.widthProperty());
        decoBg.prefHeightProperty().bind(_scene.heightProperty());
        return _scene;
    }
}
