package com.swingcraft4j.animatedchart.chart;

import com.formdev.flatlaf.util.UIScale;
import com.swingcraft4j.animatedchart.AnimatedChart;
import com.swingcraft4j.animatedchart.ChartStyle;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarPainter;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.List;

/**
 * JFreeChart bar chart, grouped or stacked, upright or horizontal, with rounded bars, an animated intro and hover highlight.
 */
public class BarChart extends AnimatedChart {

    /**
     * How the chart first appears.
     */
    public enum Intro {
        /**
         * Every bar grows from the base at once.
         */
        GROW("Grow"),
        /**
         * Bars grow one category after another.
         */
        CASCADE("Cascade");

        private final String name;

        Intro(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // share of the duration over which the categories' start times are spread in the cascade
    private static final double CASCADE_SPREAD = 0.5;

    private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    private final BarRenderer groupedRenderer = new BarRenderer();
    private final StackedBarRenderer stackedRenderer = new StackedBarRenderer();
    private final NumberAxis valueAxis = new NumberAxis();
    private final CategoryPlot plot;
    private Intro intro = Intro.CASCADE;
    private float cornerRadius = 6;
    private boolean hoverHighlight = true;

    public BarChart(String... categories) {
        super(categories);

        CategoryAxis categoryAxis = new CategoryAxis();
        categoryAxis.setCategoryMargin(0.3);
        categoryAxis.setLowerMargin(0.02);
        categoryAxis.setUpperMargin(0.02);
        valueAxis.setRange(0, 100);
        plot = new CategoryPlot(dataset, categoryAxis, valueAxis, groupedRenderer);
        // on the left when upright and at the bottom, not the top, when horizontal
        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

        for (BarRenderer renderer : List.of(groupedRenderer, stackedRenderer)) {
            renderer.setBarPainter(new RoundedBarPainter());
            renderer.setShadowVisible(false);
            renderer.setDrawBarOutline(false);
        }
        groupedRenderer.setItemMargin(0.08);
        setChart(new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, true));
    }

    @Override
    protected void seriesAdded(int series) {
        double[] values = getValues(series);
        for (int i = 0; i < values.length; i++) {
            dataset.addValue(values[i], getSeriesName(series), getCategory(i));
        }
    }

    @Override
    protected void valuesChanged() {
        morph(displayedValues());
    }

    /**
     * A hidden series shrinks to the base and then gives up its place; a shown one takes its place and grows.
     */
    @Override
    protected void visibilityChanged(int series) {
        if (isSeriesVisible(series)) {
            setRendered(series, true);
        }
        morph(displayedValues());
    }

    @Override
    public void playIntro() {
        double base = valueAxis.getLowerBound();
        if (intro == Intro.GROW) {
            double[][] from = new double[getSeriesCount()][getCategoryCount()];
            for (double[] series : from) {
                Arrays.fill(series, base);
            }
            morph(from);
        } else {
            // each category starts a little later and eases on its own
            double[][] to = targets();
            int count = getCategoryCount();
            double step = count > 1 ? CASCADE_SPREAD / (count - 1) : 0;
            double span = 1 - step * (count - 1);
            animator.startLinear(t -> show((s, i) -> {
                double local = Math.clamp((t - i * step) / span, 0, 1);
                return lerp(base, to[s][i], getEasing().apply(local));
            }), this::syncRendered);
        }
    }

    public Intro getIntro() {
        return intro;
    }

    public void setIntro(Intro intro) {
        this.intro = intro;
    }

    /**
     * Fixed range of the value axis, so it doesn't rescale while values animate.
     */
    public void setValueRange(double lower, double upper) {
        valueAxis.setRange(lower, upper);
    }

    /**
     * Whether the series are stacked on each other in one bar per category, instead of side by side.
     */
    public boolean isStacked() {
        return plot.getRenderer() == stackedRenderer;
    }

    public void setStacked(boolean stacked) {
        plot.setRenderer(stacked ? stackedRenderer : groupedRenderer);
    }

    /**
     * Whether the bars run left to right, with the categories down the side.
     */
    public boolean isHorizontal() {
        return plot.getOrientation() == PlotOrientation.HORIZONTAL;
    }

    public void setHorizontal(boolean horizontal) {
        plot.setOrientation(horizontal ? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL);
    }

    /**
     * Radius of the bars' outer corners, before UI scaling; 0 is square.
     */
    public float getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
        getChart().fireChartChanged();
    }

    /**
     * Space between the categories, as a share of the axis' length.
     */
    public double getCategoryGap() {
        return plot.getDomainAxis().getCategoryMargin();
    }

    public void setCategoryGap(double categoryGap) {
        plot.getDomainAxis().setCategoryMargin(categoryGap);
    }

    /**
     * Space between the bars of a category when they are side by side, as a share of the axis' length.
     */
    public double getBarGap() {
        return groupedRenderer.getItemMargin();
    }

    public void setBarGap(double barGap) {
        groupedRenderer.setItemMargin(barGap);
    }

    /**
     * Whether hovering a category dims the bars of the other categories.
     */
    public boolean isHoverHighlight() {
        return hoverHighlight;
    }

    public void setHoverHighlight(boolean hoverHighlight) {
        this.hoverHighlight = hoverHighlight;
        getChart().fireChartChanged();
    }

    /**
     * Target values, with the hidden series at the base of the value axis.
     */
    private double[][] targets() {
        double[][] targets = seriesValues();
        for (int s = 0; s < targets.length; s++) {
            if (!isSeriesVisible(s)) {
                Arrays.fill(targets[s], valueAxis.getLowerBound());
            }
        }
        return targets;
    }

    /**
     * Animates every bar from {@code from} to its target value.
     */
    private void morph(double[][] from) {
        double[][] to = targets();
        animator.start(p -> show((s, i) -> lerp(from[s][i], to[s][i], p)), this::syncRendered);
    }

    /**
     * Sets every bar to the value the function gives for its series and category.
     */
    private void show(ValueFunction value) {
        batch(() -> {
            for (int s = 0; s < getSeriesCount(); s++) {
                for (int i = 0; i < getCategoryCount(); i++) {
                    dataset.setValue(value.at(s, i), getSeriesName(s), getCategory(i));
                }
            }
        });
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
     * Takes the hidden series out of the bar layout once they have shrunk away.
     */
    private void syncRendered() {
        for (int s = 0; s < getSeriesCount(); s++) {
            setRendered(s, isSeriesVisible(s));
        }
    }

    private void setRendered(int series, boolean rendered) {
        groupedRenderer.setSeriesVisible(series, rendered);
        stackedRenderer.setSeriesVisible(series, rendered);
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
        // the category whose middle is nearest, so the gaps between categories belong to one too
        double position = isHorizontal() ? point.getY() : point.getX();
        int nearest = -1;
        double distance = Double.MAX_VALUE;
        for (int i = 0; i < getCategoryCount(); i++) {
            double middle = plot.getDomainAxis().getCategoryMiddle(i, getCategoryCount(), dataArea, plot.getDomainAxisEdge());
            if (Math.abs(position - middle) < distance) {
                distance = Math.abs(position - middle);
                nearest = i;
            }
        }
        return nearest;
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
        for (int i = 0; i < getSeriesCount(); i++) {
            groupedRenderer.setSeriesPaint(i, getSeriesColor(i));
            stackedRenderer.setSeriesPaint(i, getSeriesColor(i));
        }
    }

    private interface ValueFunction {
        double at(int series, int column);
    }

    /**
     * Paints bars with rounded outer corners, dimmed outside the hovered category.
     */
    private class RoundedBarPainter implements BarPainter {

        @Override
        public void paintBar(Graphics2D g2, BarRenderer renderer, int row, int column, RectangularShape bar, RectangleEdge base) {
            Paint paint = renderer.getItemPaint(row, column);
            if (hoverHighlight && paint instanceof Color color) {
                // eases in and out, so the bars don't flash
                paint = ChartStyle.alpha(color, Math.round(color.getAlpha() * getHoverOpacity(column)));
            }
            g2.setPaint(paint);
            // in a stack only the segment at the end is rounded
            if (renderer != stackedRenderer || row == lastVisibleSeries()) {
                fillRounded(g2, bar.getFrame(), base);
            } else {
                g2.fill(bar.getFrame());
            }
        }

        /**
         * Fills the bar with its two corners away from the base rounded.
         */
        private void fillRounded(Graphics2D g2, Rectangle2D r, RectangleEdge base) {
            double radius = Math.min(UIScale.scale(cornerRadius), Math.min(r.getWidth(), r.getHeight()) / 2);
            if (radius <= 0) {
                g2.fill(r);
                return;
            }
            // a plain rectangle fills many times faster than a curved shape, so only the rounded end is one:
            // the bar is split where its corners end, on a whole pixel so that no seam shows
            AffineTransform transform = g2.getTransform();
            boolean upright = base == RectangleEdge.BOTTOM || base == RectangleEdge.TOP;
            // whether the rounded end is the bar's top or left one
            boolean low = base == RectangleEdge.BOTTOM || base == RectangleEdge.RIGHT;
            double scale = upright ? transform.getScaleY() : transform.getScaleX();
            double shift = upright ? transform.getTranslateY() : transform.getTranslateX();
            double from = upright ? r.getMinY() : r.getMinX();
            double to = upright ? r.getMaxY() : r.getMaxX();
            double pixel = (low ? from + radius : to - radius) * scale + shift;
            double split = ((low ? Math.ceil(pixel) : Math.floor(pixel)) - shift) / scale;
            boolean plain = transform.getShearX() == 0 && transform.getShearY() == 0 && scale > 0;
            if (!plain || split <= from || split >= to) {
                // too short to split
                g2.fill(shape(r, base, radius));
                return;
            }
            double end = low ? split - from : to - split;
            double rest = low ? to - split : split - from;
            // wider than the bar, so the clip cuts the rounded end only at the split
            double pad = 2;
            Rectangle2D cap = upright
                    ? new Rectangle2D.Double(r.getX() - pad, low ? from - pad : split, r.getWidth() + pad * 2, end + pad)
                    : new Rectangle2D.Double(low ? from - pad : split, r.getY() - pad, end + pad, r.getHeight() + pad * 2);
            Rectangle2D body = upright
                    ? new Rectangle2D.Double(r.getX(), low ? split : from, r.getWidth(), rest)
                    : new Rectangle2D.Double(low ? split : from, r.getY(), rest, r.getHeight());
            Shape clip = g2.getClip();
            g2.clip(cap);
            g2.fill(new RoundRectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight(), radius * 2, radius * 2));
            g2.setClip(clip);
            g2.fill(body);
        }

        @Override
        public void paintBarShadow(Graphics2D g2, BarRenderer renderer, int row, int column, RectangularShape bar, RectangleEdge base, boolean pegShadow) {
        }

        private int lastVisibleSeries() {
            for (int s = getSeriesCount() - 1; s >= 0; s--) {
                if (isSeriesVisible(s)) {
                    return s;
                }
            }
            return -1;
        }

        /**
         * The bar with its two corners away from the base rounded, as one shape.
         */
        private Shape shape(Rectangle2D r, RectangleEdge base, double radius) {
            Area shape = new Area(new RoundRectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight(), radius * 2, radius * 2));
            // square off the half at the base
            double w = r.getWidth() / 2;
            double h = r.getHeight() / 2;
            // RectangleEdge is a class of constants, not an enum, so no switch
            Rectangle2D square;
            if (base == RectangleEdge.BOTTOM) {
                square = new Rectangle2D.Double(r.getX(), r.getCenterY(), r.getWidth(), h);
            } else if (base == RectangleEdge.TOP) {
                square = new Rectangle2D.Double(r.getX(), r.getY(), r.getWidth(), h);
            } else if (base == RectangleEdge.LEFT) {
                square = new Rectangle2D.Double(r.getX(), r.getY(), w, r.getHeight());
            } else {
                square = new Rectangle2D.Double(r.getCenterX(), r.getY(), w, r.getHeight());
            }
            shape.add(new Area(square));
            return shape;
        }
    }
}
