package com.swingcraft4j.animatedchart;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;
import com.swingcraft4j.animatedchart.AnimatedChart.HoverRow;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.panel.AbstractOverlay;
import org.jfree.chart.panel.Overlay;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks what is under the mouse and paints a shadowed popup card with its values over the chart.
 */
class ChartHover extends AbstractOverlay implements Overlay {

    // fade in/out time and time constant of the card's slide toward the mouse, in milliseconds
    private static final float FADE = 140;
    private static final double SLIDE = 60;
    private static final int SHADOW_LAYERS = 8;

    private final AnimatedChart chart;
    private final ChartPanel panel;
    private final Timer timer = new Timer(16, e -> tick());
    private Point mouse;
    // index the card is for; kept while it fades out
    private int index = -1;
    private boolean hovering;
    private boolean popup = true;
    private float alpha;
    // how far, from 0 to 1, each index hovered lately is in focus, so that the focus eases from one index to the next
    private final Map<Integer, Float> focus = new HashMap<>();
    // top-left corner of the card as painted, sliding toward its place beside the mouse
    private double x;
    private double y;
    private long lastTick;

    ChartHover(AnimatedChart chart, ChartPanel panel) {
        this.chart = chart;
        this.panel = panel;
        MouseAdapter listener = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                update(e.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                update(e.getPoint());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                update(null);
            }
        };
        panel.addMouseListener(listener);
        panel.addMouseMotionListener(listener);
        panel.addOverlay(this);
    }

    /**
     * Index under the mouse, or -1.
     */
    int getIndex() {
        return hovering ? index : -1;
    }

    /**
     * How far, from 0 to 1, an index is emphasized now as the hovered one; it eases in and out.
     */
    float getFocus(int index) {
        return alpha * focus.getOrDefault(index, 0f);
    }

    /**
     * How far, from 0 to 1, an index is dimmed now because another one is hovered; it eases in and out.
     */
    float getDim(int index) {
        return alpha * (1 - focus.getOrDefault(index, 0f));
    }

    /**
     * Where the mouse last was over the data area, in panel coordinates, or {@code null} before it ever was.
     */
    Point getMouse() {
        return mouse;
    }

    boolean isPopup() {
        return popup;
    }

    void setPopup(boolean popup) {
        this.popup = popup;
        fireOverlayChanged();
    }

    private void update(Point point) {
        int before = getIndex();
        Rectangle2D dataArea = panel.getScreenDataArea();
        int hovered = point != null && dataArea.contains(point) ? chart.hoverAt(point, dataArea) : -1;
        hovering = hovered >= 0;
        if (hovering) {
            mouse = point;
            index = hovered;
            if (alpha == 0) {
                // appears beside the mouse instead of sliding in from where it last was
                Point2D.Double place = place();
                x = place.x;
                y = place.y;
                // and nothing else is in focus that it would have to ease over from
                focus.clear();
                focus.put(index, 1f);
            }
        }
        if (!timer.isRunning() && index >= 0) {
            lastTick = System.nanoTime();
            timer.start();
        }
        if (getIndex() != before) {
            chart.hoverChanged();
        }
        fireOverlayChanged();
    }

    private void tick() {
        long now = System.nanoTime();
        double elapsed = (now - lastTick) / 1e6;
        lastTick = now;

        float fade = (float) (elapsed / FADE);
        alpha = hovering ? Math.min(1, alpha + fade) : Math.max(0, alpha - fade);
        boolean settled = alpha == (hovering ? 1 : 0);
        if (hovering) {
            // the focus moves to the hovered index and off the others; once nothing is hovered it stays as it is
            focus.putIfAbsent(index, 0f);
            focus.replaceAll((key, value) -> key == index ? Math.min(1, value + fade) : value - fade);
            focus.values().removeIf(value -> value <= 0);
            if (focus.size() != 1 || focus.getOrDefault(index, 0f) < 1) {
                settled = false;
            }
            Point2D.Double place = place();
            double step = 1 - Math.exp(-elapsed / SLIDE);
            x += (place.x - x) * step;
            y += (place.y - y) * step;
            if (Math.abs(place.x - x) < 0.5 && Math.abs(place.y - y) < 0.5) {
                x = place.x;
                y = place.y;
            } else {
                settled = false;
            }
        } else if (settled) {
            index = -1;
        }
        if (settled) {
            timer.stop();
        }
        fireOverlayChanged();
    }

    /**
     * Where the card belongs: beside the mouse, flipped to its left near the right edge and kept inside the panel;
     * or, for a chart that wants an area kept clear, the first place around the mouse that leaves it clear.
     */
    private Point2D.Double place() {
        Dimension size = cardSize();
        int gap = UIScale.scale(14);
        int margin = UIScale.scale(10);
        Shape clear = chart.hoverKeepClear(index);
        if (clear != null) {
            double right = mouse.x + gap;
            double left = mouse.x - gap - size.width;
            double below = mouse.y + gap;
            double above = mouse.y - gap - size.height;
            double center = mouse.x - size.width / 2.0;
            double middle = mouse.y - size.height / 2.0;
            // beside the mouse, then at its corners, then under and over it
            double[][] places = {{right, middle}, {left, middle}, {right, above}, {right, below}, {left, above}, {left, below},
                    {center, below}, {center, above}};
            for (double[] place : places) {
                // moved inside the panel, which mustn't put it over the mouse
                double px = Math.clamp(place[0], margin, Math.max(margin, panel.getWidth() - margin - size.width));
                double py = Math.clamp(place[1], margin, Math.max(margin, panel.getHeight() - margin - size.height));
                Rectangle2D card = new Rectangle2D.Double(px, py, size.width, size.height);
                if (!card.intersects(mouse.x - gap / 2.0, mouse.y - gap / 2.0, gap, gap) && !clear.intersects(card)) {
                    return new Point2D.Double(px, py);
                }
            }
        }
        double px = mouse.x + gap;
        if (px + size.width > panel.getWidth() - margin) {
            px = mouse.x - gap - size.width;
        }
        double py = Math.clamp(mouse.y - size.height / 2.0, margin, Math.max(margin, panel.getHeight() - margin - size.height));
        return new Point2D.Double(Math.max(margin, px), py);
    }

    private Dimension cardSize() {
        FontMetrics metrics = panel.getFontMetrics(chart.getStyle().font());
        FontMetrics bold = panel.getFontMetrics(chart.getStyle().boldFont());
        String title = chart.hoverTitle(index);
        List<HoverRow> rows = chart.hoverRows(index);
        int names = 0;
        int numbers = 0;
        for (HoverRow row : rows) {
            names = Math.max(names, metrics.stringWidth(row.name()));
            numbers = Math.max(numbers, bold.stringWidth(row.value()));
        }
        int width = UIScale.scale(8 + 6) + names + UIScale.scale(18) + numbers;
        // a row is its text plus a gap above it, which the first row has only under a title
        int height = UIScale.scale(10) * 2 + rows.size() * (metrics.getHeight() + UIScale.scale(4)) - UIScale.scale(4);
        if (title != null) {
            width = Math.max(width, bold.stringWidth(title));
            height += bold.getHeight() + UIScale.scale(8);
        }
        return new Dimension(width + UIScale.scale(12) * 2, height);
    }

    @Override
    public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
        Rectangle2D dataArea = panel.getScreenDataArea();
        if (!popup || alpha <= 0 || index < 0 || !chart.isHoverShown(index, dataArea)) {
            return;
        }
        Graphics2D g = (Graphics2D) g2.create();
        try {
            FlatUIUtils.setRenderingHints(g);
            g.setComposite(AlphaComposite.SrcOver.derive(alpha));
            chart.paintHoverMarks(g, dataArea, index);
            paintCard(g);
        } finally {
            g.dispose();
        }
    }

    private void paintCard(Graphics2D g) {
        ChartStyle style = chart.getStyle();
        Dimension size = cardSize();
        float arc = UIScale.scale(10f);
        paintShadow(g, size, arc);
        RoundRectangle2D card = new RoundRectangle2D.Double(x, y, size.width, size.height, arc, arc);
        g.setColor(style.popupBackground());
        g.fill(card);
        g.setColor(style.popupBorder());
        g.setStroke(new BasicStroke(1));
        g.draw(card);

        Font font = style.font();
        Font boldFont = style.boldFont();
        FontMetrics metrics = g.getFontMetrics(font);
        FontMetrics bold = g.getFontMetrics(boldFont);
        float left = (float) x + UIScale.scale(12);
        float right = (float) x + size.width - UIScale.scale(12);
        float top = (float) y + UIScale.scale(10);

        String title = chart.hoverTitle(index);
        if (title != null) {
            g.setFont(boldFont);
            g.setColor(style.foreground());
            g.drawString(title, left, top + bold.getAscent());
            top += bold.getHeight() + UIScale.scale(8);
        }

        float dot = UIScale.scale(8f);
        for (HoverRow row : chart.hoverRows(index)) {
            float baseline = top + metrics.getAscent();
            g.setColor(row.color());
            g.fill(new Ellipse2D.Float(left, top + (metrics.getHeight() - dot) / 2, dot, dot));
            g.setFont(font);
            g.setColor(style.mutedForeground());
            g.drawString(row.name(), left + dot + UIScale.scale(6), baseline);
            g.setFont(boldFont);
            g.setColor(style.foreground());
            g.drawString(row.value(), right - bold.stringWidth(row.value()), baseline);
            top += metrics.getHeight() + UIScale.scale(4);
        }
    }

    /**
     * Soft drop shadow: stacked translucent outlines of the card, growing outward and shifted down.
     */
    private void paintShadow(Graphics2D g, Dimension size, float arc) {
        float spread = UIScale.scale(6f);
        float drop = UIScale.scale(1.5f);
        g.setColor(chart.getStyle().popupShadow());
        for (int i = 1; i <= SHADOW_LAYERS; i++) {
            float grow = spread * i / SHADOW_LAYERS;
            g.fill(new RoundRectangle2D.Double(x - grow, y - grow + drop, size.width + grow * 2, size.height + grow * 2,
                    arc + grow * 2, arc + grow * 2));
        }
    }
}
