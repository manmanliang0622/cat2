package catcatch;

/**
 * Neko Pastel – kawaii candy-sticker palette.
 * Each theme keeps the round-card, thick-border candy feel,
 * only the hue family shifts.
 */
public enum Theme {
    // warm cream + dusty rose (default)
    PINK("粉色",
        "#FDF6EE", "#FFFCF7", "#DDA8B8", "#B8788C",
        "#4A3438", "#FFFCF7", "#C0A8AC", "#ECC8D4",
        "#C8DFF0", "#C4E0D4", "#F0DFA8", "#D8C8E4"),

    // soft sky + powder blue
    BLUE("藍色",
        "#EEF5FA", "#F6FAFE", "#98C8E0", "#608DB0",
        "#2A3C50", "#F6FAFE", "#88AABF", "#C0D8EE",
        "#DDA8B8", "#C4E0D4", "#F0DFA8", "#D8C8E4"),

    // sage green + mint cream
    GREEN("綠色",
        "#EEF8F2", "#F6FCF8", "#96C8AC", "#6A9E80",
        "#2A3C30", "#F6FCF8", "#88B098", "#C4E4D0",
        "#DDA8B8", "#C8DFF0", "#F0DFA8", "#D8C8E4"),

    // soft lavender night
    DARK("夜色",
        "#22202E", "#2C2A3C", "#C0AAD4", "#9A80B4",
        "#E8E4F4", "#F0EDF8", "#6A6080", "#DCD0EC",
        "#AEE2FF", "#B5EAD7", "#FFE5A0", "#FFB3C6");

    /** label shown in settings */
    public final String label;
    /** page background */
    public final String bg;
    /** card fill */
    public final String panelBg;
    /** primary accent (button fill, code colour) */
    public final String accent;
    /** darker accent (border, hover) */
    public final String accentDark;
    /** main text */
    public final String text;
    /** text on accent buttons */
    public final String textOnAccent;
    /** muted / secondary text */
    public final String muted;
    /** button hover fill */
    public final String buttonHover;
    // extra palette slots used for decorations & secondary UI
    public final String sky, mint, gold, lavender;

    Theme(String label,
          String bg, String panelBg, String accent, String accentDark,
          String text, String textOnAccent, String muted, String buttonHover,
          String sky, String mint, String gold, String lavender) {
        this.label = label;
        this.bg = bg; this.panelBg = panelBg;
        this.accent = accent; this.accentDark = accentDark;
        this.text = text; this.textOnAccent = textOnAccent;
        this.muted = muted; this.buttonHover = buttonHover;
        this.sky = sky; this.mint = mint;
        this.gold = gold; this.lavender = lavender;
    }

    // ── Style strings ─────────────────────────────────────────────────────────

    public String rootStyle() {
        return "-fx-background-color:" + bg + ";" +
               "-fx-font-family:'jf-openhuninn','Microsoft JhengHei','Rounded Mplus 1c','Segoe UI',sans-serif;";
    }

    /** White card with coloured thick sticker-border + soft shadow */
    public String cardStyle() {
        return "-fx-background-color:" + panelBg + ";-fx-background-radius:24;" +
               "-fx-border-color:" + accentDark + ";-fx-border-radius:24;-fx-border-width:2.5;" +
               "-fx-effect:dropshadow(gaussian," + shadow() + ",14,0,3,5);";
    }

    /** Softer card variant (used for sub-panels) */
    public String softCard() {
        return "-fx-background-color:" + panelBg + ";-fx-background-radius:18;" +
               "-fx-border-color:" + muted + ";-fx-border-radius:18;-fx-border-width:2;" +
               "-fx-effect:dropshadow(gaussian," + shadow() + ",8,0,2,3);";
    }

    /** Candy / bubble button – filled accent */
    public String primaryBtn() {
        return "-fx-background-color:" + accent + ";-fx-text-fill:" + textOnAccent + ";" +
               "-fx-font-size:14px;-fx-font-weight:bold;-fx-background-radius:32;-fx-cursor:hand;" +
               "-fx-padding:12 34 12 34;" +
               "-fx-border-color:" + accentDark + ";-fx-border-radius:32;-fx-border-width:2.5;" +
               "-fx-effect:dropshadow(gaussian," + shadow() + ",8,0,2,4);";
    }

    public String primaryBtnHover() {
        return "-fx-background-color:" + buttonHover + ";-fx-text-fill:" + textOnAccent + ";" +
               "-fx-font-size:14px;-fx-font-weight:bold;-fx-background-radius:32;-fx-cursor:hand;" +
               "-fx-padding:12 34 12 34;" +
               "-fx-border-color:" + accentDark + ";-fx-border-radius:32;-fx-border-width:2.5;" +
               "-fx-effect:dropshadow(gaussian," + shadow() + ",12,0,2,6);";
    }

    public String primaryBtnPressed() {
        return "-fx-background-color:" + accentDark + ";-fx-text-fill:" + textOnAccent + ";" +
               "-fx-font-size:14px;-fx-font-weight:bold;-fx-background-radius:32;-fx-cursor:hand;" +
               "-fx-padding:13 34 11 34;" +         // shift down = press feel
               "-fx-border-color:" + accentDark + ";-fx-border-radius:32;-fx-border-width:2.5;";
    }

    /** Ghost button – white fill, coloured border */
    public String secondaryBtn() {
        return "-fx-background-color:" + panelBg + ";-fx-text-fill:" + text + ";" +
               "-fx-font-size:13px;-fx-background-radius:28;-fx-cursor:hand;" +
               "-fx-border-color:" + muted + ";-fx-border-radius:28;-fx-border-width:2;" +
               "-fx-padding:10 28 10 28;";
    }

    public String secondaryBtnHover() {
        return "-fx-background-color:" + bg + ";-fx-text-fill:" + accentDark + ";" +
               "-fx-font-size:13px;-fx-background-radius:28;-fx-cursor:hand;" +
               "-fx-border-color:" + accentDark + ";-fx-border-radius:28;-fx-border-width:2.5;" +
               "-fx-padding:10 28 10 28;" +
               "-fx-effect:dropshadow(gaussian," + shadow() + ",6,0,1,3);";
    }

    public String inputStyle() {
        boolean dark = this == DARK;
        return "-fx-background-color:" + (dark ? "#2C2840" : "#FFFFFF") + ";" +
               "-fx-border-color:" + muted + ";-fx-border-radius:16;-fx-background-radius:16;" +
               "-fx-border-width:2;-fx-padding:10 16;-fx-font-size:14px;" +
               "-fx-text-fill:" + text + ";";
    }

    public String labelStyle(int size, boolean bold) {
        return "-fx-font-size:" + size + "px;-fx-text-fill:" + text +
               (bold ? ";-fx-font-weight:bold;" : ";");
    }

    private String shadow() {
        return this == DARK ? "rgba(0,0,0,0.40)" : "rgba(160,110,110,0.13)";
    }
}
