package com.swingcraft4j.animatedchart.chart;

import com.formdev.flatlaf.util.UIScale;
import com.swingcraft4j.animatedchart.AnimatedChart;
import com.swingcraft4j.animatedchart.ChartStyle;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JFreeChart line chart over named categories, with line styles, an animated intro and animated value changes.
 */
public class LineChart extends AnimatedChart {

    /**
     * How the chart first appears.
     */
    public enum Intro {
        /**
         * Lines are drawn in from left to right.
         */
        DRAW("Draw"),
        /**
         * Lines rise from the bottom of the value range.
         */
        RISE("Rise");

        private final String name;

        Intro(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * How the points of a line are joined.
     */
    public enum LineStyle {
        /**
         * A round curve through the points; it may overshoot them on spiky data.
         */
        SMOOTH("Smooth"),
        /**
         * A curve through the points that never goes above or below them.
         */
        MONOTONE("Monotone"),
        /**
         * Straight segments from point to point.
         */
        STRAIGHT("Straight"),
        /**
         * Each value held flat across its category, with a vertical step between categories.
         */
        STEP("Step");

        private final String name;

        LineStyle(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final XYSeriesCollection dataset = new XYSeriesCollection();
    private final List<Boolean> dashed = new ArrayList<>();
    private final LineRenderer renderer = new LineRenderer();
    private final NumberAxis valueAxis = new NumberAxis();
    private final RevealPlot plot;
    private Intro intro = Intro.DRAW;
    private float lineWidth = 2.5f;
    private float pointRadius = 4;

    public LineChart(String... categories) {
        super(categories);

        valueAxis.setRange(0, 100);
        plot = new RevealPlot(dataset, new SpacedAxis(categories), valueAxis, renderer);

        renderer.setDrawOutlines(true);
        renderer.setUseOutlinePaint(true);
        renderer.setAreaPainter(this::paintArea);
        setChart(new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, true));
    }

    @Override
    protected void seriesAdded(int series) {
        XYSeries data = new XYSeries(getSeriesName(series), false, true);
        double[] values = getValues(series);
        for (int i = 0; i < values.length; i++) {
            data.add(i, values[i]);
        }
        dashed.add(false);
        dataset.addSeries(data);
    }

    @Override
    protected void valuesChanged() {
        plot.setReveal(1);
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
        if (intro == Intro.DRAW) {
            animator.stop();
            show(seriesValues(), targetOpacity());
            plot.setReveal(0);
            animator.start(plot::setReveal);
        } else {
            plot.setReveal(1);
            double[][] from = new double[getSeriesCount()][getCategoryCount()];
            for (double[] series : from) {
                Arrays.fill(series, valueAxis.getLowerBound());
            }
            morph(from);
        }
    }

    public Intro getIntro() {
        return intro;
    }

    public void setIntro(Intro intro) {
        this.intro = intro;
    }

    public LineStyle getLineStyle() {
        return renderer.getLineStyle();
    }

    public void setLineStyle(LineStyle lineStyle) {
        renderer.setLineStyle(lineStyle);
    }

    public boolean isSeriesDashed(int series) {
        return dashed.get(series);
    }

    /**
     * Draws one series with a dashed line, e.g. a target or forecast.
     */
    public void setSeriesDashed(int series, boolean dashed) {
        this.dashed.set(series, dashed);
        applyStyle();
    }

    /**
     * Fixed range of the value axis, so it doesn't rescale while values animate.
     */
    public void setValueRange(double lower, double upper) {
        valueAxis.setRange(lower, upper);
    }

    /**
     * Whether each line has its area filled: by default the area under it, with a fading gradient.
     */
    public void setAreaFilled(boolean filled) {
        renderer.setAreaFilled(filled);
    }

    public boolean isAreaFilled() {
        return renderer.isAreaFilled();
    }

    /**
     * Whether each line is colored by value: its full color at the top of the value range, fading to a tint at the bottom.
     */
    public void setValueGradient(boolean valueGradient) {
        renderer.setValueGradient(valueGradient);
    }

    public boolean isValueGradient() {
        return renderer.isValueGradient();
    }

    public void setPointsVisible(boolean visible) {
        renderer.setDefaultShapesVisible(visible);
    }

    public boolean isPointsVisible() {
        return renderer.getDefaultShapesVisible();
    }

    /**
     * Width of the lines, before UI scaling.
     */
    public float getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
        applyStyle();
    }

    /**
     * Radius of the points, before UI scaling.
     */
    public float getPointRadius() {
        return pointRadius;
    }

    public void setPointRadius(float pointRadius) {
        this.pointRadius = pointRadius;
        applyStyle();
    }

    /**
     * Opacity, from 0 to 1, of the area fill; under a line it is that of the top, fading to nothing at the axis.
     */
    public float getAreaOpacity() {
        return renderer.getAreaOpacity();
    }

    public void setAreaOpacity(float areaOpacity) {
        renderer.setAreaOpacity(Math.clamp(areaOpacity, 0, 1));
    }

    /**
     * Animates every series from {@code from} to its target values, and its opacity to shown or hidden.
     */
    private void morph(double[][] from) {
        double[][] to = seriesValues();
        float[] fadeFrom = displayedOpacity();
        float[] fadeTo = targetOpacity();
        animator.start(p -> {
            double[][] values = new double[to.length][];
            float[] opacity = new float[to.length];
            // overshooting easings would take the opacity out of range
            double fade = Math.clamp(p, 0, 1);
            for (int s = 0; s < to.length; s++) {
                values[s] = new double[to[s].length];
                for (int i = 0; i < to[s].length; i++) {
                    values[s][i] = lerp(from[s][i], to[s][i], p);
                }
                opacity[s] = (float) lerp(fadeFrom[s], fadeTo[s], fade);
            }
            onFrame(p);
            show(values, opacity);
        });
    }

    /**
     * Called on each frame of a value change with its progress, for charts that animate more than the values.
     */
    protected void onFrame(double progress) {
    }

    /**
     * Paints the area of a series, before its line; by default the area under the line, fading down to the axis.
     */
    protected void paintArea(Graphics2D g2, int series, Path2D line, Rectangle2D dataArea) {
        double base = valueY(valueAxis.getLowerBound(), dataArea);
        Path2D area = under(line, base, dataArea);
        Color color = getSeriesColor(series);
        float top = (float) area.getBounds2D().getMinY();
        g2.setPaint(new GradientPaint(0, top, ChartStyle.alpha(color, getAreaOpacity()), 0, (float) base, ChartStyle.alpha(color, 0)));
        g2.fill(area);
    }

    /**
     * The area from a line through every category down to a y of the panel.
     */
    protected Path2D under(Path2D line, double y, Rectangle2D dataArea) {
        Path2D area = new Path2D.Double(line);
        area.lineTo(categoryX(getCategoryCount() - 1, dataArea), y);
        area.lineTo(categoryX(0, dataArea), y);
        area.closePath();
        return area;
    }

    /**
     * Where the given values, one per category, are on the panel.
     */
    protected List<Point2D.Double> points(double[] values, Rectangle2D dataArea) {
        List<Point2D.Double> points = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            points.add(new Point2D.Double(categoryX(i, dataArea), valueY(values[i], dataArea)));
        }
        return points;
    }

    /**
     * Values of a series as shown now, while it animates.
     */
    protected double[] shownValues(int series) {
        double[] shown = new double[getCategoryCount()];
        for (int i = 0; i < shown.length; i++) {
            shown[i] = dataset.getYValue(series, i);
        }
        return shown;
    }

    /**
     * Opacity of a series as shown now: 1, or less while it fades out or in.
     */
    protected float shownOpacity(int series) {
        return renderer.getSeriesOpacity(series);
    }

    protected double valueY(double value, Rectangle2D dataArea) {
        return valueAxis.valueToJava2D(value, dataArea, plot.getRangeAxisEdge());
    }

    private void show(double[][] values, float[] opacity) {
        batch(() -> {
            for (int s = 0; s < values.length; s++) {
                XYSeries series = dataset.getSeries(s);
                for (int i = 0; i < values[s].length; i++) {
                    series.updateByIndex(i, values[s][i]);
                }
                renderer.setSeriesOpacity(s, opacity[s]);
            }
        });
    }

    private double[][] displayedValues() {
        double[][] shown = new double[getSeriesCount()][getCategoryCount()];
        for (int s = 0; s < shown.length; s++) {
            for (int i = 0; i < shown[s].length; i++) {
                shown[s][i] = dataset.getYValue(s, i);
            }
        }
        return shown;
    }

    private float[] displayedOpacity() {
        float[] shown = new float[getSeriesCount()];
        for (int s = 0; s < shown.length; s++) {
            shown[s] = renderer.getSeriesOpacity(s);
        }
        return shown;
    }

    private float[] targetOpacity() {
        float[] target = new float[getSeriesCount()];
        for (int s = 0; s < target.length; s++) {
            target[s] = isSeriesVisible(s) ? 1 : 0;
        }
        return target;
    }

    @Override
    protected NumberAxis getValueAxis() {
        return valueAxis;
    }

    @Override
    protected void setLegendItems(LegendItemCollection items) {
        plot.setFixedLegendItems(items);
    }

    @Override
    protected int hoverAt(Point2D point, Rectangle2D dataArea) {
        double value = plot.getDomainAxis().java2DToValue(point.getX(), dataArea, plot.getDomainAxisEdge());
        return Math.clamp(Math.round(value), 0, getCategoryCount() - 1);
    }

    /**
     * Not while the draw-in intro hasn't reached the category yet.
     */
    @Override
    protected boolean isHoverShown(int category, Rectangle2D dataArea) {
        return plot.isRevealed(categoryX(category, dataArea), dataArea);
    }

    /**
     * A vertical guide line at the category and an enlarged point on each visible line.
     */
    @Override
    protected void paintHoverMarks(Graphics2D g, Rectangle2D dataArea, int category) {
        double x = categoryX(category, dataArea);
        g.setColor(ChartStyle.mix(getStyle().foreground(), getStyle().background(), 0.3f));
        g.setStroke(new BasicStroke(1));
        g.draw(new Line2D.Double(x, dataArea.getMinY(), x, dataArea.getMaxY()));

        for (int s = 0; s < getSeriesCount(); s++) {
            if (!isSeriesVisible(s)) {
                continue;
            }
            double y = valueAxis.valueToJava2D(dataset.getYValue(s, category), dataArea, plot.getRangeAxisEdge());
            Color color = getSeriesColor(s);
            // a halo, then a ring of the background around a point a little larger than the others
            fillCircle(g, x, y, UIScale.scale(pointRadius + 6), ChartStyle.alpha(color, 60));
            fillCircle(g, x, y, UIScale.scale(pointRadius + 2.5f), getStyle().background());
            fillCircle(g, x, y, UIScale.scale(pointRadius + 0.5f), color);
        }
    }

    protected double categoryX(int category, Rectangle2D dataArea) {
        return plot.getDomainAxis().valueToJava2D(category, dataArea, plot.getDomainAxisEdge());
    }

    private static void fillCircle(Graphics2D g, double x, double y, float radius, Color color) {
        g.setColor(color);
        g.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
    }

    @Override
    protected void applyStyle() {
        super.applyStyle();
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(getStyle().gridVisible());
        plot.setRangeGridlinePaint(getStyle().gridColor());
        plot.setRangeGridlineStroke(new BasicStroke(1));
        plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
        getStyle().applyAxis(plot.getDomainAxis());
        getStyle().applyAxis(valueAxis);
        renderer.setBackground(getStyle().background());

        float radius = UIScale.scale(pointRadius);
        Shape point = new Ellipse2D.Float(-radius, -radius, radius * 2, radius * 2);
        for (int i = 0; i < getSeriesCount(); i++) {
            renderer.setSeriesPaint(i, getSeriesColor(i));
            float[] dash = dashed.get(i) ? new float[]{UIScale.scale(6f), UIScale.scale(7f)} : null;
            renderer.setSeriesStroke(i, new BasicStroke(UIScale.scale(lineWidth), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, dash, 0));
            renderer.setSeriesShape(i, point);
            // a ring of the background color around each point
            renderer.setSeriesOutlinePaint(i, getStyle().background());
            renderer.setSeriesOutlineStroke(i, new BasicStroke(UIScale.scale(2f)));
        }
    }
}
