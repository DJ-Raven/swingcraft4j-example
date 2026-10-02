package com.swingcraft4j.animatedchart.chart;

import com.formdev.flatlaf.util.UIScale;
import com.swingcraft4j.animatedchart.AnimatedChart;
import com.swingcraft4j.animatedchart.ChartStyle;
import com.swingcraft4j.animatedchart.ValueFormatter;
import com.swingcraft4j.animatedchart.chart.LineChart.LineStyle;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JFreeChart candlestick chart over named categories, with volume bars, moving averages, a hover crosshair,
 * an animated intro and animated value changes.
 */
public class CandlestickChart extends AnimatedChart {

    /**
     * How the chart first appears.
     */
    public enum Intro {
        /**
         * Candles are drawn in from left to right.
         */
        DRAW("Draw"),
        /**
         * Every candle grows from the middle of its body, and its volume bar from the bottom.
         */
        GROW("Grow");

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
     * How a category's open, high, low and close are drawn.
     */
    public enum CandleStyle {
        /**
         * A filled body from open to close with a wick from low to high.
         */
        SOLID("Solid"),
        /**
         * Like solid, but a candle that closed up is only an outline.
         */
        HOLLOW("Hollow"),
        /**
         * A line from low to high with a tick to the left at the open and to the right at the close.
         */
        BARS("OHLC bars"),
        /**
         * Solid candles of averaged values, which smooth the trend; the popup still shows the real values.
         */
        HEIKIN_ASHI("Heikin-Ashi");

        private final String name;

        CandleStyle(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // the values of a candle; each is a series of the chart, in this order, and the moving averages come after them
    private static final String[] PARTS = {"Open", "High", "Low", "Close", "Volume"};
    private static final int OPEN = 0;
    private static final int HIGH = 1;
    private static final int LOW = 2;
    private static final int CLOSE = 3;
    private static final int VOLUME = 4;
    // alpha of the volume bars
    private static final int VOLUME_ALPHA = 70;

    private final CandleDataset dataset = new CandleDataset();
    private final NumberAxis valueAxis = new NumberAxis();
    private final RevealPlot plot;
    private final DecimalFormat volumeFormat = new DecimalFormat("#,##0");
    private final DecimalFormat percentFormat = new DecimalFormat("0.0");
    // period of each moving average, and its opacity as shown now, fading when it is hidden or shown
    private final List<Integer> periods = new ArrayList<>();
    private final List<Double> averageOpacity = new ArrayList<>();
    private Intro intro = Intro.DRAW;
    private CandleStyle candleStyle = CandleStyle.SOLID;
    // null for the style's positive and negative colors
    private Color upColor;
    private Color downColor;
    private float candleWidth = 16;
    private float averageWidth = 1.8f;
    private double volumeHeight = 0.2;
    private boolean volumeVisible = true;
    private boolean priceLineVisible = true;
    private boolean crosshair = true;
    private boolean hoverHighlight;
    private boolean hasCandles;
    // true while setCandles changes several series, so they animate together and not one by one
    private boolean updating;

    public CandlestickChart(String... categories) {
        super(categories);
        valueAxis.setRange(0, 100);
        plot = new RevealPlot(dataset, new SpacedAxis(categories), valueAxis, new CandleRenderer()) {
            @Override
            public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor, PlotState parentState, PlotRenderingInfo info) {
                super.draw(g2, area, anchor, parentState, info);
                // after the plot, because the tag sits beside the data area, where the renderer can't paint
                if (info != null) {
                    paintPriceTag(g2, info.getDataArea());
                }
            }
        };
        setChart(new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, true));
        for (String part : PARTS) {
            addSeries(part, null, new double[categories.length]);
        }
    }

    /**
     * Sets the open, high, low and close of every category, without volumes.
     */
    public void setCandles(double[] open, double[] high, double[] low, double[] close) {
        setCandles(open, high, low, close, new double[open.length]);
    }

    /**
     * Sets the open, high, low, close and volume of every category; after the first time, animates to them.
     */
    public void setCandles(double[] open, double[] high, double[] low, double[] close, double[] volume) {
        double[][] parts = {open, high, low, close, volume};
        updating = true;
        try {
            for (int i = 0; i < parts.length; i++) {
                setValues(i, parts[i]);
            }
            for (int a = 0; a < periods.size(); a++) {
                setValues(PARTS.length + a, average(close, periods.get(a)));
            }
        } finally {
            updating = false;
        }
        if (hasCandles) {
            valuesChanged();
        } else {
            hasCandles = true;
            dataset.show(parts());
        }
    }

    /**
     * Adds a line of the average close over the last {@code period} categories; a {@code null} color uses the style's palette.
     */
    public void addMovingAverage(int period, Color color) {
        periods.add(period);
        averageOpacity.add(1.0);
        addSeries("MA " + period, color, average(getValues(CLOSE), period));
    }

    /**
     * Average of each value and the ones before it, up to {@code period} of them; fewer at the start.
     */
    private static double[] average(double[] values, int period) {
        double[] average = new double[values.length];
        double sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += values[i];
            if (i >= period) {
                sum -= values[i - period];
            }
            average[i] = sum / Math.min(i + 1, period);
        }
        return average;
    }

    @Override
    protected void seriesAdded(int series) {
    }

    @Override
    protected void valuesChanged() {
        if (hasCandles && !updating) {
            plot.setReveal(1);
            morph(dataset.shown());
        }
    }

    /**
     * A moving average fades out or in; the values of a candle belong together and can't be hidden on their own.
     */
    @Override
    protected void visibilityChanged(int series) {
        if (series >= PARTS.length) {
            valuesChanged();
        }
    }

    @Override
    public void playIntro() {
        if (!hasCandles) {
            return;
        }
        double[][] to = parts();
        if (intro == Intro.DRAW) {
            animator.stop();
            showAverages(1);
            dataset.show(to);
            plot.setReveal(0);
            animator.start(plot::setReveal);
        } else {
            plot.setReveal(1);
            double[][] from = new double[PARTS.length][getCategoryCount()];
            for (int i = 0; i < getCategoryCount(); i++) {
                double middle = (to[OPEN][i] + to[CLOSE][i]) / 2;
                for (int part = OPEN; part <= CLOSE; part++) {
                    from[part][i] = middle;
                }
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

    /**
     * Fixed range of the value axis, so it doesn't rescale while values animate.
     */
    public void setValueRange(double lower, double upper) {
        valueAxis.setRange(lower, upper);
        applyInsets();
    }

    public CandleStyle getCandleStyle() {
        return candleStyle;
    }

    public void setCandleStyle(CandleStyle candleStyle) {
        this.candleStyle = candleStyle;
        getChart().fireChartChanged();
    }

    /**
     * Whether a dashed line marks the last close, with its value tagged to the right of the plot.
     */
    public boolean isPriceLineVisible() {
        return priceLineVisible;
    }

    public void setPriceLineVisible(boolean priceLineVisible) {
        this.priceLineVisible = priceLineVisible;
        applyInsets();
    }

    @Override
    public void setValueFormatter(ValueFormatter formatter) {
        super.setValueFormatter(formatter);
        applyInsets();
    }

    /**
     * Leaves room to the right of the plot for the last close's tag, as wide as the widest value of the axis.
     */
    private void applyInsets() {
        double right = 8;
        if (priceLineVisible) {
            FontMetrics metrics = getChartPanel().getFontMetrics(getStyle().font());
            right = UIScale.scale(4) + Math.max(ChartTag.width(metrics, formatValue(valueAxis.getLowerBound())),
                    ChartTag.width(metrics, formatValue(valueAxis.getUpperBound())));
        }
        plot.setInsets(new RectangleInsets(4, 8, 4, right));
    }

    /**
     * The last close as shown now, tagged in its candle's color beside the end of the price line.
     */
    private void paintPriceTag(Graphics2D g, Rectangle2D dataArea) {
        // not until the draw-in intro has reached the last candle
        if (!priceLineVisible || !hasCandles || plot.getReveal() < 1) {
            return;
        }
        int last = getCategoryCount() - 1;
        double close = dataset.getCloseValue(0, last);
        double y = valueAxis.valueToJava2D(close, dataArea, plot.getRangeAxisEdge());
        FontMetrics metrics = g.getFontMetrics(getStyle().font());
        Color color = color(dataset.getOpenValue(0, last), close);
        ChartTag.paint(g, metrics, formatValue(close), dataArea.getMaxX() + UIScale.scale(4), y - ChartTag.height(metrics) / 2.0,
                color, ChartStyle.contrast(color));
    }

    /**
     * Colors of a candle that closed at or above its open, and of one that closed below it;
     * {@code null} uses the style's positive or negative color.
     */
    public void setColors(Color up, Color down) {
        upColor = up;
        downColor = down;
        getChart().fireChartChanged();
    }

    public Color getUpColor() {
        return upColor != null ? upColor : getStyle().positive();
    }

    public Color getDownColor() {
        return downColor != null ? downColor : getStyle().negative();
    }

    /**
     * Width of a candle at most, before UI scaling; candles get narrower when there isn't room for it.
     */
    public float getCandleWidth() {
        return candleWidth;
    }

    public void setCandleWidth(float candleWidth) {
        this.candleWidth = candleWidth;
        getChart().fireChartChanged();
    }

    /**
     * Width of the moving average lines, before UI scaling.
     */
    public float getAverageWidth() {
        return averageWidth;
    }

    public void setAverageWidth(float averageWidth) {
        this.averageWidth = averageWidth;
        getChart().fireChartChanged();
    }

    /**
     * Height of the tallest volume bar as a share of the plot's height, from 0 to 1.
     */
    public double getVolumeHeight() {
        return volumeHeight;
    }

    public void setVolumeHeight(double volumeHeight) {
        this.volumeHeight = Math.clamp(volumeHeight, 0, 1);
        getChart().fireChartChanged();
    }

    /**
     * Whether the volumes are drawn as bars along the bottom of the plot.
     */
    public boolean isVolumeVisible() {
        return volumeVisible;
    }

    public void setVolumeVisible(boolean volumeVisible) {
        this.volumeVisible = volumeVisible;
        getChart().fireChartChanged();
    }

    /**
     * Whether hovering shows a crosshair with the price and the category tagged on the axes.
     */
    public boolean isCrosshair() {
        return crosshair;
    }

    public void setCrosshair(boolean crosshair) {
        this.crosshair = crosshair;
        getChart().fireChartChanged();
    }

    /**
     * Whether hovering a candle dims the other candles.
     */
    public boolean isHoverHighlight() {
        return hoverHighlight;
    }

    public void setHoverHighlight(boolean hoverHighlight) {
        this.hoverHighlight = hoverHighlight;
        getChart().fireChartChanged();
    }

    /**
     * Target open, high, low, close and volume, by part then category.
     */
    private double[][] parts() {
        return Arrays.copyOf(seriesValues(), PARTS.length);
    }

    /**
     * Animates every candle from {@code from} to its target values, and every moving average to shown or hidden.
     */
    private void morph(double[][] from) {
        double[][] to = parts();
        List<Double> fadeFrom = new ArrayList<>(averageOpacity);
        animator.start(p -> {
            double[][] frame = new double[to.length][getCategoryCount()];
            for (int s = 0; s < to.length; s++) {
                for (int i = 0; i < frame[s].length; i++) {
                    frame[s][i] = lerp(from[s][i], to[s][i], p);
                }
            }
            for (int a = 0; a < fadeFrom.size(); a++) {
                // overshooting easings would take the opacity out of range
                averageOpacity.set(a, lerp(fadeFrom.get(a), isSeriesVisible(PARTS.length + a) ? 1 : 0, Math.clamp(p, 0, 1)));
            }
            dataset.show(frame);
        });
    }

    /**
     * Sets every moving average straight to shown or hidden, scaled by {@code progress}.
     */
    private void showAverages(double progress) {
        for (int a = 0; a < averageOpacity.size(); a++) {
            averageOpacity.set(a, isSeriesVisible(PARTS.length + a) ? progress : 0);
        }
    }

    private Color color(double open, double close) {
        return close >= open ? getUpColor() : getDownColor();
    }

    @Override
    protected NumberAxis getValueAxis() {
        return valueAxis;
    }

    /**
     * The legend lists only the moving averages; without any, it is hidden.
     */
    @Override
    protected void setLegendItems(LegendItemCollection items) {
        LegendItemCollection averages = new LegendItemCollection();
        for (int i = PARTS.length; i < items.getItemCount(); i++) {
            averages.add(items.get(i));
        }
        plot.setFixedLegendItems(averages);
        getChart().getLegend().setVisible(getStyle().legendVisible() && averages.getItemCount() > 0);
    }

    @Override
    protected int hoverAt(Point2D point, Rectangle2D dataArea) {
        if (!hasCandles) {
            return -1;
        }
        double value = plot.getDomainAxis().java2DToValue(point.getX(), dataArea, plot.getDomainAxisEdge());
        return Math.clamp(Math.round(value), 0, getCategoryCount() - 1);
    }

    /**
     * Not while the draw-in intro hasn't reached the candle yet.
     */
    @Override
    protected boolean isHoverShown(int category, Rectangle2D dataArea) {
        return plot.isRevealed(categoryX(category, dataArea), dataArea);
    }

    /**
     * The candle's open, high, low and close in its color, then its volume and the visible moving averages.
     */
    @Override
    protected List<HoverRow> hoverRows(int category) {
        Color color = color(getValues(OPEN)[category], getValues(CLOSE)[category]);
        List<HoverRow> rows = new ArrayList<>();
        for (int part = OPEN; part <= CLOSE; part++) {
            rows.add(new HoverRow(color, PARTS[part], formatValue(getValues(part)[category])));
        }
        // against the close before, or the open for the first candle
        double close = getValues(CLOSE)[category];
        double before = category > 0 ? getValues(CLOSE)[category - 1] : getValues(OPEN)[category];
        String sign = close >= before ? "+" : "-";
        double percent = before != 0 ? Math.abs(close - before) / Math.abs(before) * 100 : 0;
        rows.add(new HoverRow(color(before, close), "Change",
                sign + formatValue(Math.abs(close - before)) + " (" + sign + percentFormat.format(percent) + "%)"));
        if (volumeVisible && dataset.largestVolume() > 0) {
            rows.add(new HoverRow(getStyle().mutedForeground(), PARTS[VOLUME], volumeFormat.format(getValues(VOLUME)[category])));
        }
        for (int series = PARTS.length; series < getSeriesCount(); series++) {
            if (isSeriesVisible(series)) {
                rows.add(new HoverRow(getSeriesColor(series), getSeriesName(series), formatValue(getValues(series)[category])));
            }
        }
        return rows;
    }

    /**
     * The crosshair: a vertical line through the candle and a horizontal one at the mouse, each tagged on its axis.
     */
    @Override
    protected void paintHoverMarks(Graphics2D g, Rectangle2D dataArea, int category) {
        Point mouse = getHoverPoint();
        if (!crosshair || mouse == null) {
            return;
        }
        double x = categoryX(category, dataArea);
        double y = Math.clamp(mouse.y, dataArea.getMinY(), dataArea.getMaxY());
        float dash = UIScale.scale(4f);
        g.setColor(ChartStyle.mix(getStyle().foreground(), getStyle().background(), 0.45f));
        g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{dash, dash}, 0));
        g.draw(new Line2D.Double(x, dataArea.getMinY(), x, dataArea.getMaxY()));
        g.draw(new Line2D.Double(dataArea.getMinX(), y, dataArea.getMaxX(), y));

        FontMetrics metrics = g.getFontMetrics(getStyle().font());
        String price = formatValue(valueAxis.java2DToValue(y, dataArea, plot.getRangeAxisEdge()));
        // on the value axis, ending where the plot starts
        ChartTag.paint(g, metrics, price, dataArea.getMinX() - UIScale.scale(4) - ChartTag.width(metrics, price), y - ChartTag.height(metrics) / 2.0,
                getStyle().foreground(), getStyle().background());
        // on the category axis, centered under the candle
        String day = getCategory(category);
        ChartTag.paint(g, metrics, day, x - ChartTag.width(metrics, day) / 2.0, dataArea.getMaxY() + UIScale.scale(4),
                getStyle().foreground(), getStyle().background());
    }

    private double categoryX(int category, Rectangle2D dataArea) {
        return plot.getDomainAxis().valueToJava2D(category, dataArea, plot.getDomainAxisEdge());
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
        applyInsets();
    }

    /**
     * Heikin-Ashi candles of the given open, high, low and close; the volume is passed through.
     */
    private static double[][] heikinAshi(double[][] parts) {
        int count = parts[OPEN].length;
        double[][] smoothed = new double[PARTS.length][count];
        smoothed[VOLUME] = parts[VOLUME];
        for (int i = 0; i < count; i++) {
            // the close is the candle's average, and the open the middle of the previous smoothed body
            smoothed[CLOSE][i] = (parts[OPEN][i] + parts[HIGH][i] + parts[LOW][i] + parts[CLOSE][i]) / 4;
            smoothed[OPEN][i] = i == 0 ? (parts[OPEN][i] + parts[CLOSE][i]) / 2 : (smoothed[OPEN][i - 1] + smoothed[CLOSE][i - 1]) / 2;
            smoothed[HIGH][i] = Math.max(parts[HIGH][i], Math.max(smoothed[OPEN][i], smoothed[CLOSE][i]));
            smoothed[LOW][i] = Math.min(parts[LOW][i], Math.min(smoothed[OPEN][i], smoothed[CLOSE][i]));
        }
        return smoothed;
    }

    /**
     * The values shown now, one candle per category at x = its index; replaced as a whole on every animation frame.
     */
    private static class CandleDataset extends AbstractXYDataset implements OHLCDataset {

        private double[][] parts = new double[PARTS.length][0];

        double[][] shown() {
            return parts;
        }

        void show(double[][] parts) {
            this.parts = parts;
            fireDatasetChanged();
        }

        double largestVolume() {
            double largest = 0;
            for (double volume : parts[VOLUME]) {
                largest = Math.max(largest, volume);
            }
            return largest;
        }

        @Override
        public int getSeriesCount() {
            return 1;
        }

        @Override
        public Comparable<?> getSeriesKey(int series) {
            return "Candles";
        }

        @Override
        public int getItemCount(int series) {
            return parts[OPEN].length;
        }

        @Override
        public Number getX(int series, int item) {
            return item;
        }

        @Override
        public Number getY(int series, int item) {
            return parts[CLOSE][item];
        }

        @Override
        public Number getOpen(int series, int item) {
            return parts[OPEN][item];
        }

        @Override
        public double getOpenValue(int series, int item) {
            return parts[OPEN][item];
        }

        @Override
        public Number getHigh(int series, int item) {
            return parts[HIGH][item];
        }

        @Override
        public double getHighValue(int series, int item) {
            return parts[HIGH][item];
        }

        @Override
        public Number getLow(int series, int item) {
            return parts[LOW][item];
        }

        @Override
        public double getLowValue(int series, int item) {
            return parts[LOW][item];
        }

        @Override
        public Number getClose(int series, int item) {
            return parts[CLOSE][item];
        }

        @Override
        public double getCloseValue(int series, int item) {
            return parts[CLOSE][item];
        }

        @Override
        public Number getVolume(int series, int item) {
            return parts[VOLUME][item];
        }

        @Override
        public double getVolumeValue(int series, int item) {
            return parts[VOLUME][item];
        }
    }

    /**
     * Paints each candle as a thin wick under a rounded body over its volume bar, then the moving averages on top.
     */
    private class CandleRenderer extends AbstractXYItemRenderer {

        // tallest volume shown now and the values to draw in the current style, found once per repaint
        private double largestVolume;
        private double[][] drawn;

        @Override
        public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
                             ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset data, int series, int item,
                             CrosshairState crosshairState, int pass) {
            if (item == 0) {
                largestVolume = dataset.largestVolume();
                drawn = candleStyle == CandleStyle.HEIKIN_ASHI ? heikinAshi(dataset.shown()) : dataset.shown();
            }
            double x = domainAxis.valueToJava2D(item, dataArea, plot.getDomainAxisEdge());
            double step = Math.abs(domainAxis.valueToJava2D(1, dataArea, plot.getDomainAxisEdge())
                    - domainAxis.valueToJava2D(0, dataArea, plot.getDomainAxisEdge()));
            // as wide as the spacing allows
            double width = Math.min(step * 0.62, UIScale.scale(candleWidth));
            double open = drawn[OPEN][item];
            double close = drawn[CLOSE][item];
            Color color = color(open, close);

            if (volumeVisible && largestVolume > 0) {
                double height = dataArea.getHeight() * volumeHeight * drawn[VOLUME][item] / largestVolume;
                g2.setColor(ChartStyle.alpha(color, VOLUME_ALPHA));
                g2.fill(new Rectangle2D.Double(x - width / 2, dataArea.getMaxY() - height, width, height));
            }

            double high = rangeAxis.valueToJava2D(drawn[HIGH][item], dataArea, plot.getRangeAxisEdge());
            double low = rangeAxis.valueToJava2D(drawn[LOW][item], dataArea, plot.getRangeAxisEdge());
            double openY = rangeAxis.valueToJava2D(open, dataArea, plot.getRangeAxisEdge());
            double closeY = rangeAxis.valueToJava2D(close, dataArea, plot.getRangeAxisEdge());
            double top = Math.min(openY, closeY);
            double bottom = Math.max(openY, closeY);
            if (hoverHighlight) {
                // eases in and out, so the candles don't flash
                color = ChartStyle.alpha(color, getHoverOpacity(item));
            }
            float line = UIScale.scale(1.5f);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(line, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (candleStyle == CandleStyle.BARS) {
                g2.draw(new Line2D.Double(x, high, x, low));
                g2.draw(new Line2D.Double(x - width / 2, openY, x, openY));
                g2.draw(new Line2D.Double(x, closeY, x + width / 2, closeY));
            } else {
                // two pieces, so the wick doesn't show through a dimmed or hollow body
                g2.draw(new Line2D.Double(x, high, x, top));
                g2.draw(new Line2D.Double(x, bottom, x, low));

                // at least a thin line when open equals close
                double height = Math.max(bottom - top, line);
                double arc = Math.min(UIScale.scale(4f), Math.min(width, height));
                if (candleStyle == CandleStyle.HOLLOW && close >= open && height > line * 2) {
                    // the outline drawn inside the body's bounds
                    g2.draw(new RoundRectangle2D.Double(x - width / 2 + line / 2, top + line / 2, width - line, height - line, arc, arc));
                } else {
                    g2.fill(new RoundRectangle2D.Double(x - width / 2, top, width, height, arc, arc));
                }
            }

            if (item == dataset.getItemCount(series) - 1) {
                drawPriceLine(g2, dataArea, rangeAxis, plot);
                drawAverages(g2, dataArea, plot, domainAxis, rangeAxis);
            }
        }

        /**
         * A dashed line across the plot at the last close shown now, in its candle's color.
         */
        private void drawPriceLine(Graphics2D g2, Rectangle2D dataArea, ValueAxis rangeAxis, XYPlot plot) {
            if (!priceLineVisible) {
                return;
            }
            int last = dataset.getItemCount(0) - 1;
            double close = dataset.getCloseValue(0, last);
            double y = rangeAxis.valueToJava2D(close, dataArea, plot.getRangeAxisEdge());
            float dash = UIScale.scale(4f);
            g2.setColor(color(dataset.getOpenValue(0, last), close));
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{dash, dash}, 0));
            g2.draw(new Line2D.Double(dataArea.getMinX(), y, dataArea.getMaxX(), y));
        }

        /**
         * Each moving average of the closes shown now, so the lines follow the candles while they animate.
         */
        private void drawAverages(Graphics2D g2, Rectangle2D dataArea, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis) {
            double[] closes = dataset.shown()[CLOSE];
            if (closes.length < 2) {
                return;
            }
            Composite composite = g2.getComposite();
            g2.setStroke(new BasicStroke(UIScale.scale(averageWidth), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int a = 0; a < periods.size(); a++) {
                float opacity = averageOpacity.get(a).floatValue();
                if (opacity <= 0) {
                    continue;
                }
                double[] average = average(closes, periods.get(a));
                List<Point2D.Double> points = new ArrayList<>();
                for (int i = 0; i < average.length; i++) {
                    points.add(new Point2D.Double(domainAxis.valueToJava2D(i, dataArea, plot.getDomainAxisEdge()),
                            rangeAxis.valueToJava2D(average[i], dataArea, plot.getRangeAxisEdge())));
                }
                g2.setComposite(AlphaComposite.SrcOver.derive(Math.min(1, opacity)));
                g2.setColor(getSeriesColor(PARTS.length + a));
                g2.draw(LinePaths.create(points, LineStyle.MONOTONE));
            }
            g2.setComposite(composite);
        }
    }
}
