package catcatch;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.*;
import javafx.util.Duration;

public class MainMenuScene {

    public static Scene build(CatCatchApp app) {
        Theme t = CatCatchApp.theme;

        // ── Root: fills entire window, centres the card ───────────────────────
        StackPane root = new StackPane();
        root.setStyle(t.rootStyle());
        root.setAlignment(Pos.CENTER);

        Pane decoBg = new Pane();
        decoBg.setMouseTransparent(true);
        KawaiiDeco.addBackground(decoBg, t, CatCatchApp.WIDTH, CatCatchApp.HEIGHT);

        // ── Candy card ────────────────────────────────────────────────────────
        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(46, 68, 46, 68));
        card.setStyle(t.cardStyle());
        card.setMaxWidth(430);

        // ── Kawaii sticker title (replaces icon + plain label) ───────────────
        Pane titlePane = buildKawaiiTitle(t);

        // Deco divider
        HBox divider = kawaiiDivider(t, 300);

        // ── Name input ────────────────────────────────────────────────────────
        Label nameLabel = new Label("你的名字");
        nameLabel.setStyle(t.labelStyle(13, true));

        TextField nameField = new TextField();
        nameField.setPromptText("輸入名字...");
        nameField.setStyle(t.inputStyle());
        nameField.setMaxWidth(300);

        VBox nameBox = new VBox(7, nameLabel, nameField);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        nameBox.setMaxWidth(300);

        Label errorLabel = new Label(" ");
        errorLabel.setStyle("-fx-text-fill:#D04060;-fx-font-size:12px;-fx-font-weight:bold;");
        errorLabel.setMinHeight(16);

        // ── Buttons ───────────────────────────────────────────────────────────
        Button hostBtn    = primaryBtn("建立房間",       300, t);
        Button searchBtn  = primaryBtn("搜尋附近房間",   300, t);
        Button joinToggle = iconBtn("手動輸入 IP 加入", "gear", 14, 300, t);
        Button settingsBtn = iconBtn("設定", "gear", 15, 300, t);
        Button quitBtn    = secondaryBtn("離開遊戲", 300, t);

        // ── Join panel（手動輸入 IP 備用）────────────────────────────────────
        VBox joinPanel = buildJoinPanel(app, t, nameField, errorLabel);
        joinPanel.setVisible(false);
        joinPanel.setManaged(false);

        joinToggle.setOnAction(e -> {
            boolean show = !joinPanel.isVisible();
            joinPanel.setVisible(show);
            joinPanel.setManaged(show);
            joinToggle.setText(show ? "取消手動輸入" : "手動輸入 IP 加入");
        });

        hostBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { errorLabel.setText("請先輸入名字！"); return; }
            errorLabel.setText(" ");
            hostBtn.setDisable(true);
            hostBtn.setText("啟動中...");
            doHost(app, name, hostBtn, errorLabel);
        });

        searchBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { errorLabel.setText("請先輸入名字！"); return; }
            errorLabel.setText(" ");
            app.goRoomBrowse(name);
        });

        settingsBtn.setOnAction(e -> app.goSettings());
        quitBtn.setOnAction(e -> { CatCatchApp.audio.shutdown(); System.exit(0); });

        card.getChildren().addAll(
            titlePane, divider,
            nameBox, errorLabel,
            hostBtn, searchBtn, joinToggle, joinPanel,
            kawaiiDivider(t, 300),
            settingsBtn, quitBtn
        );

        // StackPane centres card automatically; deco layer is mouse-transparent behind it
        root.getChildren().addAll(decoBg, card);
        StackPane.setAlignment(card, Pos.CENTER);

        Scene scene = new Scene(root);
        // Bind deco pane so it always fills the window
        decoBg.prefWidthProperty().bind(scene.widthProperty());
        decoBg.prefHeightProperty().bind(scene.heightProperty());
        return scene;
    }

    // ── Kawaii sticker title ──────────────────────────────────────────────────

    /**
     * Builds the "抓小貓" title as a layered sticker:
     *  · bottom shadow (dark pink, offset)
     *  · thick cream outline
     *  · gradient-ish pink fill
     *  · floating deco: stars, paw dots, fish-bone, bell
     * Each character is slightly rotated for a hand-drawn feel.
     */
    /** Cached jf 粉圓 font (loaded once per JVM). */
    private static Font jfFont64 = null;
    private static Font jfFont68 = null;
    private static Font jfFont72 = null;

    private static Font loadJf(double size) {
        try (java.io.FileInputStream fis =
                new java.io.FileInputStream("assets/fonts/jf-openhuninn.ttf")) {
            Font f = Font.loadFont(fis, size);
            return f != null ? f : Font.font("Microsoft JhengHei", FontWeight.BLACK, size);
        } catch (Exception e) {
            return Font.font("Microsoft JhengHei", FontWeight.BLACK, size);
        }
    }

    /**
     * "抓小貓" sticker title using jf open 粉圓.
     *
     * Each character gets 4 layers:
     *   shadow → cream outline → pink fill → pale highlight
     * Plus decorations: stars, paw prints, fish-bone, bell.
     */
    static Pane buildKawaiiTitle(Theme t) {
        // Load jf 粉圓 at three sizes
        if (jfFont64 == null) { jfFont64 = loadJf(64); jfFont68 = loadJf(68); jfFont72 = loadJf(72); }

        Pane pane = new Pane();
        pane.setPrefSize(340, 140);
        pane.setMaxWidth(340);

        // ── Character layout ──────────────────────────────────────────────────
        // {glyph, baseX, baseY (from top), rotation, font}
        Object[][] chars = {
            {"抓", 10,  68, -3.5, jfFont64},
            {"小", 90,  64,  2.0, jfFont68},
            {"貓", 172, 66, -2.0, jfFont72},
        };

        for (Object[] ch : chars) {
            String glyph = (String) ch[0];
            double bx    = (Integer) ch[1];
            double by    = (Integer) ch[2];
            double rot   = (Double)  ch[3];
            Font   f     = (Font)    ch[4];

            // L1 – shadow: warm brown-rose, soft offset
            Text shadow = new Text(glyph);
            shadow.setFont(f);
            shadow.setFill(Color.web("#A06070", 0.28));
            shadow.setLayoutX(bx + 3); shadow.setLayoutY(by + 5);
            shadow.setRotate(rot);

            // L2 – warm cream outline
            Text outline = new Text(glyph);
            outline.setFont(f);
            outline.setFill(Color.web("#FDF4EC"));
            outline.setStroke(Color.web("#FDF4EC"));
            outline.setStrokeWidth(10);
            outline.setStrokeType(StrokeType.OUTSIDE);
            outline.setLayoutX(bx); outline.setLayoutY(by);
            outline.setRotate(rot);

            // L3 – dusty rose fill + warm border
            Text fill = new Text(glyph);
            fill.setFont(f);
            fill.setFill(Color.web("#D4919C"));   // muted dusty rose
            fill.setStroke(Color.web("#B87080"));
            fill.setStrokeWidth(1.8);
            fill.setStrokeType(StrokeType.OUTSIDE);
            fill.setLayoutX(bx); fill.setLayoutY(by);
            fill.setRotate(rot);

            // L4 – pale highlight for soft glow
            Text hi = new Text(glyph);
            hi.setFont(f);
            hi.setFill(Color.web("#F8E0E6", 0.40));
            hi.setLayoutX(bx - 1.5); hi.setLayoutY(by - 2);
            hi.setRotate(rot);

            pane.getChildren().addAll(shadow, outline, fill, hi);
        }

        // ── Decorations ───────────────────────────────────────────────────────

        // Paw prints – muted dusty rose / powder blue
        addPawDeco(pane,  -2,  4, "#D4A8B4", 0.55);
        addPawDeco(pane, 298, 10, "#A8C8DC", 0.50);

        // Small rotating stars – warm cream tones
        double[][] stars = {{4, 95, 11}, {326, 88, 9}, {152, -2, 9}, {298, 52, 7}};
        String[]   scols = {"#E8C870", "#F0DFA8", "#D4A8B4", "#E8C870"};
        for (int i = 0; i < stars.length; i++) {
            Polygon s = fiveStar(stars[i][2]);
            s.setFill(Color.web(scols[i], 0.80));
            s.setStroke(Color.web("#B89840", 0.40));
            s.setStrokeWidth(0.8);
            s.setLayoutX(stars[i][0] + stars[i][2]);
            s.setLayoutY(stars[i][1] + stars[i][2]);
            RotateTransition rt = new RotateTransition(
                Duration.seconds(5.5 + i * 1.4), s);
            rt.setByAngle(360); rt.setCycleCount(Animation.INDEFINITE); rt.play();
            pane.getChildren().add(s);
        }

        // Fish bone – powder blue, softer
        addFishBone(pane, 264, 112, "#A8C8DC", 0.65);

        // Bell – warm cream
        addBellDeco(pane, 2, 106, "#E8D898", "#B09040");

        // Whole title gently floats up & down
        TranslateTransition fl = new TranslateTransition(Duration.seconds(2.8), pane);
        fl.setByY(-6); fl.setAutoReverse(true);
        fl.setCycleCount(Animation.INDEFINITE);
        fl.setInterpolator(Interpolator.EASE_BOTH);
        fl.play();

        return pane;
    }

    private static void addPawDeco(Pane p, double x, double y, String col, double op) {
        // Palm
        Circle palm = circle(x + 7, y + 10, 7, col, op);
        // Toes
        Circle t1 = circle(x,      y + 3,  4, col, op);
        Circle t2 = circle(x + 7,  y,      4, col, op);
        Circle t3 = circle(x + 14, y + 3,  4, col, op);
        p.getChildren().addAll(palm, t1, t2, t3);
    }

    private static Circle circle(double cx, double cy, double r, String col, double op) {
        Circle c = new Circle(cx, cy, r);
        c.setFill(Color.web(col, op));
        c.setStroke(Color.web(col, op * 0.55));
        c.setStrokeWidth(1);
        return c;
    }

    /** Simple 5-pointed star centred at (0,0); caller translates */
    private static Polygon fiveStar(double r) {
        Polygon s = new Polygon();
        for (int i = 0; i < 5; i++) {
            double a1 = Math.toRadians(i * 72 - 90);
            double a2 = Math.toRadians(i * 72 - 90 + 36);
            s.getPoints().addAll(
                r * Math.cos(a1), r * Math.sin(a1),
                r * 0.40 * Math.cos(a2), r * 0.40 * Math.sin(a2));
        }
        return s;
    }

    private static void addFishBone(Pane p, double x, double y, String col, double op) {
        // Spine
        javafx.scene.shape.Line spine = new javafx.scene.shape.Line(x, y, x + 28, y);
        spine.setStroke(Color.web(col, op)); spine.setStrokeWidth(2);
        // Ribs (4 pairs)
        for (int i = 1; i <= 4; i++) {
            double bx = x + i * 5;
            javafx.scene.shape.Line r1 = new javafx.scene.shape.Line(bx, y, bx - 3, y - 5);
            javafx.scene.shape.Line r2 = new javafx.scene.shape.Line(bx, y, bx - 3, y + 5);
            r1.setStroke(Color.web(col, op)); r1.setStrokeWidth(1.5);
            r2.setStroke(Color.web(col, op)); r2.setStrokeWidth(1.5);
            p.getChildren().addAll(r1, r2);
        }
        // Tail fan
        for (double a : new double[]{-40, -15, 15, 40}) {
            double bx = x + 28 + 7 * Math.cos(Math.toRadians(a));
            double by = y      + 7 * Math.sin(Math.toRadians(a));
            javafx.scene.shape.Line tail = new javafx.scene.shape.Line(x + 28, y, bx, by);
            tail.setStroke(Color.web(col, op)); tail.setStrokeWidth(1.5);
            p.getChildren().add(tail);
        }
        // Head circle
        Circle head = new Circle(x, y, 4);
        head.setFill(Color.web(col, op));
        p.getChildren().addAll(spine, head);
    }

    private static void addBellDeco(Pane p, double x, double y, String fill, String stroke) {
        // Bell body (arc approximated with ellipse)
        Ellipse body = new Ellipse(x + 9, y + 6, 9, 7);
        body.setFill(Color.web(fill, 0.85));
        body.setStroke(Color.web(stroke, 0.70));
        body.setStrokeWidth(1.5);
        // Top knob
        Circle knob = new Circle(x + 9, y, 3);
        knob.setFill(Color.web(fill, 0.85));
        knob.setStroke(Color.web(stroke, 0.70));
        knob.setStrokeWidth(1.2);
        // Clapper
        javafx.scene.shape.Line clapper = new javafx.scene.shape.Line(x + 9, y + 11, x + 9, y + 15);
        clapper.setStroke(Color.web(stroke, 0.70)); clapper.setStrokeWidth(1.5);
        Circle ball = new Circle(x + 9, y + 17, 2.5);
        ball.setFill(Color.web(stroke, 0.70));
        // Gentle swing
        RotateTransition swing = new RotateTransition(Duration.seconds(1.4), body);
        swing.setByAngle(12); swing.setAutoReverse(true);
        swing.setCycleCount(Animation.INDEFINITE);
        swing.setInterpolator(Interpolator.EASE_BOTH);
        swing.play();
        p.getChildren().addAll(body, knob, clapper, ball);
    }

    // ── Kawaii divider ────────────────────────────────────────────────────────
    static HBox kawaiiDivider(Theme t, double w) {
        // ...star... line ...star...
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER);
        row.setMaxWidth(w);

        Polygon s1 = KawaiiDeco.miniStar(6, t.accent, 0.55);
        Polygon s2 = KawaiiDeco.miniStar(5, t.gold, 0.50);
        Polygon s3 = KawaiiDeco.miniStar(6, t.accent, 0.55);

        Line line1 = dottedLine(w * 0.35, t);
        Line line2 = dottedLine(w * 0.35, t);

        row.getChildren().addAll(s1, line1, s2, line2, s3);
        return row;
    }

    private static Line dottedLine(double len, Theme t) {
        Line l = new Line(0, 0, len, 0);
        l.setStroke(Color.web(t.muted, 0.40));
        l.setStrokeWidth(1.5);
        l.getStrokeDashArray().addAll(4.0, 4.0);
        return l;
    }

    // ── Join panel ────────────────────────────────────────────────────────────
    private static VBox buildJoinPanel(CatCatchApp app, Theme t,
                                        TextField nameField, Label errorLabel) {
        VBox panel = new VBox(12);
        panel.setStyle(t.softCard());
        panel.setMaxWidth(300);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(18));

        Label ipLbl = new Label("主機 IP");
        ipLbl.setStyle(t.labelStyle(12, true));
        TextField ipField = new TextField();
        ipField.setPromptText("192.168.x.x");
        ipField.setStyle(t.inputStyle());
        ipField.setMaxWidth(280);

        Label codeLbl = new Label("房間號碼");
        codeLbl.setStyle(t.labelStyle(12, true));
        TextField codeField = new TextField();
        codeField.setPromptText("4 位數字房號");
        codeField.setStyle(t.inputStyle());
        codeField.setMaxWidth(280);
        codeField.setTextFormatter(new TextFormatter<>(c -> {
            String s = c.getControlNewText();
            // 只允許數字，最多 4 位
            if (s.matches("\\d{0,4}")) return c;
            return null;
        }));

        Button connectBtn = primaryBtn("連線加入", 280, t);
        connectBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String ip   = ipField.getText().trim();
            String code = codeField.getText().trim();
            if (name.isEmpty())     { errorLabel.setText("請先輸入名字！");          return; }
            if (ip.isEmpty())       { errorLabel.setText("請輸入主機 IP！");         return; }
            if (code.length() != 4) { errorLabel.setText("請輸入 4 位數字房號！");  return; }
            errorLabel.setText(" ");
            connectBtn.setDisable(true);
            connectBtn.setText("連線中...");
            doJoin(app, name, ip, code, connectBtn, errorLabel);
        });

        panel.getChildren().addAll(ipLbl, ipField, codeLbl, codeField, connectBtn);
        return panel;
    }

    // ── Host / Join logic ─────────────────────────────────────────────────────
    private static void doHost(CatCatchApp app, String name, Button btn, Label err) {
        new Thread(() -> {
            try {
                GameServer.EmbeddedServer srv = GameServer.startEmbedded(CatCatchApp.SERVER_PORT);
                CatCatchApp.embeddedServer = srv;
                Thread.sleep(200);
                // 啟動 UDP Discovery 伺服器，讓其他玩家能搜尋到這個房間
                CatCatchApp.startDiscovery();
            } catch (Exception ex) {
                resetBtn(btn, "建立房間", err, "伺服器無法啟動！"); return;
            }
            final GameClient[] ref = {null};
            GameClient.Listener l = new GameClient.Listener() {
                @Override public void onConnected(String id) { Platform.runLater(() -> app.goLobby(ref[0], true)); }
                @Override public void onState(GameClient.GameState s) {}
                @Override public void onError(String m) { CatCatchApp.stopServer(); resetBtn(btn, "建立房間", err, "建立失敗："+m); }
                @Override public void onDisconnected(String r) {}
            };
            ref[0] = new GameClient(l);
            try { ref[0].connectAndJoin("localhost", CatCatchApp.SERVER_PORT, name, "", true); }
            catch (Exception ex) { CatCatchApp.stopServer(); resetBtn(btn, "建立房間", err, "連線失敗"); }
        }, "host-thread").start();
    }

    private static void doJoin(CatCatchApp app, String name, String ip, String code,
                                Button btn, Label err) {
        new Thread(() -> {
            final GameClient[] ref = {null};
            GameClient.Listener l = new GameClient.Listener() {
                @Override public void onConnected(String id) { Platform.runLater(() -> app.goLobby(ref[0], false)); }
                @Override public void onState(GameClient.GameState s) {}
                @Override public void onError(String m) { resetBtn(btn, "連線加入", err, "連線失敗："+m); }
                @Override public void onDisconnected(String r) {}
            };
            ref[0] = new GameClient(l);
            try { ref[0].connectAndJoin(ip, CatCatchApp.SERVER_PORT, name, code, false); }
            catch (Exception ex) { resetBtn(btn, "連線加入", err, "連線失敗"); }
        }, "join-thread").start();
    }

    private static void resetBtn(Button b, String txt, Label err, String msg) {
        Platform.runLater(() -> { b.setDisable(false); b.setText(txt); err.setText(msg); });
    }

    // ── Button factories ──────────────────────────────────────────────────────

    static Button primaryBtn(String text, double width, Theme t) {
        Button b = new Button(text);
        b.setPrefWidth(width);
        b.setStyle(t.primaryBtn());
        b.setOnMouseEntered(e -> b.setStyle(t.primaryBtnHover()));
        b.setOnMouseExited(e  -> b.setStyle(t.primaryBtn()));
        b.setOnMousePressed(e -> b.setStyle(t.primaryBtnPressed()));
        b.setOnMouseReleased(e -> b.setStyle(t.primaryBtnHover()));
        return b;
    }

    static Button secondaryBtn(String text, double width, Theme t) {
        Button b = new Button(text);
        b.setPrefWidth(width);
        b.setStyle(t.secondaryBtn());
        b.setOnMouseEntered(e -> b.setStyle(t.secondaryBtnHover()));
        b.setOnMouseExited(e  -> b.setStyle(t.secondaryBtn()));
        return b;
    }

    static Button iconBtn(String text, String iconName, double iconSize, double width, Theme t) {
        ImageView iv = Icons.get(iconName, iconSize);
        Button b = new Button(text, iv);
        b.setGraphicTextGap(8);
        b.setContentDisplay(ContentDisplay.LEFT);
        b.setPrefWidth(width);
        b.setStyle(t.secondaryBtn());
        b.setOnMouseEntered(e -> b.setStyle(t.secondaryBtnHover()));
        b.setOnMouseExited(e  -> b.setStyle(t.secondaryBtn()));
        return b;
    }

    static Button primaryIconBtn(String text, String iconName, double iconSize, double width, Theme t) {
        ImageView iv = Icons.get(iconName, iconSize);
        Button b = new Button(text, iv);
        b.setGraphicTextGap(8);
        b.setContentDisplay(ContentDisplay.LEFT);
        b.setPrefWidth(width);
        b.setStyle(t.primaryBtn());
        b.setOnMouseEntered(e -> b.setStyle(t.primaryBtnHover()));
        b.setOnMouseExited(e  -> b.setStyle(t.primaryBtn()));
        b.setOnMousePressed(e -> b.setStyle(t.primaryBtnPressed()));
        b.setOnMouseReleased(e -> b.setStyle(t.primaryBtnHover()));
        return b;
    }
}
