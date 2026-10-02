package com.swingcraft4j.animatedchart.chart;

import com.formdev.flatlaf.util.UIScale;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.NumberTick;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Category axis that labels only as many categories as fit with a gap between them, for dense data.
 */
class SpacedAxis extends SymbolAxis {

    SpacedAxis(String[] categories) {
        super(null, categories);
        setGridBandsVisible(false);
        // a tick per category; refreshTicks then drops the labels that don't fit
        setTickUnit(new NumberTickUnit(1));
        // half a step of space before the first and after the last category
        setRange(-0.5, categories.length - 0.5);
    }

    @Override
    public List<NumberTick> refreshTicks(Graphics2D g2, AxisState state, Rectangle2D dataArea, RectangleEdge edge) {
        FontMetrics metrics = g2.getFontMetrics(getTickLabelFont());
        double gap = UIScale.scale(16);
        double lastEnd = Double.NEGATIVE_INFINITY;
        List<NumberTick> ticks = new ArrayList<>();
        for (Object each : super.refreshTicks(g2, state, dataArea, edge)) {
            NumberTick tick = (NumberTick) each;
            String label = valueToString(tick.getValue());
            double middle = valueToJava2D(tick.getValue(), dataArea, edge);
            double half = metrics.stringWidth(label) / 2.0;
            // also not past the right edge, where it would be cut off
            boolean fits = middle - half >= lastEnd + gap && middle + half <= dataArea.getMaxX() + gap / 2;
            if (fits) {
                lastEnd = middle + half;
            }
            ticks.add(new NumberTick(tick.getNumber(), fits ? label : "", tick.getTextAnchor(), tick.getRotationAnchor(), tick.getAngle()));
        }
        return ticks;
    }
}
