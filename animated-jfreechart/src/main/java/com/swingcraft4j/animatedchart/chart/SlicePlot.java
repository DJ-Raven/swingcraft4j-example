package com.swingcraft4j.animatedchart.chart;

import com.formdev.flatlaf.util.UIScale;
import com.swingcraft4j.animatedchart.ChartStyle;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.PieSectionEntity;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.data.general.PieDataset;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.function.IntToDoubleFunction;

/**
 * Pie plot that paints its own slices: ring, half circle, rose, gaps, rounded corners and outside labels.
 */
class SlicePlot extends PiePlot<String> {

    // how far the hovered slice grows, and the parts of a label's leader line, before UI scaling
    private static final float HOVER_GROW = 5;
    private static final float LEADER_START = 4;
    private static final float LEADER_ELBOW = 16;
    private static final float LEADER_TAIL = 12;
    private static final float LABEL_GAP = 4;

    private LegendItemCollection legendItems = new LegendItemCollection();
    private Color[] colors = new Color[0];
    // 1 for a slice that is shown and 0 for a hidden one, with values in between while it animates
    private double[] presence = new double[0];
    // opacity of each slice now and how far it is grown as the hovered one, from 0 to 1; hovering eases both
    private IntToDoubleFunction opacity = slice -> 1;
    private IntToDoubleFunction focus = slice -> 0;
    private double sweep = 1;
    private double scale = 1;
    private double innerRatio;
    private float gap;
    private float cornerRadius;
    private boolean half;
    private boolean rose;
    private boolean labelsVisible;
    private String centerText;
    private Font centerFont;
    private Color centerColor;

    SlicePlot(PieDataset<String> dataset) {
        super(dataset);
    }

    void setLegendItems(LegendItemCollection legendItems) {
        this.legendItems = legendItems;
        fireChangeEvent();
    }

    @Override
    public LegendItemCollection getLegendItems() {
        return legendItems;
    }

    void setColors(Color[] colors) {
        this.colors = colors;
        fireChangeEvent();
    }

    /**
     * What gives the opacity of a slice and how far it is grown as the hovered one, each time the pie is painted.
     */
    void setHover(IntToDoubleFunction opacity, IntToDoubleFunction focus) {
        this.opacity = opacity;
        this.focus = focus;
    }

    /**
     * Color of a slice as painted now.
     */
    private Color color(int slice) {
        Color color = slice < colors.length ? colors[slice] : Color.GRAY;
        return ChartStyle.alpha(color, (int) Math.round(color.getAlpha() * opacity.applyAsDouble(slice)));
    }

    double getPresence(int slice) {
        return slice < presence.length ? presence[slice] : 1;
    }

    void setPresence(double[] presence) {
        this.presence = presence;
        fireChangeEvent();
    }

    boolean isIntroDone() {
        return sweep >= 1 && scale == 1;
    }

    /**
     * Swept fraction of the pie (clamped to 0 to 1) and scale of the pie; both 1 once the intro is done.
     */
    void setIntro(double sweep, double scale) {
        this.sweep = Math.clamp(sweep, 0, 1);
        this.scale = Math.max(0.001, scale);
        fireChangeEvent();
    }

    /**
     * Shape of the pie: the hole as a share of the radius, the gap between slices and their corner radius in pixels
     * before UI scaling, whether it is a half circle, and whether it is a rose with each slice's radius by its value.
     */
    void setShape(double innerRatio, float gap, float cornerRadius, boolean half, boolean rose) {
        this.innerRatio = innerRatio;
        this.gap = gap;
        this.cornerRadius = cornerRadius;
        this.half = half;
        this.rose = rose;
        fireChangeEvent();
    }

    boolean isLabelsVisible() {
        return labelsVisible;
    }

    void setLabelsVisible(boolean labelsVisible) {
        this.labelsVisible = labelsVisible;
        fireChangeEvent();
    }

    /**
     * Text in the middle of the pie, or {@code null} for none.
     */
    void setCenter(String text, Font font, Color color) {
        centerText = text;
        centerFont = font;
        centerColor = color;
        fireChangeEvent();
    }

    @Override
    protected void drawPie(Graphics2D g2, Rectangle2D area, PlotRenderingInfo info) {
        PieDataset<String> dataset = getDataset();
        int count = dataset.getItemCount();
        double[] values = new double[count];
        // what a slice's angle is proportional to: its value, or in a rose the same for every shown slice
        double[] share = new double[count];
        double shares = 0;
        double largest = 0;
        for (int i = 0; i < count; i++) {
            values[i] = Math.max(0, dataset.getValue(i).doubleValue());
            share[i] = rose ? getPresence(i) : values[i];
            shares += share[i];
            largest = Math.max(largest, values[i]);
        }
        if (shares <= 0) {
            return;
        }

        // room around the pie for the hovered slice to grow, and for the labels
        FontMetrics labelMetrics = g2.getFontMetrics(getLabelFont());
        double marginX = UIScale.scale(HOVER_GROW);
        double marginY = marginX;
        if (labelsVisible) {
            int widest = 0;
            for (int i = 0; i < count; i++) {
                widest = Math.max(widest, labelMetrics.stringWidth(dataset.getKey(i)));
            }
            marginX += UIScale.scale(LEADER_ELBOW + LEADER_TAIL + LABEL_GAP) + widest;
            marginY += UIScale.scale(LEADER_ELBOW) + labelMetrics.getHeight() / 2.0;
        }
        double width = area.getWidth() - marginX * 2;
        double height = area.getHeight() - marginY * 2;
        double radius = half ? Math.min(width / 2, height) : Math.min(width, height) / 2;
        if (radius <= 0) {
            return;
        }
        double x = area.getCenterX();
        // a half circle sits on its flat side, centered in the height
        double y = half ? area.getCenterY() + radius / 2 : area.getCenterY();
        double inner = radius * innerRatio;
        double start = half ? 180 : 90;
        double circle = half ? 180 : 360;

        Graphics2D plain = (Graphics2D) g2.create();
        Graphics2D slices = (Graphics2D) g2.create();
        try {
            for (Graphics2D g : new Graphics2D[]{plain, slices}) {
                g.translate(x, y);
                g.scale(scale, scale);
                g.translate(-x, -y);
            }
            if (sweep < 1) {
                // a wedge from the start angle, wide enough to cover the plot whatever its shape
                double reach = area.getWidth() + area.getHeight();
                slices.clip(new Arc2D.Double(x - reach, y - reach, reach * 2, reach * 2, start, -circle * sweep, Arc2D.PIE));
            }
            EntityCollection entities = info != null && info.getOwner() != null && isIntroDone()
                    ? info.getOwner().getEntityCollection() : null;

            double angle = start;
            for (int i = 0; i < count; i++) {
                double extent = circle * share[i] / shares;
                double outer = rose ? inner + (radius - inner) * (largest > 0 ? values[i] / largest : 0) : radius;
                outer += UIScale.scale(HOVER_GROW) * focus.applyAsDouble(i);
                Shape slice = slice(x, y, inner, outer, angle, extent);
                if (slice != null) {
                    slices.setPaint(color(i));
                    slices.fill(slice);
                    if (entities != null) {
                        entities.add(new PieSectionEntity(slice, dataset, 0, i, dataset.getKey(i), null, null));
                    }
                    // the label appears once the sweep has passed the middle of its slice
                    double middle = angle - extent / 2;
                    if (labelsVisible && extent > 1 && (start - middle) / circle <= sweep) {
                        drawLabel(plain, labelMetrics, dataset.getKey(i), i, x, y, outer, middle);
                    }
                }
                angle -= extent;
            }
            if (centerText != null) {
                FontMetrics metrics = plain.getFontMetrics(centerFont);
                // in a half circle the text stands on the flat side
                double baseline = half ? y - UIScale.scale(4) : y + (metrics.getAscent() - metrics.getDescent()) / 2.0;
                plain.setFont(centerFont);
                plain.setColor(centerColor);
                plain.drawString(centerText, (float) (x - metrics.stringWidth(centerText) / 2.0), (float) baseline);
            }
        } finally {
            plain.dispose();
            slices.dispose();
        }
    }

    /**
     * A slice from {@code start} clockwise over {@code extent} degrees, or {@code null} if it is too thin to draw.
     */
    private Shape slice(double x, double y, double inner, double outer, double start, double extent) {
        // corners a little less round than half the slice's thickness, so a thin slice still has a body
        double corner = Math.min(UIScale.scale(cornerRadius), (outer - inner) * 0.4);
        Shape rounded = slice(x, y, inner, outer, start, extent, corner);
        // a slice too narrow for its corners is drawn square instead of not at all
        return rounded != null || corner <= 0 ? rounded : slice(x, y, inner, outer, start, extent, 0);
    }

    private Shape slice(double x, double y, double inner, double outer, double start, double extent, double corner) {
        // the path is drawn smaller by the corner radius, then grown back with a round stroke
        double outerPath = outer - corner;
        double innerPath = inner > 0 ? inner + corner : 0;
        if (outerPath <= innerPath) {
            return null;
        }
        Path2D path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        if (extent >= 359.99) {
            // the only slice: a full ring, without sides
            path.append(new Ellipse2D.Double(x - outerPath, y - outerPath, outerPath * 2, outerPath * 2), false);
            if (innerPath > 0) {
                path.append(new Ellipse2D.Double(x - innerPath, y - innerPath, innerPath * 2, innerPath * 2), false);
            }
        } else {
            // each side is moved in by half the gap, parallel to itself, so the gap is as wide at the rim as at the hole
            double inset = UIScale.scale(gap) / 2 + corner;
            double halfExtent = Math.toRadians(extent) / 2;
            if (inset >= outerPath) {
                return null;
            }
            double outerInset = Math.asin(inset / outerPath);
            if (outerInset >= halfExtent) {
                return null;
            }
            path.append(arc(x, y, outerPath, start - Math.toDegrees(outerInset), -(extent - Math.toDegrees(outerInset) * 2)), false);
            double innerInset = innerPath > inset ? Math.asin(inset / innerPath) : Double.MAX_VALUE;
            if (innerInset < halfExtent) {
                path.append(arc(x, y, innerPath, start - extent + Math.toDegrees(innerInset), extent - Math.toDegrees(innerInset) * 2), true);
            } else {
                // the sides meet before the hole, at a point on the slice's middle line
                double tip = inset / Math.sin(halfExtent);
                if (tip >= outerPath) {
                    return null;
                }
                double middle = Math.toRadians(start) - halfExtent;
                path.lineTo(x + tip * Math.cos(middle), y - tip * Math.sin(middle));
            }
            path.closePath();
        }
        if (corner <= 0) {
            return path;
        }
        Area rounded = new Area(path);
        rounded.add(new Area(new BasicStroke((float) (corner * 2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(path)));
        return rounded;
    }

    private static Arc2D arc(double x, double y, double radius, double start, double extent) {
        return new Arc2D.Double(x - radius, y - radius, radius * 2, radius * 2, start, extent, Arc2D.OPEN);
    }

    /**
     * The slice's name beside it, joined by a leader line that bends to the horizontal.
     */
    private void drawLabel(Graphics2D g, FontMetrics metrics, String text, int slice, double x, double y, double outer, double middle) {
        double cos = Math.cos(Math.toRadians(middle));
        double sin = Math.sin(Math.toRadians(middle));
        double from = outer + UIScale.scale(LEADER_START);
        double elbow = outer + UIScale.scale(LEADER_ELBOW);
        int side = cos >= 0 ? 1 : -1;
        double elbowX = x + elbow * cos;
        double elbowY = y - elbow * sin;
        double tailX = elbowX + side * UIScale.scale(LEADER_TAIL);

        Path2D leader = new Path2D.Double();
        leader.moveTo(x + from * cos, y - from * sin);
        leader.lineTo(elbowX, elbowY);
        leader.lineTo(tailX, elbowY);
        g.setColor(color(slice));
        g.setStroke(new BasicStroke(1));
        g.draw(leader);

        double textX = side > 0 ? tailX + UIScale.scale(LABEL_GAP) : tailX - UIScale.scale(LABEL_GAP) - metrics.stringWidth(text);
        g.setFont(getLabelFont());
        g.setPaint(getLabelPaint());
        g.drawString(text, (float) textX, (float) (elbowY + (metrics.getAscent() - metrics.getDescent()) / 2.0));
    }
}
