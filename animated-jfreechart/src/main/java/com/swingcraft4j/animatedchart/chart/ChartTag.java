package com.swingcraft4j.animatedchart.chart;

import com.formdev.flatlaf.util.UIScale;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Small rounded text label painted on or beside a plot, e.g. to tag a value on an axis.
 */
final class ChartTag {

    private ChartTag() {
    }

    static int width(FontMetrics metrics, String text) {
        return metrics.stringWidth(text) + UIScale.scale(6) * 2;
    }

    static int height(FontMetrics metrics) {
        return metrics.getHeight() + UIScale.scale(2) * 2;
    }

    /**
     * Paints the label with its top-left corner at the point.
     */
    static void paint(Graphics2D g, FontMetrics metrics, String text, double x, double y, Color background, Color foreground) {
        float arc = UIScale.scale(6f);
        double left = Math.max(0, x);
        g.setColor(background);
        g.fill(new RoundRectangle2D.Double(left, y, width(metrics, text), height(metrics), arc, arc));
        g.setFont(metrics.getFont());
        g.setColor(foreground);
        g.drawString(text, (float) left + UIScale.scale(6), (float) y + UIScale.scale(2) + metrics.getAscent());
    }
}
