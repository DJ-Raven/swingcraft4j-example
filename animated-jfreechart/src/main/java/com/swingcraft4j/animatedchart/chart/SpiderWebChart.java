package com.swingcraft4j.animatedchart.chart;

import com.formdev.flatlaf.util.UIScale;
import com.swingcraft4j.animatedchart.AnimatedChart;
import com.swingcraft4j.animatedchart.ChartStyle;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.SpiderWebPlot;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntToDoubleFunction;

/**
 * JFreeChart spider web (radar) chart of series over named axes, with an animated intro and animated value changes.
 */
public class SpiderWebChart extends AnimatedChart {

    /**
     * How the chart first appears.
     */
    public enum Intro {
        /**
         * Every series grows out of the center.
         */
        EXPAND("Expand"),
        /**
         * The series are swept in clockwise from the top.
         */
        SWEEP("Sweep");

        private final String name;

        Intro(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // gap between the web and its labels, and how far outside the web the mouse still hovers it, before UI scaling
    private static final float LABEL_GAP = 10;
    private static final float HOVER_REACH = 16;
    // radius of the area around the center that hovers nothing, before UI scaling
    private static final float CENTER_GAP = 10;

    private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    private final WebPlot plot = new WebPlot();
    // opacity of each series as shown now, fading when it is hidden or shown
    private final List<Double> opacity = new ArrayList<>();
    private LegendItemCollection legendItems = new LegendItemCollection();
    private Intro intro = Intro.EXPAND;
    private double maxValue = 100;
    private int rings = 5;
    private boolean circular;
    private boolean areaFilled = true;
    private boolean pointsVisible = true;
    private boolean valueLabelsVisible = true;
    private float lineWidth = 2;
    private float pointRadius = 4;
    private float areaOpacity = 0.2f;
    // swept fraction of the series and their scale; both 1 once the intro is done
    private double sweep = 1;
    private double scale = 1;
    // center and radius of the web as last painted, and where each category's name is
    private double centerX;
    private double centerY;
    private double radius;
    private List<Rectangle2D> labels = List.of();

    /**
     * Creates a chart with one axis per category, clockwise from the top.
     */
    public SpiderWebChart(String... categories) {
        super(categories);
        setChart(new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, true));
    }

    @Override
    protected void seriesAdded(int series) {
        double[] values = getValues(series);
        for (int i = 0; i < values.length; i++) {
            dataset.addValue(values[i], getSeriesName(series), getCategory(i));
        }
        opacity.add(1.0);
    }

    @Override
    protected void valuesChanged() {
        sweep = 1;
        scale = 1;
        morph(displayedValues());
    }

    /**
     * A hidden series fades out and a shown one fades in.
     */
    @Override
    protected void visibilityChanged(int series) {
        valuesChanged();
    }

    @Override
    public void playIntro() {
        animator.stop();
        // straight to the target values, with every series shown or hidden
        batch(() -> {
            for (int s = 0; s < getSeriesCount(); s++) {
                opacity.set(s, isSeriesVisible(s) ? 1.0 : 0);
            }
            show(seriesValues());
        });
        if (intro == Intro.SWEEP) {
            animator.start(p -> setIntro(Math.clamp(p, 0, 1), 1));
        } else {
            // an overshooting easing can dip below 0, which would turn the web inside out
            animator.start(p -> setIntro(1, Math.max(0, p)));
        }
    }

    private void setIntro(double sweep, double scale) {
        this.sweep = sweep;
        this.scale = scale;
        getChart().fireChartChanged();
    }

    public Intro getIntro() {
        return intro;
    }

    public void setIntro(Intro intro) {
        this.intro = intro;
    }

    /**
     * Value at the rim of the web; the center is 0.
     */
    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
        getChart().fireChartChanged();
    }

    /**
     * Number of rings of the web, the outermost being its rim.
     */
    public int getRings() {
        return rings;
    }

    public void setRings(int rings) {
        this.rings = Math.max(1, rings);
        getChart().fireChartChanged();
    }

    /**
     * Whether the rings of the web are circles, instead of polygons with a corner on each axis.
     */
    public boolean isCircular() {
        return circular;
    }

    public void setCircular(boolean circular) {
        this.circular = circular;
        getChart().fireChartChanged();
    }

    /**
     * Whether the area inside each series is filled with a tint of its color.
     */
    public boolean isAreaFilled() {
        return areaFilled;
    }

    public void setAreaFilled(boolean areaFilled) {
        this.areaFilled = areaFilled;
        getChart().fireChartChanged();
    }

    /**
     * Opacity, from 0 to 1, of the area fill.
     */
    public float getAreaOpacity() {
        return areaOpacity;
    }

    public void setAreaOpacity(float areaOpacity) {
        this.areaOpacity = Math.clamp(areaOpacity, 0, 1);
        getChart().fireChartChanged();
    }

    public boolean isPointsVisible() {
        return pointsVisible;
    }

    public void setPointsVisible(boolean pointsVisible) {
        this.pointsVisible = pointsVisible;
        getChart().fireChartChanged();
    }

    /**
     * Whether each ring has its value on the first axis.
     */
    public boolean isValueLabelsVisible() {
        return valueLabelsVisible;
    }

    public void setValueLabelsVisible(boolean valueLabelsVisible) {
        this.valueLabelsVisible = valueLabelsVisible;
        getChart().fireChartChanged();
    }

    /**
     * Width of the lines, before UI scaling.
     */
    public float getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
        getChart().fireChartChanged();
    }

    /**
     * Radius of the points, before UI scaling.
     */
    public float getPointRadius() {
        return pointRadius;
    }

    public void setPointRadius(float pointRadius) {
        this.pointRadius = pointRadius;
        getChart().fireChartChanged();
    }

    /**
     * Animates every series from {@code from} to its target values, and its opacity to shown or hidden.
     */
    private void morph(double[][] from) {
        double[][] to = seriesValues();
        List<Double> fadeFrom = new ArrayList<>(opacity);
        animator.start(p -> {
            double[][] values = new double[to.length][getCategoryCount()];
            for (int s = 0; s < to.length; s++) {
                for (int i = 0; i < values[s].length; i++) {
                    values[s][i] = lerp(from[s][i], to[s][i], p);
                }
            }
            // overshooting easings would take the opacity out of range
            double fade = Math.clamp(p, 0, 1);
            batch(() -> {
                for (int s = 0; s < to.length; s++) {
                    opacity.set(s, lerp(fadeFrom.get(s), isSeriesVisible(s) ? 1 : 0, fade));
                }
                show(values);
            });
        });
    }

    private void show(double[][] values) {
        for (int s = 0; s < values.length; s++) {
            for (int i = 0; i < values[s].length; i++) {
                dataset.setValue(values[s][i], getSeriesName(s), getCategory(i));
            }
        }
    }

    private double[][] displayedValues() {
        double[][] shown = new double[getSeriesCount()][getCategoryCount()];
        for (int s = 0; s < shown.length; s++) {
            for (int i = 0; i < shown[s].length; i++) {
                shown[s][i] = dataset.getValue(s, i).doubleValue();
            }
        }
        return shown;
    }

    /**
     * The point on a category's axis at a share of the radius from the center, as last painted.
     */
    private Point2D.Double point(int category, double share) {
        double angle = Math.toRadians(90 - 360.0 * category / getCategoryCount());
        return new Point2D.Double(centerX + radius * share * Math.cos(angle), centerY - radius * share * Math.sin(angle));
    }

    /**
     * How far along its axis a value shown now is, from 0 at the center to 1 at the rim.
     */
    private double share(int series, int category) {
        return Math.max(0, dataset.getValue(series, category).doubleValue()) / maxValue * scale;
    }

    @Override
    protected NumberAxis getValueAxis() {
        return null;
    }

    @Override
    protected void setLegendItems(LegendItemCollection items) {
        legendItems = items;
        getChart().fireChartChanged();
    }

    /**
     * The category whose axis is nearest the point, if it is on the web or just outside it.
     */
    @Override
    protected int hoverAt(Point2D point, Rectangle2D dataArea) {
        // a category's name hovers it too
        for (int i = 0; i < labels.size(); i++) {
            if (labels.get(i).contains(point)) {
                return i;
            }
        }
        double dx = point.getX() - centerX;
        double dy = centerY - point.getY();
        double distance = Math.hypot(dx, dy);
        // not at the center, where the axes meet and the slightest move would go from one to another
        if (radius <= 0 || distance > radius + UIScale.scale(HOVER_REACH) || distance < UIScale.scale(CENTER_GAP)) {
            return -1;
        }
        // clockwise from the top, in turns
        double turn = (90 - Math.toDegrees(Math.atan2(dy, dx))) / 360;
        return Math.floorMod(Math.round(turn * getCategoryCount()), getCategoryCount());
    }

    /**
     * Not while the intro is still revealing the series.
     */
    @Override
    protected boolean isHoverShown(int category, Rectangle2D dataArea) {
        return sweep >= 1 && scale == 1;
    }

    /**
     * The category's axis with room for its points, so the popup card doesn't cover what it is about.
     */
    @Override
    protected Shape hoverKeepClear(int category) {
        Stroke room = new BasicStroke(UIScale.scale(pointRadius + 8) * 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        return room.createStrokedShape(new Line2D.Double(new Point2D.Double(centerX, centerY), point(category, 1)));
    }

    /**
     * The category's axis marked and an enlarged point on each visible series.
     */
    @Override
    protected void paintHoverMarks(Graphics2D g, Rectangle2D dataArea, int category) {
        ChartStyle style = getStyle();
        g.setColor(ChartStyle.mix(style.foreground(), style.background(), 0.3f));
        g.setStroke(new BasicStroke(1));
        g.draw(new Line2D.Double(new Point2D.Double(centerX, centerY), point(category, 1)));

        for (int s = 0; s < getSeriesCount(); s++) {
            if (!isSeriesVisible(s)) {
                continue;
            }
            Point2D.Double point = point(category, share(s, category));
            Color color = getSeriesColor(s);
            // a halo, then a ring of the background around a point a little larger than the others
            fillCircle(g, point, UIScale.scale(pointRadius + 6), ChartStyle.alpha(color, 60));
            fillCircle(g, point, UIScale.scale(pointRadius + 2.5f), style.background());
            fillCircle(g, point, UIScale.scale(pointRadius + 0.5f), color);
        }
    }

    private static void fillCircle(Graphics2D g, Point2D.Double center, float radius, Color color) {
        g.setColor(color);
        g.fill(new Ellipse2D.Double(center.x - radius, center.y - radius, radius * 2, radius * 2));
    }

    /**
     * Paints the web with its labels, then each series as a closed line over its filled area.
     */
    private class WebPlot extends SpiderWebPlot {

        WebPlot() {
            super(SpiderWebChart.this.dataset);
        }

        @Override
        public LegendItemCollection getLegendItems() {
            return legendItems;
        }

        @Override
        public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor, PlotState parentState, PlotRenderingInfo info) {
            getInsets().trim(area);
            if (info != null) {
                info.setPlotArea(area);
                info.setDataArea(area);
            }
            // room around the web for the widest label beside it and a label above and below it
            FontMetrics metrics = g2.getFontMetrics(getStyle().font());
            int widest = 0;
            for (int i = 0; i < getCategoryCount(); i++) {
                widest = Math.max(widest, metrics.stringWidth(getCategory(i)));
            }
            double gap = UIScale.scale(LABEL_GAP);
            centerX = area.getCenterX();
            centerY = area.getCenterY();
            radius = Math.max(0, Math.min(area.getWidth() / 2 - gap - widest, area.getHeight() / 2 - gap - metrics.getHeight()));
            if (radius <= 0 || getCategoryCount() == 0) {
                labels = List.of();
                return;
            }

            Graphics2D g = (Graphics2D) g2.create();
            try {
                drawWeb(g);
                drawLabels(g, metrics, gap);
                if (sweep < 1) {
                    // a wedge from the top, wide enough to cover the plot
                    double reach = area.getWidth() + area.getHeight();
                    g.clip(new Arc2D.Double(centerX - reach, centerY - reach, reach * 2, reach * 2, 90, -360 * sweep, Arc2D.PIE));
                }
                drawSeries(g);
            } finally {
                g.dispose();
            }
        }

        /**
         * The rings and the axes, if the style shows grid lines.
         */
        private void drawWeb(Graphics2D g) {
            if (!getStyle().gridVisible()) {
                return;
            }
            g.setColor(getStyle().gridColor());
            g.setStroke(new BasicStroke(1));
            for (int ring = 1; ring <= rings; ring++) {
                double share = (double) ring / rings;
                if (circular) {
                    double r = radius * share;
                    g.draw(new Ellipse2D.Double(centerX - r, centerY - r, r * 2, r * 2));
                } else {
                    g.draw(outline(category -> share));
                }
            }
            for (int i = 0; i < getCategoryCount(); i++) {
                g.draw(new Line2D.Double(new Point2D.Double(centerX, centerY), point(i, 1)));
            }
        }

        /**
         * Each category's name outside the end of its axis, and each ring's value on the first axis.
         */
        private void drawLabels(Graphics2D g, FontMetrics metrics, double gap) {
            ChartStyle style = getStyle();
            g.setFont(style.font());
            g.setColor(style.mutedForeground());
            List<Rectangle2D> bounds = new ArrayList<>();
            labels = bounds;
            for (int i = 0; i < getCategoryCount(); i++) {
                double angle = Math.toRadians(90 - 360.0 * i / getCategoryCount());
                double cos = Math.cos(angle);
                Point2D.Double end = point(i, (radius + gap) / radius);
                String text = getCategory(i);
                int width = metrics.stringWidth(text);
                // beside the axis on the left and right, centered on it at the top and bottom
                double x = Math.abs(cos) < 0.01 ? end.x - width / 2.0 : cos > 0 ? end.x : end.x - width;
                double middle = end.y - Math.sin(angle) * metrics.getHeight() / 2.0;
                bounds.add(new Rectangle2D.Double(x, middle - metrics.getHeight() / 2.0, width, metrics.getHeight()));
                g.drawString(text, (float) x, (float) (middle + (metrics.getAscent() - metrics.getDescent()) / 2.0));
            }
            if (!valueLabelsVisible) {
                return;
            }
            g.setFont(style.axisFont());
            FontMetrics small = g.getFontMetrics();
            int pad = UIScale.scale(3);
            // the rim's value would run into the first category's name
            for (int ring = 1; ring < rings; ring++) {
                String text = formatValue(maxValue * ring / rings);
                int width = small.stringWidth(text);
                double y = centerY - radius * ring / rings;
                // on a patch of the background, so the web doesn't run through the text
                g.setColor(style.background());
                g.fill(new Rectangle2D.Double(centerX - width / 2.0 - pad, y - small.getHeight() / 2.0, width + pad * 2, small.getHeight()));
                g.setColor(style.mutedForeground());
                g.drawString(text, (float) (centerX - width / 2.0), (float) (y + (small.getAscent() - small.getDescent()) / 2.0));
            }
        }

        private void drawSeries(Graphics2D g) {
            float point = UIScale.scale(pointRadius);
            Composite composite = g.getComposite();
            g.setStroke(new BasicStroke(UIScale.scale(lineWidth), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int s = 0; s < getSeriesCount(); s++) {
                float alpha = (float) Math.min(1, opacity.get(s));
                if (alpha <= 0) {
                    continue;
                }
                int series = s;
                Path2D shape = outline(category -> share(series, category));
                Color color = getSeriesColor(s);
                g.setComposite(AlphaComposite.SrcOver.derive(alpha));
                if (areaFilled) {
                    g.setColor(ChartStyle.alpha(color, areaOpacity));
                    g.fill(shape);
                }
                g.setColor(color);
                g.draw(shape);
                if (pointsVisible) {
                    for (int i = 0; i < getCategoryCount(); i++) {
                        // a ring of the background color around each point
                        Point2D.Double center = point(i, share(s, i));
                        fillCircle(g, center, point + UIScale.scale(1f), getStyle().background());
                        fillCircle(g, center, point - UIScale.scale(1f), color);
                    }
                }
            }
            g.setComposite(composite);
        }

        /**
         * A closed line through the point on each axis at the share of the radius the function gives for it.
         */
        private Path2D outline(IntToDoubleFunction share) {
            Path2D path = new Path2D.Double();
            for (int i = 0; i < getCategoryCount(); i++) {
                Point2D.Double point = point(i, share.applyAsDouble(i));
                if (i == 0) {
                    path.moveTo(point.x, point.y);
                } else {
                    path.lineTo(point.x, point.y);
                }
            }
            path.closePath();
            return path;
        }
    }
}
