package com.swingcraft4j.badgenotification;

import com.formdev.flatlaf.ui.FlatStylingSupport;
import com.formdev.flatlaf.ui.FlatStylingSupport.Styleable;

import javax.swing.*;
import java.awt.*;

/**
 * Animated notification badge shown on a corner of a component via {@link #wrap(JComponent)}.
 */
public class Badge {

    /**
     * Corner the badge sits on; leading/trailing follow the component's orientation.
     */
    public enum Position {TOP_TRAILING, TOP_LEADING, BOTTOM_TRAILING, BOTTOM_LEADING}

    @Styleable
    protected Color background;
    @Styleable
    protected Color foreground;
    @Styleable
    protected Color borderColor;
    @Styleable
    protected Font font;
    @Styleable
    protected int borderWidth = -1;
    @Styleable
    protected int height = -1;
    @Styleable
    protected int dotDiameter = -1;
    @Styleable
    protected int offsetX = Integer.MIN_VALUE;
    @Styleable
    protected int offsetY = Integer.MIN_VALUE;
    @Styleable
    protected Insets margin;

    private Position position = Position.TOP_TRAILING;
    private String text;
    private int count = -1;
    private int maxCount = 99;
    private boolean badgeVisible;
    private int alertCount;
    private JLayer<?> layer;

    /**
     * Wraps the component in a {@link JLayer} that paints this badge; add the returned layer to the container.
     */
    public <V extends JComponent> JLayer<V> wrap(V view) {
        JLayer<V> layer = new JLayer<>(view, new BadgeLayerUI<>(this));
        this.layer = layer;
        return layer;
    }

    /**
     * Applies a FlatLaf style string, e.g. {@code "background:$Actions.Blue;font:bold -1;height:18"}.
     */
    public void setStyle(String style) {
        FlatStylingSupport.parseAndApply(null, style,
                (key, v) -> FlatStylingSupport.applyToAnnotatedObject(this, key, v));
        if (layer != null) {
            layer.revalidate();
        }
        repaint();
    }

    public Position getPosition() {
        return position;
    }

    /**
     * Moves the badge to another corner of the component.
     */
    public void setPosition(Position position) {
        this.position = position != null ? position : Position.TOP_TRAILING;
        if (layer != null) {
            // the reserved room moves to the new corner's sides
            layer.revalidate();
        }
        repaint();
    }

    public String getText() {
        return text;
    }

    /**
     * Badge text; {@code null} or empty paints a dot.
     */
    public void setText(String text) {
        this.text = text;
        count = -1;
        repaint();
    }

    /**
     * Shows the count (capped at "{maxCount}+"), or hides the badge if the count is zero or less.
     */
    public void setCount(int count) {
        this.count = Math.max(0, count);
        text = countText();
        setBadgeVisible(count > 0);
    }

    public int getMaxCount() {
        return maxCount;
    }

    /**
     * Largest count shown as is; higher counts show as e.g. "99+" (default 99).
     */
    public void setMaxCount(int maxCount) {
        this.maxCount = Math.max(1, maxCount);
        if (count >= 0) {
            text = countText();
            repaint();
        }
    }

    private String countText() {
        return count > maxCount ? maxCount + "+" : String.valueOf(count);
    }

    public boolean isBadgeVisible() {
        return badgeVisible;
    }

    /**
     * Shows or hides the badge with a pop animation.
     */
    public void setBadgeVisible(boolean badgeVisible) {
        this.badgeVisible = badgeVisible;
        repaint();
    }

    /**
     * Plays the alert animation (pulse, wobble and ripple), showing the badge first if hidden.
     */
    public void alert() {
        badgeVisible = true;
        alertCount++;
        repaint();
    }

    /**
     * Sets the text and plays the alert animation.
     */
    public void alert(String text) {
        this.text = text;
        count = -1;
        alert();
    }

    /**
     * Sets the count and plays the alert animation, or hides the badge if the count is zero or less.
     */
    public void alert(int count) {
        setCount(count);
        if (count > 0) {
            alert();
        }
    }

    int getAlertCount() {
        return alertCount;
    }

    boolean isDot() {
        return text == null || text.isEmpty();
    }

    private void repaint() {
        if (layer != null) {
            // AnimatedIcon starts the animation on the next paint, once it sees the changed value
            layer.repaint();
        }
    }
}
