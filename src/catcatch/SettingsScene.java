package catcatch;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.*;

public class SettingsScene {

    public static Scene build(CatCatchApp app) {
        Theme t = CatCatchApp.theme;

        StackPane fullRoot = new StackPane();
        fullRoot.setStyle(t.rootStyle());

        // Deco background
        javafx.scene.layout.Pane decoBg = new javafx.scene.layout.Pane();
        decoBg.setMouseTransparent(true);
        KawaiiDeco.addBackground(decoBg, t, CatCatchApp.WIDTH, CatCatchApp.HEIGHT);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:transparent;");
        root.setPadding(new Insets(28));

        fullRoot.getChildren().addAll(decoBg, root);

        // ── Top bar ───────────────────────────────────────────────────────────
        HBox topBar = new HBox(14);
        topBar.setAlignment(Pos.CENTER_LEFT);
        Button backBtn = MainMenuScene.iconBtn("返回主選單", "home", 15, 145, t);
        backBtn.setOnAction(e -> app.goMainMenu());
        Label title = new Label("設定");
        title.setStyle("-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:" + t.accentDark +
                       ";-fx-letter-spacing:1;");
        topBar.getChildren().addAll(backBtn, title);
        BorderPane.setMargin(topBar, new Insets(0, 0, 22, 0));
        root.setTop(topBar);

        // ── Settings card ─────────────────────────────────────────────────────
        VBox card = new VBox(24);
        card.setStyle(t.cardStyle());
        card.setPadding(new Insets(34, 44, 34, 44));
        card.setMaxWidth(560);

        // ── Theme section ─────────────────────────────────────────────────────
        HBox themeTitle = sectionTitle("介面主題", "coin", t);
        HBox themeRow = new HBox(12);
        themeRow.setAlignment(Pos.CENTER_LEFT);
        for (Theme th : Theme.values()) {
            boolean selected = th == CatCatchApp.theme;
            Button tb = new Button(th.label);
            tb.setPrefWidth(110); tb.setPrefHeight(46);
            applyThemeBtn(tb, th, selected);
            tb.setOnAction(e -> { CatCatchApp.theme = th; app.goSettings(); });
            themeRow.getChildren().add(tb);
        }

        // ── Sound section ─────────────────────────────────────────────────────
        HBox soundTitle = sectionTitle("音效設定", "speaker", t);

        HBox musicRow = new HBox(16);
        musicRow.setAlignment(Pos.CENTER_LEFT);
        Label musicLbl = new Label("背景音樂");
        musicLbl.setStyle(t.labelStyle(14, false));
        musicLbl.setMinWidth(90);
        final boolean[] musicOn = {CatCatchApp.musicEnabled};
        Button musicToggle = candyToggle(musicOn[0], t);
        musicToggle.setOnAction(e -> {
            musicOn[0] = !musicOn[0];
            CatCatchApp.musicEnabled = musicOn[0];
            CatCatchApp.audio.setMusicEnabled(musicOn[0]);
            applyToggle(musicToggle, musicOn[0], t);
        });
        musicRow.getChildren().addAll(musicLbl, musicToggle);

        HBox sfxRow = new HBox(16);
        sfxRow.setAlignment(Pos.CENTER_LEFT);
        Label sfxLbl = new Label("遊戲音效");
        sfxLbl.setStyle(t.labelStyle(14, false));
        sfxLbl.setMinWidth(90);
        final boolean[] sfxOn = {CatCatchApp.soundEnabled};
        Button sfxToggle = candyToggle(sfxOn[0], t);
        sfxToggle.setOnAction(e -> {
            sfxOn[0] = !sfxOn[0];
            CatCatchApp.soundEnabled = sfxOn[0];
            CatCatchApp.audio.setSfxEnabled(sfxOn[0]);
            applyToggle(sfxToggle, sfxOn[0], t);
        });
        sfxRow.getChildren().addAll(sfxLbl, sfxToggle);

        HBox volRow = new HBox(16);
        volRow.setAlignment(Pos.CENTER_LEFT);
        Label volLbl = new Label("音量");
        volLbl.setStyle(t.labelStyle(14, false));
        volLbl.setMinWidth(90);
        Slider volSlider = new Slider(0, 100, CatCatchApp.volume * 100);
        volSlider.setPrefWidth(250);
        volSlider.setShowTickLabels(true);
        volSlider.setShowTickMarks(true);
        volSlider.setMajorTickUnit(25);
        volSlider.setStyle("-fx-control-inner-background:" + t.bg + ";");
        Label volVal = new Label((int)(CatCatchApp.volume * 100) + "%");
        volVal.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + t.accentDark + ";-fx-min-width:42;");
        volSlider.valueProperty().addListener((ob, ov, nv) -> {
            float v = nv.floatValue() / 100f;
            CatCatchApp.volume = v;
            CatCatchApp.audio.setVolume(v);
            volVal.setText((int)(v * 100) + "%");
        });
        volRow.getChildren().addAll(volLbl, volSlider, volVal);

        // ── Info section ──────────────────────────────────────────────────────
        HBox infoTitle = sectionTitle("遊戲說明", "cat", t);

        VBox rules = new VBox(9);
        Object[][] ruleItems = {
            {"點擊「目標色」小貓",     "+10 分", t.mint,   "#1E6040"},
            {"點擊其他顏色小貓",       " -5 分", t.gold,   "#807010"},
            {"點擊地雷（狗）",         "-15 分", "#FFCDD2","#C03030"},
            {"每 5 秒切換目標顏色",    "",       t.sky,    "#2060A0"},
            {"45 秒倒數，時間到計算排名","",      t.lavender,"#604080"},
        };
        for (Object[] ri : ruleItems) {
            HBox ruleRow = new HBox(10);
            ruleRow.setAlignment(Pos.CENTER_LEFT);
            Circle dot = new Circle(5);
            dot.setFill(Color.web((String) ri[2]));
            dot.setStroke(Color.web((String) ri[3], 0.60));
            dot.setStrokeWidth(1.5);
            Label rl = new Label((String) ri[0]);
            rl.setStyle(t.labelStyle(13, false));
            ruleRow.getChildren().addAll(dot, rl);
            if (!((String) ri[1]).isEmpty()) {
                Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
                Label sc = new Label((String) ri[1]);
                sc.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + ri[3] + ";" +
                             "-fx-background-color:" + ri[2] + ";-fx-background-radius:8;-fx-padding:1 8;");
                ruleRow.getChildren().addAll(spacer, sc);
            }
            rules.getChildren().add(ruleRow);
        }

        card.getChildren().addAll(
            themeTitle, themeRow,
            deco(t),
            soundTitle, musicRow, sfxRow, volRow,
            deco(t),
            infoTitle, rules
        );

        StackPane centre = new StackPane(card);
        centre.setStyle("-fx-background-color:transparent;");
        root.setCenter(centre);

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

    private static Line deco(Theme t) {
        Line line = new Line(0, 0, 440, 0);
        line.setStroke(Color.web(t.muted, 0.30));
        line.setStrokeWidth(1.5);
        line.getStrokeDashArray().addAll(4.0, 5.0);
        return line;
    }

    private static Button candyToggle(boolean on, Theme t) {
        Button b = new Button(on ? "開啟" : "關閉");
        b.setPrefWidth(96);
        b.setStyle(on ? t.primaryBtn() : t.secondaryBtn());
        return b;
    }

    private static void applyToggle(Button b, boolean on, Theme t) {
        b.setText(on ? "開啟" : "關閉");
        b.setStyle(on ? t.primaryBtn() : t.secondaryBtn());
    }

    private static void applyThemeBtn(Button b, Theme th, boolean selected) {
        String base = "-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:16;-fx-cursor:hand;-fx-padding:8 0;";
        if (selected) {
            b.setStyle(base + "-fx-background-color:" + th.accent + ";-fx-text-fill:" + th.textOnAccent + ";" +
                       "-fx-border-color:" + th.accentDark + ";-fx-border-radius:16;-fx-border-width:3;" +
                       "-fx-effect:dropshadow(gaussian,rgba(180,120,140,0.25),8,0,1,3);");
        } else {
            b.setStyle(base + "-fx-background-color:" + th.panelBg + ";-fx-text-fill:" + th.text + ";" +
                       "-fx-border-color:" + th.muted + ";-fx-border-radius:16;-fx-border-width:2;");
        }
        b.setOnMouseEntered(e -> {
            if (th != CatCatchApp.theme)
                b.setStyle(base + "-fx-background-color:" + th.accent + "60;-fx-text-fill:" + th.accentDark + ";" +
                           "-fx-border-color:" + th.accentDark + ";-fx-border-radius:16;-fx-border-width:2;");
        });
        b.setOnMouseExited(e -> applyThemeBtn(b, th, th == CatCatchApp.theme));
    }
}
