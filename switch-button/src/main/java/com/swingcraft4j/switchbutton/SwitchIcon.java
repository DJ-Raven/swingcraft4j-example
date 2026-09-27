package com.swingcraft4j.switchbutton;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.icons.FlatAnimatedIcon;
import com.formdev.flatlaf.ui.FlatStylingSupport;
import com.formdev.flatlaf.ui.FlatStylingSupport.Styleable;
import com.formdev.flatlaf.util.ColorFunctions;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Animated switch icon for a {@link JCheckBox} or {@link JToggleButton}; the thumb slides and colors blend on toggle.
 */
public class SwitchIcon extends FlatAnimatedIcon {

    public static final int DEFAULT_WIDTH = 36;
    public static final int DEFAULT_HEIGHT = 20;

    // ring width when the theme gives none
    private static final float DEFAULT_FOCUS_WIDTH = 2;
    private static final float FOCUS_GAP = 1;
    private static final int DEFAULT_DURATION = 200;
    // how much of the accent a soft part keeps, blended into the background
    private static final float SOFT_WEIGHT = 0.35f;
    private static final float HOVER_AMOUNT = 0.06f;
    // how much of each color a disabled switch keeps, blended into the background
    private static final float DISABLED_WEIGHT = 0.45f;

    @Styleable
    protected Color onColor;
    @Styleable
    protected Color offColor;
    @Styleable
    protected Color thumbColor;
    @Styleable
    protected int duration = -1;
    @Styleable
    protected boolean focusPainted = true;
    @Styleable
    protected float focusWidth = -1;

    private final SwitchType type;
    // components this icon was painted on, repainted when a setting changes
    private final Set<Component> components = Collections.newSetFromMap(new WeakHashMap<>());

    public SwitchIcon() {
        this(SwitchType.CLASSIC);
    }

    public SwitchIcon(SwitchType type) {
        this(type, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Switch of the given unscaled size, not counting the focus ring around it.
     */
    public SwitchIcon(SwitchType type, int width, int height) {
        super(width, height, null);
        this.type = type != null ? type : SwitchType.CLASSIC;
    }

    /**
     * Applies a FlatLaf style string, e.g. {@code "onColor:$Actions.Green;duration:300"}.
     */
    public void setStyle(String style) {
        FlatStylingSupport.parseAndApply(null, style,
                (key, v) -> FlatStylingSupport.applyToAnnotatedObject(this, key, v));
        repaintComponents();
    }

    public SwitchType getType() {
        return type;
    }

    /**
     * Color when selected; {@code null} uses {@code Switch.onColor}, then {@code Component.accentColor}.
     */
    public Color getOnColor() {
        return onColor;
    }

    public void setOnColor(Color onColor) {
        this.onColor = onColor;
        repaintComponents();
    }

    /**
     * Color when not selected; {@code null} uses {@code Switch.offColor}, then a shade of the background.
     */
    public Color getOffColor() {
        return offColor;
    }

    public void setOffColor(Color offColor) {
        this.offColor = offColor;
        repaintComponents();
    }

    /**
     * Color of white thumbs; {@code null} uses {@code Switch.thumbColor}, then white.
     */
    public Color getThumbColor() {
        return thumbColor;
    }

    public void setThumbColor(Color thumbColor) {
        this.thumbColor = thumbColor;
        repaintComponents();
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public boolean isFocusPainted() {
        return focusPainted;
    }

    /**
     * Whether the focus ring is painted when the button has focus.
     */
    public void setFocusPainted(boolean focusPainted) {
        this.focusPainted = focusPainted;
        repaintComponents();
    }

    /**
     * Focus ring width; {@code -1} uses {@code Switch.focusWidth}, then {@code CheckBox.icon.focusWidth}, then {@code Component.focusWidth}.
     */
    public float getFocusWidth() {
        return focusWidth;
    }

    public void setFocusWidth(float focusWidth) {
        this.focusWidth = focusWidth;
        repaintComponents();
    }

    // the icon grows with the focus ring, so the ring always fits inside it
    @Override
    public int getIconWidth() {
        return UIScale.scale(width + focusMargin() * 2);
    }

    @Override
    public int getIconHeight() {
        return UIScale.scale(height + focusMargin() * 2);
    }

    @Override
    public int getAnimationDuration() {
        return duration > 0 ? duration : DEFAULT_DURATION;
    }

    @Override
    public float getValue(Component c) {
        return c instanceof AbstractButton b && b.isSelected() ? 1 : 0;
    }

    @Override
    public void paintIconAnimated(Component c, Graphics g, int x, int y, float value) {
        // graphics is already antialiased and UI-scaled, so everything is in unscaled pixels
        if (c != null) {
            components.add(c);
        }
        Graphics2D g2 = (Graphics2D) g;
        float w = width;
        float h = height;
        int margin = focusMargin();
        g2.translate(x + margin, y + margin);

        Color background = c != null ? c.getBackground() : UIManager.getColor("Panel.background");
        Color accent = ColorFunctions.mix(resolveOn(), resolveOff(background), value);
        if (c instanceof AbstractButton b && b.isEnabled() && b.getModel().isRollover()) {
            accent = FlatLaf.isLafDark() ? ColorFunctions.lighten(accent, HOVER_AMOUNT) : ColorFunctions.darken(accent, HOVER_AMOUNT);
        }

        // disabled colors are faded into the background rather than painted translucent, so overlapping parts don't show through
        Color ringColor = accent;
        Color trackColor = tone(type.track, accent, background);
        Color thumbFill = tone(type.thumb, accent, background);
        if (c != null && !c.isEnabled()) {
            ringColor = ColorFunctions.mix(ringColor, background, DISABLED_WEIGHT);
            trackColor = ColorFunctions.mix(trackColor, background, DISABLED_WEIGHT);
            thumbFill = ColorFunctions.mix(thumbFill, background, DISABLED_WEIGHT);
        }

        // track, centered vertically
        float th = h * type.trackHeight;
        g2.setColor(trackColor);
        g2.fill(new RoundRectangle2D.Float(0, (h - th) / 2, w, th, th, th));

        // thumb, sliding from the leading to the trailing end
        float d = h * type.thumbSize;
        float inset = (h - d) / 2;
        boolean ltr = c == null || c.getComponentOrientation().isLeftToRight();
        float tx = inset + (w - d - inset * 2) * (ltr ? value : 1 - value);

        // focus ring follows the outer shape: the track when it's full height, else the thumb (drawn over the track, under the thumb)
        if (focusPainted && c instanceof AbstractButton b && b.isFocusable() && b.isFocusPainted() && b.hasFocus()) {
            if (type.trackHeight >= 1) {
                paintFocus(g2, 0, 0, w, h, background);
            } else {
                paintFocus(g2, tx, inset, d, d, background);
            }
        }

        if (type.thumbRing) {
            // ring color as a full disc, then the fill inside it, so no fill color bleeds past the outer edge
            float stroke = h * 0.1f;
            g2.setColor(ringColor);
            g2.fill(new Ellipse2D.Float(tx, inset, d, d));
            g2.setColor(thumbFill);
            g2.fill(new Ellipse2D.Float(tx + stroke, inset + stroke, d - stroke * 2, d - stroke * 2));
        } else {
            g2.setColor(thumbFill);
            g2.fill(new Ellipse2D.Float(tx, inset, d, d));
        }
    }

    /**
     * Solid pill-shaped ring around the given bounds, with a background-colored gap that cuts cleanly across the track.
     */
    private void paintFocus(Graphics2D g2, float x, float y, float w, float h, Color background) {
        Area gap = new Area(pill(x, y, w, h, FOCUS_GAP));
        gap.subtract(new Area(pill(x, y, w, h, 0)));
        g2.setColor(background);
        g2.fill(gap);

        // translucent focus colors are flattened onto the background so overlaps don't show
        Color focus = UIManager.getColor("Component.focusColor");
        focus = ColorFunctions.mix(new Color(focus.getRGB() & 0xffffff), background, focus.getAlpha() / 255f);
        Area ring = new Area(pill(x, y, w, h, FOCUS_GAP + resolveFocusWidth()));
        ring.subtract(new Area(pill(x, y, w, h, FOCUS_GAP)));
        g2.setColor(focus);
        g2.fill(ring);
    }

    /**
     * Pill around the given bounds, grown by {@code grow} on each side.
     */
    private static RoundRectangle2D pill(float x, float y, float w, float h, float grow) {
        float arc = h + grow * 2;
        return new RoundRectangle2D.Float(x - grow, y - grow, w + grow * 2, h + grow * 2, arc, arc);
    }

    private void repaintComponents() {
        for (Component c : components) {
            c.repaint();
        }
    }

    /**
     * Room kept around the switch for the gap and focus ring, in whole unscaled pixels.
     */
    private int focusMargin() {
        return (int) Math.ceil(FOCUS_GAP + resolveFocusWidth());
    }

    private Color tone(SwitchType.Tone tone, Color accent, Color background) {
        return switch (tone) {
            case ACCENT -> accent;
            case SOFT -> ColorFunctions.mix(accent, background, SOFT_WEIGHT);
            case THUMB -> resolveThumb();
        };
    }

    private Color resolveOn() {
        if (onColor != null) {
            return onColor;
        }
        Color color = UIManager.getColor("Switch.onColor");
        return color != null ? color : UIManager.getColor("Component.accentColor");
    }

    private Color resolveOff(Color background) {
        if (offColor != null) {
            return offColor;
        }
        Color color = UIManager.getColor("Switch.offColor");
        if (color != null) {
            return color;
        }
        return FlatLaf.isLafDark() ? ColorFunctions.lighten(background, 0.22f) : ColorFunctions.darken(background, 0.18f);
    }

    private Color resolveThumb() {
        if (thumbColor != null) {
            return thumbColor;
        }
        Color color = UIManager.getColor("Switch.thumbColor");
        return color != null ? color : Color.WHITE;
    }

    private float resolveFocusWidth() {
        if (focusWidth >= 0) {
            return focusWidth;
        }
        for (String key : new String[]{"Switch.focusWidth", "CheckBox.icon.focusWidth", "Component.focusWidth"}) {
            if (UIManager.get(key) instanceof Number n && n.floatValue() > 0) {
                return n.floatValue();
            }
        }
        return DEFAULT_FOCUS_WIDTH;
    }
}
