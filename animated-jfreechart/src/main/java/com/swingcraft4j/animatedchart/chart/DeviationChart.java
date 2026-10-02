package com.swingcraft4j.animatedchart.chart;

import com.swingcraft4j.animatedchart.ChartStyle;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Line chart whose series each have a band around their line, from a low to a high value per category,
 * e.g. a range, an error margin or the uncertainty of a forecast.
 */
public class DeviationChart extends LineChart {

    // how far below and above its line the band of each series reaches, by side then category:
    // the target, where the running animation started from, and what is shown now
    private final List<double[][]> bands = new ArrayList<>();
    private final List<double[][]> bandsFrom = new ArrayList<>();
    private final List<double[][]> bandsShown = new ArrayList<>();

    public DeviationChart(String... categories) {
        super(categories);
        setAreaOpacity(0.2f);
    }

    /**
     * Adds a series with a band from {@code low} to {@code high} around its values, one of each per category.
     */
    public void addSeries(String name, Color color, double[] values, double[] low, double[] high) {
        addSeries(name, color, values);
        int series = getSeriesCount() - 1;
        bands.set(series, band(values, low, high));
        bandsFrom.set(series, band(values, low, high));
        bandsShown.set(series, band(values, low, high));
        getChart().fireChartChanged();
    }

    /**
     * Animates a series to new values and a new band around them; setting only the values moves the band along.
     */
    public void setValues(int series, double[] values, double[] low, double[] high) {
        bands.set(series, band(values, low, high));
        setValues(series, values);
    }

    /**
     * Low end of a series' band, one value per category.
     */
    public double[] getLow(int series) {
        return edge(getValues(series), bands.get(series)[0], -1);
    }

    /**
     * High end of a series' band, one value per category.
     */
    public double[] getHigh(int series) {
        return edge(getValues(series), bands.get(series)[1], 1);
    }

    /**
     * The distances of a band's ends from its values, which is how it is kept, so that it follows its line.
     */
    private double[][] band(double[] values, double[] low, double[] high) {
        int count = getCategoryCount();
        if (values.length != count || low.length != count || high.length != count) {
            throw new IllegalArgumentException("Expected " + count + " values");
        }
        double[][] band = new double[2][count];
        for (int i = 0; i < count; i++) {
            band[0][i] = values[i] - low[i];
            band[1][i] = high[i] - values[i];
        }
        return band;
    }

    private static double[] edge(double[] values, double[] distance, int side) {
        double[] edge = new double[values.length];
        for (int i = 0; i < edge.length; i++) {
            edge[i] = values[i] + side * distance[i];
        }
        return edge;
    }

    private static double[][] copy(double[][] band) {
        return new double[][]{band[0].clone(), band[1].clone()};
    }

    /**
     * A series added without a band has one of no height.
     */
    @Override
    protected void seriesAdded(int series) {
        super.seriesAdded(series);
        for (List<double[][]> list : List.of(bands, bandsFrom, bandsShown)) {
            list.add(new double[2][getCategoryCount()]);
        }
    }

    /**
     * The bands animate from what they show now, along with the values.
     */
    @Override
    protected void valuesChanged() {
        for (int s = 0; s < bands.size(); s++) {
            bandsFrom.set(s, copy(bandsShown.get(s)));
        }
        super.valuesChanged();
    }

    /**
     * The intro shows the bands as they are, around lines that draw in or rise.
     */
    @Override
    public void playIntro() {
        for (int s = 0; s < bands.size(); s++) {
            bandsFrom.set(s, copy(bands.get(s)));
            bandsShown.set(s, copy(bands.get(s)));
        }
        super.playIntro();
    }

    @Override
    protected void onFrame(double progress) {
        for (int s = 0; s < bands.size(); s++) {
            double[][] shown = bandsShown.get(s);
            for (int side = 0; side < 2; side++) {
                for (int i = 0; i < shown[side].length; i++) {
                    shown[side][i] = lerp(bandsFrom.get(s)[side][i], bands.get(s)[side][i], progress);
                }
            }
        }
    }

    /**
     * The band of a series: the area between the lines through its low and its high values.
     */
    @Override
    protected void paintArea(Graphics2D g2, int series, Path2D line, Rectangle2D dataArea) {
        double[] values = shownValues(series);
        double[][] band = bandsShown.get(series);
        if (values.length < 2) {
            return;
        }
        // along the high values, then back along the low ones
        Path2D area = LinePaths.create(points(edge(values, band[1], 1), dataArea), getLineStyle());
        area.append(LinePaths.create(points(edge(values, band[0], -1), dataArea).reversed(), getLineStyle()), true);
        area.closePath();
        g2.setColor(ChartStyle.alpha(getSeriesColor(series), getAreaOpacity()));
        g2.fill(area);
    }

    /**
     * Every visible series' value, followed by the ends of its band if it has one.
     */
    @Override
    protected List<HoverRow> hoverRows(int category) {
        List<HoverRow> rows = new ArrayList<>();
        for (int s = 0; s < getSeriesCount(); s++) {
            if (!isSeriesVisible(s)) {
                continue;
            }
            double value = getValues(s)[category];
            double[][] band = bands.get(s);
            String text = formatValue(value);
            if (band[0][category] != 0 || band[1][category] != 0) {
                text += "  (" + formatValue(value - band[0][category]) + " – " + formatValue(value + band[1][category]) + ")";
            }
            rows.add(new HoverRow(getSeriesColor(s), getSeriesName(s), text));
        }
        return rows;
    }
}
