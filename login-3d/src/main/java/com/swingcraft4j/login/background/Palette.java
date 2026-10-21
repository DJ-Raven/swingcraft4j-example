package com.swingcraft4j.login.background;

import java.awt.Color;

/**
 * The colors of a macOS style theme: one of the macOS accent colors in the light or the dark appearance.
 */
public final class Palette {

    /**
     * The accent colors of macOS, with their shade in the light and in the dark appearance.
     */
    public enum Accent {
        BLUE("Blue", 0x007AFF, 0x0A84FF),
        PURPLE("Purple", 0xAF52DE, 0xBF5AF2),
        PINK("Pink", 0xFF2D55, 0xFF375F),
        RED("Red", 0xFF3B30, 0xFF453A),
        ORANGE("Orange", 0xFF9500, 0xFF9F0A),
        YELLOW("Yellow", 0xFFCC00, 0xFFD60A),
        GREEN("Green", 0x28CD41, 0x32D74B),
        GRAPHITE("Graphite", 0x8E8E93, 0x98989D);

        public final String title;
        private final int light;
        private final int dark;

        Accent(String title, int light, int dark) {
            this.title = title;
            this.light = light;
            this.dark = dark;
        }

        public Color color(boolean dark) {
            return new Color(dark ? this.dark : light);
        }
    }

    public final Accent accentKind;
    public final boolean dark;

    // the background gradient and the 3D scene from far to near
    public final Color top;
    public final Color bottom;
    public final Color far;
    public final Color near;

    public final Color accent;
    // the text on the accent
    public final Color accentForeground;

    // the card and the bar over the background, with the text and fields on them
    public final Color card;
    public final Color cardBorder;
    public final Color text;
    public final Color secondaryText;
    public final Color fieldBackground;
    public final Color fieldBorder;
    public final Color error;

    private Palette(Accent accentKind, boolean dark) {
        this.accentKind = accentKind;
        this.dark = dark;
        accent = accentKind.color(dark);
        accentForeground = accentKind == Accent.YELLOW ? new Color(0x1D1D1F) : Color.WHITE;
        if (dark) {
            top = mix(new Color(0x0B0B0F), accent, 0.10);
            bottom = mix(Color.BLACK, accent, 0.20);
            far = mix(accent, top, 0.5);
            near = mix(accent, Color.WHITE, 0.45);
            card = alpha(mix(new Color(0x1C1C1E), accent, 0.03), 205);
            cardBorder = new Color(255, 255, 255, 26);
            text = new Color(0xF5F5F7);
            secondaryText = new Color(0x98989D);
            fieldBackground = new Color(0x2A2A2D);
            fieldBorder = new Color(0x3A3A3C);
            error = new Color(0xFF453A);
        } else {
            top = mix(new Color(0xF5F5F7), accent, 0.05);
            bottom = mix(new Color(0xE3E3E8), accent, 0.16);
            far = mix(accent, Color.WHITE, 0.55);
            near = mix(accent, Color.BLACK, 0.1);
            card = new Color(255, 255, 255, 200);
            cardBorder = new Color(0, 0, 0, 22);
            text = new Color(0x1D1D1F);
            secondaryText = new Color(0x6E6E73);
            fieldBackground = Color.WHITE;
            fieldBorder = new Color(0xD1D1D6);
            error = new Color(0xFF3B30);
        }
    }

    public static Palette of(Accent accent, boolean dark) {
        return new Palette(accent, dark);
    }

    /**
     * The color from a (t = 0) to b (t = 1).
     */
    public static Color mix(Color a, Color b, double t) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    public static Color alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    /**
     * The color as #RRGGBB, or #RRGGBBAA when it is translucent, for a FlatLaf style.
     */
    public static String hex(Color c) {
        String rgb = String.format("#%06X", c.getRGB() & 0xFFFFFF);
        return c.getAlpha() == 255 ? rgb : rgb + String.format("%02X", c.getAlpha());
    }
}
