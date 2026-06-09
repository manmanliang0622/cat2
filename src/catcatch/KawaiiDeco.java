package catcatch;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Static helpers that draw pastel decorations and kawaii animations
 * on arbitrary Pane containers.
 */
public class KawaiiDeco {

    // ── Background decoration ─────────────────────────────────────────────────

    /** Paint soft clouds, stars and paw prints onto a background Pane. */
    public static void addBackground(Pane pane, Theme t, double w, double h) {
        // Clouds (pill shapes)
        addCloud(pane, 80,  60,  90, t.sky);
        addCloud(pane, w - 130, 80, 70, t.mint);
        addCloud(pane, w / 2 - 60, h - 80, 80, t.lavender);
        addCloud(pane, 40,  h - 120, 60, t.sky);

        // Soft stars
        double[][] stars = {
            {w * 0.15, h * 0.25, 10},
            {w * 0.80, h * 0.18, 12},
            {w * 0.65, h * 0.72, 9},
            {w * 0.30, h * 0.80, 8},
            {w * 0.90, h * 0.55, 10},
        };
        for (double[] s : stars) addStar(pane, s[0], s[1], s[2], t.gold);

        // Paw prints (two small circles + three tiny toe beans)
        double[][] paws = {{w * 0.08, h * 0.55}, {w * 0.88, h * 0.40}, {w * 0.50, h * 0.92}};
        for (double[] p : paws) addPaw(pane, p[0], p[1], t.accent, 0.22);
    }

    private static void addCloud(Pane p, double x, double y, double r, String color) {
        for (int[] d : new int[][]{{0,0,1},{-1,-1, 1},{1,0, 1},{-2,1, 1},{2,1,1}}) {
            Circle c = new Circle(r * 0.5 * d[2]);
            c.setFill(Color.web(color, 0.28));
            c.setTranslateX(x + d[0] * r * 0.38);
            c.setTranslateY(y + d[1] * r * 0.28);
            p.getChildren().add(c);
        }
    }

    private static void addStar(Pane p, double x, double y, double r, String color) {
        Polygon star = new Polygon();
        for (int i = 0; i < 5; i++) {
            double a1 = Math.toRadians(i * 72 - 90);
            double a2 = Math.toRadians(i * 72 - 90 + 36);
            star.getPoints().addAll(x + r * Math.cos(a1), y + r * Math.sin(a1),
                                    x + r * 0.42 * Math.cos(a2), y + r * 0.42 * Math.sin(a2));
        }
        star.setFill(Color.web(color, 0.45));
        star.setStroke(Color.web(color, 0.60));
        star.setStrokeWidth(1);
        p.getChildren().add(star);

        // gentle rotation animation
        RotateTransition rt = new RotateTransition(Duration.seconds(8 + Math.random() * 6), star);
        rt.setByAngle(360); rt.setCycleCount(Animation.INDEFINITE);
        rt.play();
    }

    private static void addPaw(Pane p, double x, double y, String color, double opacity) {
        // palm
        Circle palm = new Circle(10);
        palm.setFill(Color.web(color, opacity));
        palm.setTranslateX(x); palm.setTranslateY(y);
        p.getChildren().add(palm);
        // toe beans
        int[][] toes = {{-10,-10,5},{0,-14,5},{10,-10,5}};
        for (int[] t2 : toes) {
            Circle toe = new Circle(t2[2] * 0.5);
            toe.setFill(Color.web(color, opacity));
            toe.setTranslateX(x + t2[0]); toe.setTranslateY(y + t2[1]);
            p.getChildren().add(toe);
        }
    }

    // ── Click animations ──────────────────────────────────────────────────────

    /**
     * Correct click: burst of stars + hearts + "+10" in kawaii font.
     * All particles fall on the game canvas.
     */
    public static void correctClickBurst(Pane canvas, double cx, double cy, int delta) {
        String sign = delta > 0 ? "+" : "";

        // Score popup
        Label pop = new Label(sign + delta);
        pop.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#FF6FA8;" +
                     "-fx-background-color:rgba(255,230,240,0.90);-fx-background-radius:16;" +
                     "-fx-border-color:#FFB3C6;-fx-border-radius:16;-fx-border-width:2;" +
                     "-fx-padding:4 16;");
        pop.setLayoutX(cx - 36); pop.setLayoutY(cy - 20);
        canvas.getChildren().add(pop);

        animatePopup(canvas, pop, -80);

        // Stars & hearts burst
        String[] symbols = {"★","★","♡","★","♡"};
        String[] cols = {"#FFD700","#FFB3C6","#FF6FA8","#FFE5A0","#C4A8D4"};
        for (int i = 0; i < 5; i++) {
            Label particle = new Label(symbols[i]);
            particle.setStyle("-fx-font-size:" + (14 + (int)(Math.random() * 10)) + "px;" +
                              "-fx-text-fill:" + cols[i] + ";");
            double angle = (i / 5.0) * Math.PI * 2;
            double vx = Math.cos(angle) * 55;
            double vy = Math.sin(angle) * 55 - 20;
            particle.setLayoutX(cx + vx * 0.1);
            particle.setLayoutY(cy + vy * 0.1);
            canvas.getChildren().add(particle);

            ParallelTransition pt = new ParallelTransition(particle);
            TranslateTransition tt = new TranslateTransition(Duration.millis(600), particle);
            tt.setByX(vx); tt.setByY(vy);
            FadeTransition ft = new FadeTransition(Duration.millis(600), particle);
            ft.setFromValue(1); ft.setToValue(0);
            pt.getChildren().addAll(tt, ft);
            pt.setOnFinished(e -> canvas.getChildren().remove(particle));
            pt.play();
        }
    }

    /**
     * Wrong click: soft "Oops~" banner – warm orange, not scary.
     */
    public static void wrongClickOops(Pane canvas, double cx, double cy, int delta) {
        Label pop = new Label("Oops~  " + delta + " 分");
        pop.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#C07020;" +
                     "-fx-background-color:rgba(255,240,200,0.92);-fx-background-radius:16;" +
                     "-fx-border-color:#FFD080;-fx-border-radius:16;-fx-border-width:2;" +
                     "-fx-padding:4 16;");
        pop.setLayoutX(cx - 48); pop.setLayoutY(cy - 20);
        canvas.getChildren().add(pop);
        animatePopup(canvas, pop, -60);

        // Gentle wobble
        RotateTransition wob = new RotateTransition(Duration.millis(80), pop);
        wob.setByAngle(6); wob.setAutoReverse(true); wob.setCycleCount(6);
        wob.play();
    }

    private static void animatePopup(Pane canvas, Label pop, double byY) {
        ParallelTransition pt = new ParallelTransition(pop);
        TranslateTransition tt = new TranslateTransition(Duration.millis(900), pop);
        tt.setByY(byY);
        FadeTransition ft = new FadeTransition(Duration.millis(900), pop);
        ft.setFromValue(1); ft.setToValue(0);
        pt.getChildren().addAll(tt, ft);
        pt.setOnFinished(e -> canvas.getChildren().remove(pop));
        pt.play();
    }

    // ── Result star rating ────────────────────────────────────────────────────

    /**
     * Returns a HBox with 3 animated stars filled according to rank.
     * rank 1 → 3 stars, rank 2 → 2 stars, rank 3+ → 1 star.
     */
    public static javafx.scene.layout.HBox starRating(int rank) {
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10);
        row.setAlignment(Pos.CENTER);
        int filled = rank == 1 ? 3 : rank == 2 ? 2 : 1;
        for (int i = 0; i < 3; i++) {
            Label star = new Label("★");
            boolean on = (i < filled);
            star.setStyle("-fx-font-size:40px;-fx-text-fill:" + (on ? "#FFD700" : "#E0D0D0") + ";");
            if (on) {
                final int delay = i * 150;
                ScaleTransition st = new ScaleTransition(Duration.millis(350), star);
                st.setDelay(Duration.millis(delay));
                st.setFromX(0.2); st.setFromY(0.2);
                st.setToX(1.2);   st.setToY(1.2);
                st.setAutoReverse(true); st.setCycleCount(2);
                st.setInterpolator(Interpolator.EASE_OUT);
                st.play();
            }
            row.getChildren().add(star);
        }
        return row;
    }

    // ── Shared small star shape ───────────────────────────────────────────────

    public static Polygon miniStar(double r, String color, double opacity) {
        Polygon s = new Polygon();
        for (int i = 0; i < 5; i++) {
            double a1 = Math.toRadians(i * 72 - 90);
            double a2 = Math.toRadians(i * 72 - 90 + 36);
            s.getPoints().addAll(r * Math.cos(a1), r * Math.sin(a1),
                                 r * 0.42 * Math.cos(a2), r * 0.42 * Math.sin(a2));
        }
        s.setFill(Color.web(color, opacity));
        return s;
    }
}
