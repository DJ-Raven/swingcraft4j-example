package com.swingcraft4j.animatedchart.chart;

import com.formdev.flatlaf.util.UIScale;
import com.swingcraft4j.animatedchart.AnimatedChart;
import com.swingcraft4j.animatedchart.ChartStyle;
import com.swingcraft4j.animatedchart.ValueFormatter;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntToDoubleFunction;

/**
 * JFreeChart bubble chart: series of bubbles placed by two values and sized by a third, with an animated intro,
 * animated value changes and hover highlight.
 */
public class BubbleChart extends AnimatedChart {

    /**
     * How the chart first appears.
     */
    public enum Intro {
        /**
         * Every bubble grows from nothing at once.
         */
        GROW("Grow"),
        /**
         * Bubbles grow one after another, from left to right.
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

    /**
     * A bubble as painted: its series, its index in the series, and its center and radius on the panel.
     */
    private record Bubble(int series, int index, double x, double y, double radius) {
    }

    // what is kept of each bubble
    private static final int X = 0;
    private static final int Y = 1;
    private static final int SIZE = 2;
    private static final int PARTS = 3;
    // share of the duration over which the bubbles' start times are spread in the cascade
    private static final double CASCADE_SPREAD = 0.5;
    // radius of the smallest bubble, and of the area around a bubble's center that hovers it, before UI scaling
    private static final float MIN_RADIUS = 2;
    private static final float HIT_RADIUS = 6;

    private final BubbleDataset dataset = new BubbleDataset();
    private final NumberAxis xAxis = new NumberAxis();
    private final NumberAxis yAxis = new NumberAxis();
    private final XYPlot plot;
    private final DecimalFormat numberFormat = new DecimalFormat("#,##0.#");
    // the bubbles of each series, by part then bubble: the target values and what is shown now
    private final List<double[][]> bubbles = new ArrayList<>();
    private final List<double[][]> shown = new ArrayList<>();
    // labels of each series' bubbles, or null for a series without any
    private final List<String[]> labels = new ArrayList<>();
    // opacity of each series as shown now, fading when it is hidden or shown
    private final List<Double> opacity = new ArrayList<>();
    private Intro intro = Intro.CASCADE;
    private ValueFormatter xFormatter;
    private ValueFormatter sizeFormatter;
    private String xName = "X";
    private String yName = "Y";
    private String sizeName = "Size";
    private boolean axisNamesVisible;
    // size drawn at the largest radius: fixed, or 0 for the largest size of the chart, and what it is as shown now
    private double maxSize;
    private double shownMaxSize;
    private float maxRadius = 34;
    private float bubbleOpacity = 0.55f;
    private boolean outlineVisible = true;
    private boolean labelsVisible = true;
    private boolean hoverHighlight = true;
    // true while the intro is still growing the bubbles
    private boolean growing;

    public BubbleChart() {
        // the series aren't over shared categories: each has bubbles of its own
        xAxis.setRange(0, 100);
        yAxis.setRange(0, 100);
        plot = new XYPlot(dataset, xAxis, yAxis, new BubbleRenderer());
        setChart(new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, true));
    }

    /**
     * Adds a series of bubbles, one per index of the arrays; a {@code null} color uses the style's palette.
     */
    public void addSeries(String name, Color color, double[] x, double[] y, double[] size) {
        double[][] parts = parts(x, y, size);
        addSeries(name, color);
        int series = getSeriesCount() - 1;
        bubbles.set(series, parts);
        shown.set(series, copy(parts));
        shownMaxSize = targetMaxSize();
        dataset.changed();
    }

    /**
     * Animates a series to new bubbles: those it has move and resize, new ones grow and the rest are removed.
     */
    public void setBubbles(int series, double[] x, double[] y, double[] size) {
        bubbles.set(series, parts(x, y, size));
        valuesChanged();
    }

    /**
     * Text of each bubble of a series, drawn inside it if it fits and shown as the title of its hover popup.
     */
    public void setLabels(int series, String... labels) {
        this.labels.set(series, labels.clone());
        getChart().fireChartChanged();
    }

    private static double[][] parts(double[] x, double[] y, double[] size) {
        if (x.length != y.length || x.length != size.length) {
            throw new IllegalArgumentException("Expected as many x as y and size values");
        }
        return new double[][]{x.clone(), y.clone(), size.clone()};
    }

    private static double[][] copy(double[][] parts) {
        return new double[][]{parts[X].clone(), parts[Y].clone(), parts[SIZE].clone()};
    }

    @Override
    protected void seriesAdded(int series) {
        bubbles.add(new double[PARTS][0]);
        shown.add(new double[PARTS][0]);
        labels.add(null);
        opacity.add(1.0);
    }

    @Override
    protected void valuesChanged() {
        morph();
    }

    /**
     * The bubbles of a hidden series fade out and those of a shown one fade in.
     */
    @Override
    protected void visibilityChanged(int series) {
        morph();
    }

    @Override
    public void playIntro() {
        animator.stop();
        for (int s = 0; s < getSeriesCount(); s++) {
            opacity.set(s, isSeriesVisible(s) ? 1.0 : 0);
        }
        shownMaxSize = targetMaxSize();
        growing = true;
        if (intro == Intro.GROW) {
            // an overshooting easing can dip below 0, which isn't a size
            animator.start(p -> grow(rank -> Math.max(0, p)), () -> growing = false);
        } else {
            // each bubble starts a little later and eases on its own
            int count = bubbleCount();
            double step = count > 1 ? CASCADE_SPREAD / (count - 1) : 0;
            double span = 1 - step * (count - 1);
            animator.startLinear(t -> grow(rank -> Math.max(0, getEasing().apply(Math.clamp((t - rank * step) / span, 0, 1)))),
                    () -> growing = false);
        }
    }

    public Intro getIntro() {
        return intro;
    }

    public void setIntro(Intro intro) {
        this.intro = intro;
    }

    /**
     * Fixed range of the x axis, so it doesn't rescale while values animate.
     */
    public void setXRange(double lower, double upper) {
        xAxis.setRange(lower, upper);
        applyInsets();
    }

    /**
     * Fixed range of the y axis, so it doesn't rescale while values animate.
     */
    public void setYRange(double lower, double upper) {
        yAxis.setRange(lower, upper);
    }

    public ValueFormatter getXFormatter() {
        return xFormatter;
    }

    /**
     * Text of the values on the x axis and of a bubble's x in the hover popup; the value formatter is for the y.
     */
    public void setXFormatter(ValueFormatter formatter) {
        xFormatter = formatter;
        xAxis.setNumberFormatOverride(formatter != null ? formatter.toNumberFormat() : null);
        applyInsets();
    }

    public ValueFormatter getSizeFormatter() {
        return sizeFormatter;
    }

    /**
     * Text of a bubble's size in the hover popup.
     */
    public void setSizeFormatter(ValueFormatter formatter) {
        sizeFormatter = formatter;
    }

    /**
     * What a bubble's x, y and size are, as named in the hover popup and, if they are shown, on the axes.
     */
    public void setNames(String x, String y, String size) {
        xName = x;
        yName = y;
        sizeName = size;
        applyAxisNames();
    }

    /**
     * Whether the x and y axes have their names beside them.
     */
    public boolean isAxisNamesVisible() {
        return axisNamesVisible;
    }

    public void setAxisNamesVisible(boolean axisNamesVisible) {
        this.axisNamesVisible = axisNamesVisible;
        applyAxisNames();
    }

    private void applyAxisNames() {
        xAxis.setLabel(axisNamesVisible ? xName : null);
        yAxis.setLabel(axisNamesVisible ? yName : null);
    }

    /**
     * Size that is drawn at the largest radius; 0, the default, uses the largest size of the chart.
     */
    public double getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(double maxSize) {
        this.maxSize = maxSize;
        shownMaxSize = targetMaxSize();
        getChart().fireChartChanged();
    }

    /**
     * Radius of the largest bubble, before UI scaling; the others are sized by area.
     */
    public float getMaxRadius() {
        return maxRadius;
    }

    public void setMaxRadius(float maxRadius) {
        this.maxRadius = maxRadius;
        getChart().fireChartChanged();
    }

    /**
     * Opacity, from 0 to 1, of the bubbles' fill; below 1 shows the bubbles they overlap.
     */
    public float getBubbleOpacity() {
        return bubbleOpacity;
    }

    public void setBubbleOpacity(float bubbleOpacity) {
        this.bubbleOpacity = Math.clamp(bubbleOpacity, 0, 1);
        getChart().fireChartChanged();
    }

    public boolean isOutlineVisible() {
        return outlineVisible;
    }

    public void setOutlineVisible(boolean outlineVisible) {
        this.outlineVisible = outlineVisible;
        getChart().fireChartChanged();
    }

    /**
     * Whether the bubbles that have a label show it inside them, if it fits.
     */
    public boolean isLabelsVisible() {
        return labelsVisible;
    }

    public void setLabelsVisible(boolean labelsVisible) {
        this.labelsVisible = labelsVisible;
        getChart().fireChartChanged();
    }

    /**
     * Whether hovering a bubble dims the other bubbles.
     */
    public boolean isHoverHighlight() {
        return hoverHighlight;
    }

    public void setHoverHighlight(boolean hoverHighlight) {
        this.hoverHighlight = hoverHighlight;
        getChart().fireChartChanged();
    }

    private double targetMaxSize() {
        if (maxSize > 0) {
            return maxSize;
        }
        double largest = 0;
        for (double[][] series : bubbles) {
            for (double size : series[SIZE]) {
                largest = Math.max(largest, size);
            }
        }
        return largest;
    }

    private int bubbleCount() {
        int count = 0;
        for (double[][] series : bubbles) {
            count += series[X].length;
        }
        return count;
    }

    /**
     * Animates every bubble from what it shows now to its target values, and every series to shown or hidden.
     */
    private void morph() {
        growing = false;
        List<double[][]> from = new ArrayList<>();
        for (int s = 0; s < bubbles.size(); s++) {
            from.add(resized(shown.get(s), bubbles.get(s)));
        }
        List<Double> fadeFrom = new ArrayList<>(opacity);
        double scaleFrom = shownMaxSize;
        double scaleTo = targetMaxSize();
        animator.start(p -> {
            // overshooting easings would take the opacity out of range
            double fade = Math.clamp(p, 0, 1);
            for (int s = 0; s < bubbles.size(); s++) {
                double[][] to = bubbles.get(s);
                double[][] frame = new double[PARTS][to[X].length];
                for (int part = 0; part < PARTS; part++) {
                    for (int i = 0; i < frame[part].length; i++) {
                        frame[part][i] = lerp(from.get(s)[part][i], to[part][i], p);
                    }
                }
                shown.set(s, frame);
                opacity.set(s, lerp(fadeFrom.get(s), isSeriesVisible(s) ? 1 : 0, fade));
            }
            shownMaxSize = lerp(scaleFrom, scaleTo, fade);
            dataset.changed();
        });
    }

    /**
     * What a series shows now, as the start for as many bubbles as its target has: new ones start where they
     * belong with no size.
     */
    private static double[][] resized(double[][] shown, double[][] target) {
        double[][] from = copy(target);
        for (int i = 0; i < from[X].length; i++) {
            if (i < shown[X].length) {
                for (int part = 0; part < PARTS; part++) {
                    from[part][i] = shown[part][i];
                }
            } else {
                from[SIZE][i] = 0;
            }
        }
        return from;
    }

    /**
     * Shows every bubble in its place, grown as far as the function gives for its rank from the left, from 0 to 1.
     */
    private void grow(IntToDoubleFunction grown) {
        // every bubble of the chart, from left to right
        List<int[]> order = new ArrayList<>();
        for (int s = 0; s < bubbles.size(); s++) {
            for (int i = 0; i < bubbles.get(s)[X].length; i++) {
                order.add(new int[]{s, i});
            }
            shown.set(s, copy(bubbles.get(s)));
        }
        order.sort(Comparator.comparingDouble(bubble -> bubbles.get(bubble[0])[X][bubble[1]]));
        for (int rank = 0; rank < order.size(); rank++) {
            int[] bubble = order.get(rank);
            shown.get(bubble[0])[SIZE][bubble[1]] *= grown.applyAsDouble(rank);
        }
        dataset.changed();
    }

    /**
     * Every bubble as shown now, the largest first, so that the small ones are painted on top.
     */
    private List<Bubble> layout(Rectangle2D dataArea) {
        List<Bubble> layout = new ArrayList<>();
        for (int s = 0; s < shown.size(); s++) {
            double[][] parts = shown.get(s);
            for (int i = 0; i < parts[X].length; i++) {
                layout.add(new Bubble(s, i, xAxis.valueToJava2D(parts[X][i], dataArea, plot.getDomainAxisEdge()),
                        yAxis.valueToJava2D(parts[Y][i], dataArea, plot.getRangeAxisEdge()), radius(parts[SIZE][i])));
            }
        }
        layout.sort(Comparator.comparingDouble(Bubble::radius).reversed());
        return layout;
    }

    /**
     * Radius of a bubble of a size: its area is the size's share of the largest bubble's area.
     */
    private double radius(double size) {
        if (size <= 0 || shownMaxSize <= 0) {
            return 0;
        }
        return Math.max(UIScale.scale(MIN_RADIUS), UIScale.scale(maxRadius) * Math.sqrt(size / shownMaxSize));
    }

    /**
     * A bubble's index among all the bubbles of the chart, which is what hovering goes by.
     */
    private int indexOf(int series, int index) {
        for (int s = 0; s < series; s++) {
            index += bubbles.get(s)[X].length;
        }
        return index;
    }

    /**
     * The series and the index in it of a bubble, from its index among all the bubbles.
     */
    private int[] bubbleAt(int index) {
        int series = 0;
        while (index >= bubbles.get(series)[X].length) {
            index -= bubbles.get(series)[X].length;
            series++;
        }
        return new int[]{series, index};
    }

    private String label(int series, int index) {
        String[] names = labels.get(series);
        return names != null && index < names.length ? names[index] : null;
    }

    @Override
    protected NumberAxis getValueAxis() {
        return yAxis;
    }

    @Override
    protected void setLegendItems(LegendItemCollection items) {
        plot.setFixedLegendItems(items);
    }

    /**
     * The topmost bubble of a visible series at the point.
     */
    @Override
    protected int hoverAt(Point2D point, Rectangle2D dataArea) {
        for (Bubble bubble : layout(dataArea).reversed()) {
            boolean inside = point.distance(bubble.x, bubble.y) <= Math.max(bubble.radius, UIScale.scale(HIT_RADIUS));
            // not a bubble that is still to be removed, which the targets don't have
            if (inside && bubble.radius > 0 && isSeriesVisible(bubble.series) && bubble.index < bubbles.get(bubble.series)[X].length) {
                return indexOf(bubble.series, bubble.index);
            }
        }
        return -1;
    }

    /**
     * Not while the intro is still growing the bubbles.
     */
    @Override
    protected boolean isHoverShown(int index, Rectangle2D dataArea) {
        return !growing && index < bubbleCount();
    }

    /**
     * The bubble's label, or its series for a bubble without one.
     */
    @Override
    protected String hoverTitle(int index) {
        // the popup can still be fading out for a bubble that was removed since
        if (index >= bubbleCount()) {
            return null;
        }
        int[] bubble = bubbleAt(index);
        String label = label(bubble[0], bubble[1]);
        return label != null ? label : getSeriesName(bubble[0]);
    }

    /**
     * The bubble's x, y and size in its color.
     */
    @Override
    protected List<HoverRow> hoverRows(int index) {
        if (index >= bubbleCount()) {
            return List.of();
        }
        int[] bubble = bubbleAt(index);
        double[][] parts = bubbles.get(bubble[0]);
        Color color = getSeriesColor(bubble[0]);
        return List.of(
                new HoverRow(color, xName, format(xFormatter, parts[X][bubble[1]])),
                new HoverRow(color, yName, formatValue(parts[Y][bubble[1]])),
                new HoverRow(color, sizeName, format(sizeFormatter, parts[SIZE][bubble[1]])));
    }

    private String format(ValueFormatter formatter, double value) {
        return formatter != null ? formatter.format(value) : numberFormat.format(value);
    }

    @Override
    protected void applyStyle() {
        super.applyStyle();
        plot.setDomainGridlinesVisible(getStyle().gridVisible());
        plot.setRangeGridlinesVisible(getStyle().gridVisible());
        plot.setDomainGridlinePaint(getStyle().gridColor());
        plot.setRangeGridlinePaint(getStyle().gridColor());
        plot.setDomainGridlineStroke(new BasicStroke(1));
        plot.setRangeGridlineStroke(new BasicStroke(1));
        plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
        getStyle().applyAxis(xAxis);
        getStyle().applyAxis(yAxis);
        applyInsets();
    }

    /**
     * Leaves room to the right of the plot for the half of the x axis' last value that sticks out.
     */
    private void applyInsets() {
        FontMetrics metrics = getChartPanel().getFontMetrics(getStyle().axisFont());
        double right = UIScale.scale(4) + metrics.stringWidth(format(xFormatter, xAxis.getUpperBound())) / 2.0;
        plot.setInsets(new RectangleInsets(4, 8, 4, right));
    }

    /**
     * The bubbles shown now as one series, which is all a plot needs to know to have its renderer paint them.
     */
    private class BubbleDataset extends AbstractXYDataset {

        void changed() {
            fireDatasetChanged();
        }

        @Override
        public int getSeriesCount() {
            return 1;
        }

        @Override
        public Comparable<?> getSeriesKey(int series) {
            return "Bubbles";
        }

        @Override
        public int getItemCount(int series) {
            int count = 0;
            for (double[][] parts : shown) {
                count += parts[X].length;
            }
            return count;
        }

        @Override
        public Number getX(int series, int item) {
            return item;
        }

        @Override
        public Number getY(int series, int item) {
            return item;
        }
    }

    /**
     * Paints every bubble as a translucent circle with an outline and its label, dimmed if another is hovered.
     */
    private class BubbleRenderer extends AbstractXYItemRenderer {

        @Override
        public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
                             ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset data, int series, int item,
                             CrosshairState crosshairState, int pass) {
            // all at once, because the bubbles are painted by size and not in the order of the dataset
            if (item != 0) {
                return;
            }
            ChartStyle style = getStyle();
            Composite composite = g2.getComposite();
            g2.setFont(style.font());
            FontMetrics metrics = g2.getFontMetrics();
            for (Bubble bubble : layout(dataArea)) {
                float alpha = (float) Math.min(1, opacity.get(bubble.series));
                if (alpha <= 0 || bubble.radius <= 0) {
                    continue;
                }
                // both ease in and out, so moving over the bubbles doesn't make them flash
                int index = indexOf(bubble.series, bubble.index);
                float focus = hoverHighlight ? getHoverFocus(index) : 0;
                if (hoverHighlight) {
                    alpha *= getHoverOpacity(index);
                }
                g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
                Color color = getSeriesColor(bubble.series);
                Ellipse2D circle = new Ellipse2D.Double(bubble.x - bubble.radius, bubble.y - bubble.radius, bubble.radius * 2, bubble.radius * 2);
                g2.setColor(ChartStyle.alpha(color, bubbleOpacity));
                g2.fill(circle);
                if (outlineVisible) {
                    g2.setColor(color);
                    g2.setStroke(new BasicStroke(UIScale.scale(1.5f + focus)));
                    g2.draw(circle);
                }
                String label = labelsVisible ? label(bubble.series, bubble.index) : null;
                // only if it fits inside the bubble
                if (label != null && metrics.stringWidth(label) <= bubble.radius * 2 - UIScale.scale(8)) {
                    g2.setColor(style.foreground());
                    g2.drawString(label, (float) (bubble.x - metrics.stringWidth(label) / 2.0),
                            (float) (bubble.y + (metrics.getAscent() - metrics.getDescent()) / 2.0));
                }
            }
            g2.setComposite(composite);
        }
    }
}
