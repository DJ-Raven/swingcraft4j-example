package com.swingcraft4j.animatedchart.chart;

import com.swingcraft4j.animatedchart.ChartStyle;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Line chart of two series whose area between them is filled, in one color where the first is above the second
 * and in another where it is below.
 */
public class DifferenceChart extends LineChart {

    // null for the style's positive and negative colors
    private Color positiveColor;
    private Color negativeColor;

    /**
     * Creates a chart that compares the first series added to it with the second.
     */
    public DifferenceChart(String... categories) {
        super(categories);
        setAreaOpacity(0.25f);
    }

    /**
     * Colors of the area where the first series is above the second, and where it is below;
     * {@code null} uses the style's positive or negative color.
     */
    public void setColors(Color positive, Color negative) {
        positiveColor = positive;
        negativeColor = negative;
        getChart().fireChartChanged();
    }

    public Color getPositiveColor() {
        return positiveColor != null ? positiveColor : getStyle().positive();
    }

    public Color getNegativeColor() {
        return negativeColor != null ? negativeColor : getStyle().negative();
    }

    /**
     * The area between the first two series, painted under the first; the other series have no area.
     */
    @Override
    protected void paintArea(Graphics2D g2, int series, Path2D line, Rectangle2D dataArea) {
        if (series != 0 || getSeriesCount() < 2 || getCategoryCount() < 2 || shownOpacity(1) <= 0) {
            return;
        }
        Path2D other = LinePaths.create(points(shownValues(1), dataArea), getLineStyle());
        // below both lines, even when an overshooting easing takes them under the plot
        double base = Math.max(dataArea.getMaxY(), Math.max(line.getBounds2D().getMaxY(), other.getBounds2D().getMaxY())) + 1;
        Area first = new Area(under(line, base, dataArea));
        Area second = new Area(under(other, base, dataArea));
        // what is under the first line but not under the second is where the first is above
        Area above = new Area(first);
        above.subtract(second);
        second.subtract(first);

        // fades with either series, when one is hidden or shown
        Composite composite = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.min(shownOpacity(0), shownOpacity(1))));
        g2.setColor(ChartStyle.alpha(getPositiveColor(), getAreaOpacity()));
        g2.fill(above);
        g2.setColor(ChartStyle.alpha(getNegativeColor(), getAreaOpacity()));
        g2.fill(second);
        g2.setComposite(composite);
    }

    /**
     * The series' values, then how far the first is above or below the second, if both are shown.
     */
    @Override
    protected List<HoverRow> hoverRows(int category) {
        List<HoverRow> rows = new ArrayList<>(super.hoverRows(category));
        if (getSeriesCount() >= 2 && isSeriesVisible(0) && isSeriesVisible(1)) {
            double difference = getValues(0)[category] - getValues(1)[category];
            rows.add(new HoverRow(difference >= 0 ? getPositiveColor() : getNegativeColor(), "Difference",
                    (difference >= 0 ? "+" : "-") + formatValue(Math.abs(difference))));
        }
        return rows;
    }
}
