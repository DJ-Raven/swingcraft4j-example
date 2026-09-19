package com.swingcraft4j.treehoveraction;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.ColorFunctions;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.*;
import java.awt.*;

/**
 * A row of small trailing icon buttons (e.g. add / edit / more) painted inside a tree cell,
 * right-aligned and only meaningful for whichever row the owning tree reports as hovered.
 * The owning tree tracks mouse position and rollover state; this class only paints the icons
 * and hit-tests a point against them.
 */
public class HoverAction {

    public Item[] getItems() {
        return items;
    }

    public void setItems(Item[] items) {
        this.items = items;
    }

    public ActionVisible getActionVisible() {
        return actionVisible;
    }

    public void setActionVisible(ActionVisible actionVisible) {
        this.actionVisible = actionVisible;
    }

    public Insets getInsets() {
        return insets;
    }

    public void setInsets(Insets insets) {
        this.insets = insets;
    }

    public int getGap() {
        return gap;
    }

    public void setGap(int gap) {
        this.gap = gap;
    }

    public int getMargin() {
        return margin;
    }

    public void setMargin(int margin) {
        this.margin = margin;
    }

    public HoverAction(Item[] items) {
        this.items = items;
    }

    private Item[] items;
    private ActionVisible actionVisible;
    private Insets insets = new Insets(0, 3, 0, 3);
    private int gap = 0;
    private int margin = 2;

    /**
     * Paints the visible action icons for one row, right-aligned, highlighting {@code rolloverIndex}.
     */
    protected void paint(Component com, Graphics g, boolean selected, boolean leaf, int row, int rolloverIndex) {
        if (items == null || items.length == 0) return;

        int width = com.getWidth();
        int height = com.getHeight();
        int sGap = UIScale.scale(gap);
        int sMargin = UIScale.scale(margin);
        int x = width - UIScale.scale(insets.right);
        for (int i = items.length - 1; i >= 0; i--) {
            Icon icon = items[i].icon;
            if (icon != null && (actionVisible == null || actionVisible.isVisible(i, leaf, row))) {
                int w = icon.getIconWidth() + sMargin * 2;
                int h = icon.getIconHeight() + sMargin * 2;
                int y = (height - w) / 2;
                x -= w;

                // paint background
                if (i == rolloverIndex) {
                    float v = selected ? 0.15f : 0.08f;
                    if (FlatLaf.isLafDark()) {
                        g.setColor(ColorFunctions.tint(com.getBackground(), v));
                    } else {
                        g.setColor(ColorFunctions.shade(com.getBackground(), v));
                    }
                    FlatUIUtils.paintComponentBackground((Graphics2D) g, x, y, w, h, 0, UIScale.scale(5));
                }

                // paint icon
                icon.paintIcon(com, g, x + sMargin, y + sMargin);
                x -= sGap;
            }
        }
    }

    /**
     * Returns which action icon contains {@code point} (for hover/click hit-testing), or -1 if none.
     */
    protected int getIndexAtPoint(Rectangle bounds, Point point, boolean leaf, int row) {
        int sGap = UIScale.scale(gap);
        int sMargin = UIScale.scale(margin);
        int x = bounds.width - UIScale.scale(insets.right);
        for (int i = items.length - 1; i >= 0; i--) {
            Icon icon = items[i].icon;
            if (icon != null && (actionVisible == null || actionVisible.isVisible(i, leaf, row))) {
                int w = icon.getIconWidth() + sMargin * 2;
                int h = icon.getIconHeight() + sMargin * 2;
                x -= w;
                if (inside(point, x, bounds.y + (bounds.height - h) / 2, w, h)) {
                    return i;
                }
                x -= sGap;
            }
        }
        return -1;
    }

    private boolean inside(Point point, int x, int y, int width, int height) {
        if ((width | height) < 0) return false;
        if (point.x < x || point.y < y) return false;

        width += x;
        height += y;
        return ((width < x || width > point.x) && (height < y || height > point.y));
    }

    /**
     * One clickable action icon, with an optional tooltip.
     */
    public static class Item {

        public Icon getIcon() {
            return icon;
        }

        public String getTooltip() {
            return tooltip;
        }

        public Item(Icon icon, String tooltip) {
            this.icon = icon;
            this.tooltip = tooltip;
        }

        public Item(Icon icon) {
            this(icon, null);
        }

        private final Icon icon;
        private final String tooltip;
    }

    /**
     * Decides per-row whether a given action icon should be shown at all.
     */
    public interface ActionVisible {

        boolean isVisible(int actionIndex, boolean leaf, int row);
    }
}
