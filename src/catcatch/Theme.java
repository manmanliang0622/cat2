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

    /** 浮動卡片：無硬框線，純陰影製造層次感 */
    public String cardStyle() {
        return "-fx-background-color:" + panelBg + ";-fx-background-radius:30;" +
               "-fx-effect:dropshadow(gaussian," + shadow() + ",26,0.04,0,8);";
    }

    /** 子面板柔和卡片 */
    public String softCard() {
        return "-fx-background-color:" + panelBg + ";-fx-background-radius:22;" +
               "-fx-effect:dropshadow(gaussian," + shadow() + ",14,0.02,0,4);";
    }

    /** 泡泡糖按鈕：無硬框線，靠陰影和色塊呈現立體感 */
    public String primaryBtn() {
        return "-fx-background-color:" + accent + ";-fx-text-fill:" + textOnAccent + ";" +
               "-fx-font-size:14px;-fx-font-weight:bold;-fx-background-radius:40;-fx-cursor:hand;" +
               "-fx-padding:13 38 13 38;" +
               "-fx-effect:dropshadow(gaussian," + btnShadow() + ",14,0.10,0,5);";
    }

    public String primaryBtnHover() {
        return "-fx-background-color:" + buttonHover + ";-fx-text-fill:" + textOnAccent + ";" +
               "-fx-font-size:14px;-fx-font-weight:bold;-fx-background-radius:40;-fx-cursor:hand;" +
               "-fx-padding:13 38 13 38;" +
               "-fx-effect:dropshadow(gaussian," + btnShadow() + ",20,0.15,0,9);";
    }

    public String primaryBtnPressed() {
        return "-fx-background-color:" + accentDark + ";-fx-text-fill:" + textOnAccent + ";" +
               "-fx-font-size:14px;-fx-font-weight:bold;-fx-background-radius:40;-fx-cursor:hand;" +
               "-fx-padding:15 38 11 38;" +
               "-fx-effect:dropshadow(gaussian," + btnShadow() + ",5,0,0,1);";
    }

    /** 幽靈按鈕：淡色背景 + 細柔邊線 */
    public String secondaryBtn() {
        return "-fx-background-color:" + bg + ";-fx-text-fill:" + text + ";" +
               "-fx-font-size:13px;-fx-background-radius:34;-fx-cursor:hand;" +
               "-fx-border-color:" + accent + "88;-fx-border-radius:34;-fx-border-width:1.5;" +
               "-fx-padding:11 30 11 30;" +
               "-fx-effect:dropshadow(gaussian," + shadow() + ",8,0,0,2);";
    }

    public String secondaryBtnHover() {
        return "-fx-background-color:" + accent + "30;-fx-text-fill:" + accentDark + ";" +
               "-fx-font-size:13px;-fx-background-radius:34;-fx-cursor:hand;" +
               "-fx-border-color:" + accentDark + "AA;-fx-border-radius:34;-fx-border-width:1.5;" +
               "-fx-padding:11 30 11 30;" +
               "-fx-effect:dropshadow(gaussian," + shadow() + ",14,0.06,0,5);";
    }

    public String inputStyle() {
        boolean dark = this == DARK;
        return "-fx-background-color:" + (dark ? "#2C2840" : "#FFFFFF") + ";" +
               "-fx-border-color:" + accent + "88;-fx-border-radius:18;-fx-background-radius:18;" +
               "-fx-border-width:1.5;-fx-padding:10 16;-fx-font-size:14px;" +
               "-fx-text-fill:" + text + ";" +
               "-fx-effect:dropshadow(gaussian," + shadow() + ",6,0,0,2);";
    }

    public String labelStyle(int size, boolean bold) {
        return "-fx-font-size:" + size + "px;-fx-text-fill:" + text +
               (bold ? ";-fx-font-weight:bold;" : ";");
    }

    private String shadow() {
        return this == DARK ? "rgba(0,0,0,0.38)" : "rgba(180,130,150,0.14)";
    }

    private String btnShadow() {
        return this == DARK ? "rgba(0,0,0,0.50)" : "rgba(200,130,155,0.40)";
    }
}
