package com.swingcraft4j.animatedchart.chart;

import com.swingcraft4j.animatedchart.AnimatedChart;
import com.swingcraft4j.animatedchart.ValueFormatter;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.PieSectionEntity;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * JFreeChart pie chart of named slices: pie, donut, half circle or rose, with an animated intro and animated value changes.
 */
public class PieChart extends AnimatedChart {

    /**
     * How the chart first appears.
     */
    public enum Intro {
        /**
         * The slices are swept in clockwise from the start.
         */
        SWEEP("Sweep"),
        /**
         * The whole pie grows from its center.
         */
        EXPAND("Expand");

        private final String name;

        Intro(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // a rose keeps only a small hole, to leave its slices room to show their values
    private static final double ROSE_HOLE = 0.2;

    private final DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
    private final SlicePlot plot = new SlicePlot(dataset);
    private Intro intro = Intro.SWEEP;
    private boolean donut = true;
    private boolean half;
    private boolean rose;
    private double holeSize = 0.62;
    private float centerFontScale = 1.8f;
    private float sliceGap = 2;
    private float cornerRadius;
    private boolean hoverHighlight = true;

    public PieChart() {
        // each slice is a series with a single value
        super("Value");
        plot.setHover(slice -> hoverHighlight ? getHoverOpacity(slice) : 1, slice -> hoverHighlight ? getHoverFocus(slice) : 0);
        setChart(new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, true));
        applyShape();
    }

    /**
     * Adds a slice; a {@code null} color uses the next color of the style's palette.
     */
    public void addSlice(String name, Color color, double value) {
        addSeries(name, color, value);
    }

    @Override
    protected void seriesAdded(int series) {
        dataset.setValue(getSeriesName(series), getValues(series)[0]);
        updateCenterText();
    }

    @Override
    protected void valuesChanged() {
        plot.setIntro(1, 1);
        morph();
    }

    /**
     * A hidden slice shrinks to nothing and a shown one grows back, while the others make room.
     */
    @Override
    protected void visibilityChanged(int series) {
        valuesChanged();
    }

    @Override
    public void playIntro() {
        show(targets(), targetPresence());
        if (intro == Intro.SWEEP) {
            animator.start(p -> plot.setIntro(p, 1));
        } else {
            animator.start(p -> plot.setIntro(1, p));
        }
    }

    public Intro getIntro() {
        return intro;
    }

    public void setIntro(Intro intro) {
        this.intro = intro;
    }

    /**
     * Whether the chart is a ring with the total in the middle, instead of a full pie.
     */
    public boolean isDonut() {
        return donut;
    }

    public void setDonut(boolean donut) {
        this.donut = donut;
        applyShape();
    }

    /**
     * Whether the chart is the upper half of a circle, standing on its flat side.
     */
    public boolean isHalf() {
        return half;
    }

    public void setHalf(boolean half) {
        this.half = half;
        applyShape();
    }

    /**
     * Whether the chart is a rose: every slice has the same angle and shows its value by its radius.
     */
    public boolean isRose() {
        return rose;
    }

    public void setRose(boolean rose) {
        this.rose = rose;
        applyShape();
    }

    /**
     * The donut's hole as a share of the radius, from 0 to 1.
     */
    public double getHoleSize() {
        return holeSize;
    }

    public void setHoleSize(double holeSize) {
        this.holeSize = Math.clamp(holeSize, 0, 1);
        applyShape();
    }

    /**
     * Size of the total in the middle of a donut, as a multiple of the style's font size.
     */
    public float getCenterFontScale() {
        return centerFontScale;
    }

    public void setCenterFontScale(float centerFontScale) {
        this.centerFontScale = centerFontScale;
        updateCenterText();
    }

    /**
     * Width of the gap between slices, before UI scaling; 0 for none.
     */
    public float getSliceGap() {
        return sliceGap;
    }

    public void setSliceGap(float sliceGap) {
        this.sliceGap = sliceGap;
        applyShape();
    }

    /**
     * Radius of the slices' corners, before UI scaling; 0 is square.
     */
    public float getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
        applyShape();
    }

    /**
     * Whether each slice has its name beside it, joined by a leader line.
     */
    public boolean isLabelsVisible() {
        return plot.isLabelsVisible();
    }

    public void setLabelsVisible(boolean labelsVisible) {
        plot.setLabelsVisible(labelsVisible);
    }

    /**
     * Whether hovering a slice dims the other slices.
     */
    public boolean isHoverHighlight() {
        return hoverHighlight;
    }

    public void setHoverHighlight(boolean hoverHighlight) {
        this.hoverHighlight = hoverHighlight;
        applyPaints();
    }

    private void applyShape() {
        double hole = !donut ? 0 : rose ? Math.min(holeSize, ROSE_HOLE) : holeSize;
        plot.setShape(hole, sliceGap, cornerRadius, half, rose);
        updateCenterText();
    }

    /**
     * Target value of each slice, 0 for the hidden ones.
     */
    private double[] targets() {
        double[] targets = new double[getSeriesCount()];
        for (int s = 0; s < targets.length; s++) {
            targets[s] = isSeriesVisible(s) ? getValues(s)[0] : 0;
        }
        return targets;
    }

    private double[] targetPresence() {
        double[] presence = new double[getSeriesCount()];
        for (int s = 0; s < presence.length; s++) {
            presence[s] = isSeriesVisible(s) ? 1 : 0;
        }
        return presence;
    }

    /**
     * Animates every slice from the value shown now to its target value, and its presence to shown or hidden.
     */
    private void morph() {
        int count = getSeriesCount();
        double[] from = new double[count];
        double[] presenceFrom = new double[count];
        for (int s = 0; s < count; s++) {
            from[s] = dataset.getValue(s).doubleValue();
            presenceFrom[s] = plot.getPresence(s);
        }
        double[] to = targets();
        double[] presenceTo = targetPresence();
        animator.start(p -> {
            double[] values = new double[count];
            double[] presence = new double[count];
            // overshooting easings would take the presence out of range
            double clamped = Math.clamp(p, 0, 1);
            for (int s = 0; s < count; s++) {
                values[s] = lerp(from[s], to[s], p);
                presence[s] = lerp(presenceFrom[s], presenceTo[s], clamped);
            }
            show(values, presence);
        });
    }

    private void show(double[] values, double[] presence) {
        batch(() -> {
            for (int s = 0; s < values.length; s++) {
                // an overshooting easing can dip below 0, which a pie can't draw
                dataset.setValue(getSeriesName(s), Math.max(0, values[s]));
            }
            plot.setPresence(presence);
            updateCenterText();
        });
    }

    /**
     * The total of the slices shown now, in the middle of a donut; a rose's hole is too small for it.
     */
    private void updateCenterText() {
        double total = 0;
        for (int s = 0; s < dataset.getItemCount(); s++) {
            total += dataset.getValue(s).doubleValue();
        }
        plot.setCenter(donut && !rose ? formatValue(Math.round(total)) : null,
                getStyle().boldFont().deriveFont(getStyle().font().getSize2D() * centerFontScale), getStyle().foreground());
    }

    @Override
    public void setValueFormatter(ValueFormatter formatter) {
        super.setValueFormatter(formatter);
        updateCenterText();
    }

    /**
     * The slice colors; the plot dims them and grows the hovered slice as hovering eases in and out.
     */
    private void applyPaints() {
        Color[] colors = new Color[getSeriesCount()];
        for (int s = 0; s < colors.length; s++) {
            colors[s] = getSeriesColor(s);
        }
        plot.setColors(colors);
    }

    @Override
    protected NumberAxis getValueAxis() {
        return null;
    }

    @Override
    protected void setLegendItems(LegendItemCollection items) {
        plot.setLegendItems(items);
    }

    /**
     * Index of the slice at the point, or -1.
     */
    @Override
    protected int hoverAt(Point2D point, Rectangle2D dataArea) {
        if (getChartPanel().getEntityForPoint((int) point.getX(), (int) point.getY()) instanceof PieSectionEntity slice) {
            return slice.getSectionIndex();
        }
        return -1;
    }

    /**
     * Not while the intro is still revealing the pie.
     */
    @Override
    protected boolean isHoverShown(int index, Rectangle2D dataArea) {
        return plot.isIntroDone();
    }

    @Override
    protected String hoverTitle(int index) {
        return null;
    }

    /**
     * One row: the slice, its value and its share of the total.
     */
    @Override
    protected List<HoverRow> hoverRows(int index) {
        double value = isSeriesVisible(index) ? getValues(index)[0] : 0;
        double sum = 0;
        for (double target : targets()) {
            sum += target;
        }
        long percent = sum > 0 ? Math.round(value / sum * 100) : 0;
        return List.of(new HoverRow(getSeriesColor(index), getSeriesName(index), formatValue(value) + "  (" + percent + "%)"));
    }

    @Override
    protected void applyStyle() {
        super.applyStyle();
        plot.setLabelFont(getStyle().font());
        plot.setLabelPaint(getStyle().mutedForeground());
        updateCenterText();
        applyPaints();
    }
}
