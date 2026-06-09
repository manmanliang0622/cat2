package catcatch;

import java.io.File;
import java.util.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.util.Duration;

public class GameScene {

    private static final Map<String, String> VARIANT_LABEL = Map.of(
        "GRAY", "灰色貓咪", "GOLD", "金色貓咪", "SNOW", "雪白貓咪");
    private static final Map<String, String> VARIANT_COLOR = Map.of(
        "GRAY", "#9E9E9E", "GOLD", "#D4A800", "SNOW", "#4A9BD4");
    private static final Map<String, String> VARIANT_BG = Map.of(
        "GRAY", "#F5F4F0", "GOLD", "#FFF8DC", "SNOW", "#EAF4FF");
    private static final Map<String, String> VARIANT_BORDER = Map.of(
        "GRAY", "#C0B8AC", "GOLD", "#C89800", "SNOW", "#3A88C8");

    private static final Map<String, WritableImage> imgCache = new HashMap<>();

    public static Scene build(CatCatchApp app, GameClient client) {
        Theme t = CatCatchApp.theme;

        BorderPane root = new BorderPane();
        root.setStyle(t.rootStyle());
        preloadImages();

        // ── Top bar (candy style) ─────────────────────────────────────────────
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER);
        topBar.setPrefHeight(90);
        topBar.setPadding(new Insets(0, 18, 0, 18));
        topBar.setStyle(
            "-fx-background-color:" + t.panelBg + ";" +
            "-fx-effect:dropshadow(gaussian,rgba(180,120,140,0.14),14,0.02,0,5);");

        // --- Target section ---
        StackPane targetImgPane = new StackPane();
        targetImgPane.setPrefSize(66, 66);

        ImageView targetIV = new ImageView();
        targetIV.setFitWidth(64); targetIV.setFitHeight(64);
        targetIV.setPreserveRatio(true);

        Circle targetRing = new Circle(36);
        targetRing.setFill(Color.web(t.accent, 0.12));
        targetRing.setStroke(Color.TRANSPARENT);
        targetImgPane.getChildren().addAll(targetRing, targetIV);

        Label targetHint = new Label("現在要抓這隻！");
        targetHint.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:" + t.muted + ";");
        Label targetName = new Label("灰色貓咪");
        targetName.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#9E9E9E;");

        VBox targetText = new VBox(2, targetHint, targetName);
        targetText.setAlignment(Pos.CENTER_LEFT);

        HBox targetSection = new HBox(12, targetImgPane, targetText);
        targetSection.setAlignment(Pos.CENTER);
        targetSection.setPrefWidth(340);
        targetSection.setPadding(new Insets(8, 18, 8, 18));
        targetSection.setStyle(
            "-fx-background-color:" + VARIANT_BG.get("GRAY") + ";" +
            "-fx-background-radius:22;" +
            "-fx-effect:dropshadow(gaussian,rgba(160,120,130,0.15),10,0,0,3);");

        // --- Timer (candy card) ---
        HBox timerInner = new HBox(6, Icons.get("hourglass", 22));
        timerInner.setAlignment(Pos.CENTER);
        Label timerLabel = new Label("45");
        timerLabel.setStyle("-fx-font-size:32px;-fx-font-weight:bold;-fx-text-fill:" + t.accentDark +
                            ";-fx-min-width:52;-fx-alignment:center;");
        Label timerUnit = new Label("秒");
        timerUnit.setStyle("-fx-font-size:11px;-fx-text-fill:" + t.muted + ";");
        timerInner.getChildren().add(timerLabel);
        VBox timerBox = new VBox(1, timerInner, timerUnit);
        timerBox.setAlignment(Pos.CENTER);
        timerBox.setPrefWidth(110);
        timerBox.setStyle(
            "-fx-background-color:" + t.sky + ";" +
            "-fx-background-radius:22;" +
            "-fx-padding:6 14;" +
            "-fx-effect:dropshadow(gaussian,rgba(100,180,220,0.28),12,0.06,0,4);");

        // --- Score (candy card) ---
        Label scoreHead = new Label("スコア");
        scoreHead.setStyle("-fx-font-size:10px;-fx-text-fill:" + t.muted + ";");
        HBox scoreInner = new HBox(6, Icons.get("coin", 20));
        scoreInner.setAlignment(Pos.CENTER);
        Label scoreLabel = new Label("0");
        scoreLabel.setStyle("-fx-font-size:26px;-fx-font-weight:bold;-fx-text-fill:#907010;");
        scoreInner.getChildren().add(scoreLabel);
        VBox scoreBox = new VBox(1, scoreHead, scoreInner);
        scoreBox.setAlignment(Pos.CENTER);
        scoreBox.setPrefWidth(170);
        scoreBox.setPadding(new Insets(0, 16, 0, 0));
        scoreBox.setStyle(
            "-fx-background-color:" + t.gold + ";" +
            "-fx-background-radius:22;" +
            "-fx-padding:6 14;" +
            "-fx-effect:dropshadow(gaussian,rgba(200,160,0,0.28),12,0.06,0,4);");

        Region sp1 = new Region(); HBox.setHgrow(sp1, Priority.ALWAYS);
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        topBar.getChildren().addAll(targetSection, sp1, timerBox, sp2, scoreBox);
        root.setTop(topBar);

        // ── Game canvas (fills remaining width dynamically) ───────────────────
        Pane canvas = new Pane();
        canvas.setStyle("-fx-background-color:" + t.bg + ";");
        // The canvas grows to fill width; fixed height = screen minus top bar
        double canvasH = CatCatchApp.HEIGHT - 90;
        canvas.setPrefHeight(canvasH);

        // Light grid — drawn after layout via listener so we know actual width
        canvas.widthProperty().addListener((obs, ov, nw) -> {
            canvas.getChildren().removeIf(n -> n instanceof Line && ((Line)n).getStrokeWidth() == 1.0);
            for (int y = 60; y < canvasH; y += 60) {
                Line ln = new Line(0, y, nw.doubleValue(), y);
                ln.setStroke(Color.web(t.accent, 0.035));
                ln.setStrokeWidth(1.0);
                canvas.getChildren().add(0, ln);
            }
        });

        // ── Leaderboard (candy sidebar) ───────────────────────────────────────
        VBox lb = new VBox(8);
        lb.setPrefWidth(300);
        lb.setPadding(new Insets(16, 14, 16, 14));
        lb.setStyle(
            "-fx-background-color:" + t.lavender + "33;" +
            "-fx-border-color:" + t.accentDark + ";-fx-border-width:0 0 0 2.5;");

        HBox lbTitle = new HBox(8, Icons.get("trophy", 20));
        lbTitle.setAlignment(Pos.CENTER_LEFT);
        Label lbLbl = new Label("即時排行榜");
        lbLbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + t.text + ";");
        lbTitle.getChildren().add(lbLbl);
        VBox lbRows = new VBox(6);
        lb.getChildren().addAll(lbTitle, sep(), lbRows);

        HBox content = new HBox(canvas, lb);
        HBox.setHgrow(canvas, Priority.ALWAYS);
        root.setCenter(content);

        // ── Entity & state tracking ───────────────────────────────────────────
        Map<String, VBox> entityNodes = new HashMap<>();
        final int[]    prevScore    = {0};
        final String[] prevTarget   = {"GRAY"};
        // 倒數覆蓋層的 Label，在 listener 建立後才填入
        final Label[]  countLblRef  = {null};
        final int[]    lastShown    = {-1};

        client.setListener(new GameClient.Listener() {
            @Override public void onConnected(String id) {}

            @Override public void onState(GameClient.GameState state) {
                // 倒數期間：用 Server 的 countdownSeconds 同步顯示數字
                if ("countdown".equals(state.status())) {
                    int csec = state.countdownSeconds();
                    Label cl = countLblRef[0];
                    if (cl != null && csec > 0) showCountNumber(cl, csec, lastShown, t);
                    return; // 倒數中不處理其他 UI 更新
                }

                String tv = state.targetVariant() != null ? state.targetVariant() : "GRAY";

                // Target change
                if (!tv.equals(prevTarget[0])) {
                    prevTarget[0] = tv;
                    targetIV.setImage(imgCache.get(tv.toLowerCase()));
                    targetRing.setFill(Color.web(VARIANT_BG.getOrDefault(tv, "#F5F4F0"), 0.55));
                    targetIV.setEffect(new DropShadow(18, Color.web(VARIANT_COLOR.getOrDefault(tv, "#9E9E9E"))));
                    targetName.setText(VARIANT_LABEL.getOrDefault(tv, tv));
                    targetName.setStyle("-fx-font-size:18px;-fx-font-weight:bold;" +
                        "-fx-text-fill:" + VARIANT_COLOR.getOrDefault(tv, "#9E9E9E") + ";");
                    targetSection.setStyle(
                        "-fx-background-color:" + VARIANT_BG.getOrDefault(tv, "#F5F4F0") + ";" +
                        "-fx-background-radius:22;" +
                        "-fx-effect:dropshadow(gaussian,rgba(160,120,130,0.15),10,0,0,3);");
                    ScaleTransition fl = new ScaleTransition(Duration.millis(160), targetSection);
                    fl.setFromX(1.0); fl.setFromY(1.0); fl.setToX(1.05); fl.setToY(1.05);
                    fl.setAutoReverse(true); fl.setCycleCount(2); fl.play();
                    entityNodes.values().forEach(v -> updateGlow(v, tv, t));
                }

                // Timer
                int rem = state.remainingSeconds();
                timerLabel.setText(String.valueOf(rem));
                timerLabel.setStyle("-fx-font-size:32px;-fx-font-weight:bold;" +
                    "-fx-alignment:center;-fx-min-width:52;" +
                    "-fx-text-fill:" + (rem <= 10 ? "#C03030" : t.accentDark) + ";");

                // Score
                GameClient.RemotePlayer self = state.self();
                if (self != null) {
                    int ns = self.score();
                    if (ns != prevScore[0]) {
                        int delta = ns - prevScore[0];
                        prevScore[0] = ns;
                    }
                    scoreLabel.setText(String.valueOf(self.score()));
                }

                // Entities (diff)
                Set<String> alive = new HashSet<>();
                state.entities().forEach(e -> alive.add(e.id()));
                entityNodes.entrySet().removeIf(en -> {
                    if (!alive.contains(en.getKey())) {
                        canvas.getChildren().remove(en.getValue());
                        return true;
                    }
                    return false;
                });

                for (GameClient.RemoteEntity e : state.entities()) {
                    if (!entityNodes.containsKey(e.id())) {
                        VBox node = buildEntity(e, tv, client, canvas, t);
                        double sz = e.size();
                        double cw = canvas.getWidth()  > 0 ? canvas.getWidth()  : canvas.getPrefWidth();
                        double ch = canvas.getHeight() > 0 ? canvas.getHeight() : canvas.getPrefHeight();
                        node.setLayoutX(e.x() * cw - sz * 0.45);
                        node.setLayoutY(e.y() * ch - sz * 0.45);
                        canvas.getChildren().add(node);
                        entityNodes.put(e.id(), node);
                        spawnAnim(node);
                    }
                }

                // Leaderboard
                lbRows.getChildren().clear();
                String[] rankIcons  = {"crown","medal","medal"};
                String[] rankColors = {"#C89600","#808080","#8C5828"};
                for (int i = 0; i < state.players().size(); i++) {
                    GameClient.RemotePlayer p = state.players().get(i);
                    boolean isSelf = p.id().equals(state.selfId());
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(5, 10, 5, 10));
                    if (isSelf) row.setStyle("-fx-background-color:" + t.accent + "22;-fx-background-radius:10;");

                    javafx.scene.Node rankNode = i < 3
                        ? Icons.get(rankIcons[i], 18)
                        : new Label((i+1)+".") {{
                              setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + t.muted + ";-fx-min-width:20;");
                          }};

                    Label name = new Label(p.name());
                    name.setStyle("-fx-font-size:12px;-fx-text-fill:" + t.text + ";" +
                                  (isSelf ? "-fx-font-weight:bold;" : ""));
                    Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
                    Label sc = new Label(p.score() + "分");
                    sc.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + t.accentDark + ";");
                    row.getChildren().addAll(rankNode, name, sp, sc);
                    lbRows.getChildren().add(row);
                }

                if ("finished".equals(state.status())) {
                    CatCatchApp.audio.stopBgm();
                    CatCatchApp.audio.playFinish();
                    app.goResult(state, client, state.isHost());
                }
            }

            @Override public void onError(String msg) {}
            @Override public void onDisconnected(String r) {
                Platform.runLater(() -> { client.close(); app.goMainMenu(); });
            }
        });

        WritableImage init = imgCache.get("gray");
        if (init != null) targetIV.setImage(init);

        // ── Countdown overlay（與 Server 倒數同步）──────────────────────────────
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:rgba(255,240,250,0.85);");
        overlay.setPrefSize(CatCatchApp.WIDTH, CatCatchApp.HEIGHT);
        // 蓋住遊戲畫布，倒數期間阻擋所有點擊
        overlay.setPickOnBounds(true);

        VBox countBox = new VBox(12);
        countBox.setAlignment(Pos.CENTER);

        Label countLbl = new Label("3");
        countLbl.setStyle(countStyle(t.accentDark));
        countLblRef[0] = countLbl; // 讓 listener 可以更新倒數數字
        Label readyLbl = new Label("準備好了嗎？");
        readyLbl.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:" + t.muted + ";");

        // 浮動星星裝飾
        for (int i = 0; i < 8; i++) {
            double x = 80 + Math.random() * (CatCatchApp.WIDTH - 160);
            double y = 40  + Math.random() * (CatCatchApp.HEIGHT - 80);
            Polygon star = KawaiiDeco.miniStar(12 + Math.random() * 10, t.gold, 0.55);
            star.setTranslateX(x - CatCatchApp.WIDTH / 2.0);
            star.setTranslateY(y - CatCatchApp.HEIGHT / 2.0);
            overlay.getChildren().add(star);
            RotateTransition rt = new RotateTransition(Duration.seconds(4 + Math.random() * 4), star);
            rt.setByAngle(360); rt.setCycleCount(Animation.INDEFINITE); rt.play();
        }

        countBox.getChildren().addAll(countLbl, readyLbl);
        overlay.getChildren().add(countBox);

        StackPane fullRoot = new StackPane(root, overlay);

        // 倒數動畫：由 Server STATE 驅動，保持與伺服器同步
        // 同時啟動本地 Timeline 作為備用（網路延遲時仍能正常顯示）
        Timeline countdown = new Timeline();
        for (int i = 3; i >= 0; i--) {
            final int val = i;
            countdown.getKeyFrames().add(new KeyFrame(Duration.seconds(3 - val), ev -> {
                if (val == 0) {
                    // 倒數結束，播放 BGM 並顯示 Go!
                    CatCatchApp.audio.startBgm();
                    countLbl.setText("GO！");
                    countLbl.setStyle(countStyle(t.accent));
                    readyLbl.setVisible(false);
                    FadeTransition ft = new FadeTransition(Duration.millis(600), overlay);
                    ft.setFromValue(1.0); ft.setToValue(0.0);
                    ft.setOnFinished(e2 -> fullRoot.getChildren().remove(overlay));
                    ft.play();
                } else {
                    showCountNumber(countLblRef[0] != null ? countLblRef[0] : countLbl, val, lastShown, t);
                }
            }));
        }
        countdown.play();

        return new Scene(fullRoot);
    }

    // ── Entity ────────────────────────────────────────────────────────────────

    private static VBox buildEntity(GameClient.RemoteEntity e, String targetVariant,
                                     GameClient client, Pane canvas, Theme t) {
        boolean isDog = "DOG".equals(e.kind());
        String variant = e.variant();
        double size = e.size();
        double imgSize = size * 0.84;

        StackPane imgPane = new StackPane();
        imgPane.setPrefSize(imgSize, imgSize);
        imgPane.setMaxSize(imgSize, imgSize);

        String cacheKey = isDog ? "dog" : variant.toLowerCase();
        WritableImage proc = imgCache.get(cacheKey);

        // Pastel background circle
        Circle bg = new Circle(imgSize * 0.50);
        bg.setFill(Color.web(isDog ? "#FFCDD2" : VARIANT_BG.getOrDefault(variant, "#F5F4F0")));
        bg.setStroke(Color.web(isDog ? "#C03030" : VARIANT_BORDER.getOrDefault(variant, "#C0B8AC")));
        bg.setStrokeWidth(2.5);

        if (proc != null) {
            ImageView iv = new ImageView(proc);
            iv.setFitWidth(imgSize * 0.88);
            iv.setFitHeight(imgSize * 0.88);
            iv.setPreserveRatio(true);
            imgPane.getChildren().addAll(bg, iv);
        } else {
            Label txt = new Label(isDog ? "犬" : "猫");
            txt.setStyle("-fx-font-size:" + (imgSize * 0.32) + "px;-fx-font-weight:bold;" +
                         "-fx-text-fill:" + (isDog ? "#C03030" : VARIANT_COLOR.getOrDefault(variant, "#9E9E9E")) + ";");
            imgPane.getChildren().addAll(bg, txt);
        }

        // Kawaii label tag
        Label label = new Label(isDog ? "地雷！" : VARIANT_LABEL.getOrDefault(variant, variant));
        label.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-alignment:center;" +
            "-fx-background-color:" + (isDog ? "#FFCDD2" : VARIANT_BG.getOrDefault(variant, "#F5F4F0")) + ";" +
            "-fx-text-fill:" + (isDog ? "#C03030" : VARIANT_COLOR.getOrDefault(variant, "#888")) + ";" +
            "-fx-background-radius:10;-fx-border-color:" + (isDog ? "#C03030" : VARIANT_BORDER.getOrDefault(variant, "#C0B8AC")) + ";" +
            "-fx-border-radius:10;-fx-border-width:1.5;-fx-padding:2 8;");

        VBox vbox = new VBox(4, imgPane, label);
        vbox.setAlignment(Pos.CENTER);
        vbox.setUserData(isDog ? "DOG" : variant);

        updateGlow(vbox, targetVariant, t);

        vbox.setOnMouseClicked(evt -> {
            vbox.setOnMouseClicked(null);
            client.sendClick(e.id());
            clickAnim(vbox);
            double cx = vbox.getLayoutX() + vbox.getBoundsInLocal().getWidth() / 2;
            double cy = vbox.getLayoutY() + 10;
            if (isDog) {
                CatCatchApp.audio.playDog();
                KawaiiDeco.wrongClickOops(canvas, cx, cy, -15);
            } else if (variant.equals(targetVariant)) {
                CatCatchApp.audio.playMeow();
                KawaiiDeco.correctClickBurst(canvas, cx, cy, 10);
            } else {
                CatCatchApp.audio.playWrong();
                KawaiiDeco.wrongClickOops(canvas, cx, cy, -5);
            }
        });
        vbox.setCursor(javafx.scene.Cursor.HAND);
        return vbox;
    }

    private static void updateGlow(VBox vbox, String currentTarget, Theme t) {
        if (vbox.getChildren().isEmpty()) return;
        javafx.scene.Node imgPane = vbox.getChildren().get(0);
        String variant = vbox.getUserData() != null ? vbox.getUserData().toString() : "";
        boolean isDog   = "DOG".equals(variant);
        boolean isTarget = !isDog && variant.equals(currentTarget);

        if (isDog) {
            imgPane.setEffect(new DropShadow(10, Color.web("#C03030", 0.55)));
        } else if (isTarget) {
            DropShadow glow = new DropShadow(20, Color.web(VARIANT_COLOR.getOrDefault(variant, "#9E9E9E")));
            glow.setSpread(0.28);
            imgPane.setEffect(glow);
        } else {
            imgPane.setEffect(new DropShadow(5, Color.rgb(100, 60, 60, 0.15)));
        }
    }

    // ── Animations ────────────────────────────────────────────────────────────

    private static void spawnAnim(javafx.scene.Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(240), node);
        st.setFromX(0.05); st.setFromY(0.05); st.setToX(1.0); st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT); st.play();
    }

    private static void clickAnim(javafx.scene.Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(110), node);
        st.setFromX(1.0); st.setFromY(1.0); st.setToX(1.26); st.setToY(1.26);
        st.setAutoReverse(true); st.setCycleCount(2); st.play();
    }

    private static Separator sep() {
        Separator s = new Separator();
        s.setStyle("-fx-opacity:0.30;");
        return s;
    }

    /** 顯示倒數數字並播放縮放動畫；同一個數字不重複跳動。 */
    private static void showCountNumber(Label lbl, int val, int[] lastShown, Theme t) {
        if (lastShown[0] == val) return; // 避免同一秒重複觸發
        lastShown[0] = val;
        lbl.setText(String.valueOf(val));
        lbl.setStyle(countStyle(t.accentDark));
        ScaleTransition pulse = new ScaleTransition(Duration.millis(360), lbl);
        pulse.setFromX(1.55); pulse.setFromY(1.55);
        pulse.setToX(1.0);   pulse.setToY(1.0);
        pulse.setInterpolator(Interpolator.EASE_OUT);
        pulse.play();
    }

    /** 倒數數字的統一樣式。 */
    private static String countStyle(String color) {
        return "-fx-font-size:138px;-fx-font-weight:bold;" +
               "-fx-text-fill:" + color + ";" +
               "-fx-effect:dropshadow(gaussian,rgba(230,130,160,0.38),20,0,2,5);";
    }

    // ── Image preload ─────────────────────────────────────────────────────────

    private static void preloadImages() {
        if (!imgCache.isEmpty()) return;
        // 每個 variant 可設多個候選路徑，依序嘗試直到載入成功
        String[][][] specs = {
            {{"gray"},  {"assets/茄子貓貓去背.png", "assets/cat-gray.jpg"}},
            {{"gold"},  {"assets/橘貓去背.png",     "assets/cat-gold.jpg"}},
            {{"snow"},  {"assets/蘋果貓貓去背.png", "assets/cat-snow.jpg"}},
            {{"dog"},   {"assets/狗狗地雷去背.png", "assets/dog.jpg"}},
        };
        for (String[][] spec : specs) {
            String key = spec[0][0];
            for (String path : spec[1]) {
                File f = new File(path);
                if (!f.exists()) continue;
                try (java.io.FileInputStream fis = new java.io.FileInputStream(f)) {
                    javafx.scene.image.Image raw = new javafx.scene.image.Image(fis);
                    if (!raw.isError()) {
                        imgCache.put(key, new WritableImage(
                            raw.getPixelReader(), 0, 0,
                            (int) raw.getWidth(), (int) raw.getHeight()));
                        break; // 載入成功，不嘗試後備路徑
                    }
                } catch (Exception ignored) {}
            }
        }
    }
}
