package com.swingcraft4j.animatedchart.chart;

import com.swingcraft4j.animatedchart.ChartStyle;
import com.swingcraft4j.animatedchart.chart.LineChart.LineStyle;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws each series as one path in the chart's line style, over an optional area fill.
 */
class LineRenderer extends XYLineAndShapeRenderer {

    // share of the series color left in the line at the bottom of the value range, with the value gradient
    private static final float GRADIENT_LOW = 0.3f;

    /**
     * Paints the area that belongs to a series' line, under the line.
     */
    interface AreaPainter {
        void paint(Graphics2D g2, int series, Path2D line, Rectangle2D dataArea);
    }

    // screen points of the series being drawn
    private final List<Point2D.Double> points = new ArrayList<>();
    // opacity of each series, 1 if it has none here; a series fades through it when hidden or shown
    private final List<Float> opacity = new ArrayList<>();
    private LineStyle lineStyle = LineStyle.SMOOTH;
    private boolean areaFilled = true;
    private boolean valueGradient;
    private AreaPainter areaPainter;
    private float areaOpacity = 0.31f;
    // what the value gradient fades the line into
    private Color background = Color.WHITE;

    LineRenderer() {
        super(true, true);
        setDrawSeriesLineAsPath(true);
    }

    LineStyle getLineStyle() {
        return lineStyle;
    }

    void setLineStyle(LineStyle lineStyle) {
        this.lineStyle = lineStyle;
        fireChangeEvent();
    }

    boolean isAreaFilled() {
        return areaFilled;
    }

    void setAreaFilled(boolean areaFilled) {
        this.areaFilled = areaFilled;
        fireChangeEvent();
    }

    boolean isValueGradient() {
        return valueGradient;
    }

    void setValueGradient(boolean valueGradient) {
        this.valueGradient = valueGradient;
        fireChangeEvent();
    }

    void setAreaPainter(AreaPainter areaPainter) {
        this.areaPainter = areaPainter;
    }

    float getAreaOpacity() {
        return areaOpacity;
    }

    void setAreaOpacity(float areaOpacity) {
        this.areaOpacity = areaOpacity;
        fireChangeEvent();
    }

    void setBackground(Color background) {
        this.background = background;
        fireChangeEvent();
    }

    float getSeriesOpacity(int series) {
        return series < opacity.size() ? opacity.get(series) : 1;
    }

    void setSeriesOpacity(int series, float value) {
        while (opacity.size() <= series) {
            opacity.add(1f);
        }
        opacity.set(series, value);
        fireChangeEvent();
    }

    /**
     * Draws the item at its series' opacity, or not at all once the series has faded out.
     */
    @Override
    public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
                         ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset, int series, int item,
                         CrosshairState crosshairState, int pass) {
        float alpha = getSeriesOpacity(series);
        if (alpha <= 0) {
            return;
        }
        Composite composite = g2.getComposite();
        if (alpha < 1) {
            g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
        }
        try {
            super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState, pass);
        } finally {
            g2.setComposite(composite);
        }
    }

    /**
     * Collects the series' points item by item, then fills and draws the whole line at the last one.
     */
    @Override
    protected void drawPrimaryLineAsPath(XYItemRendererState state, Graphics2D g2, XYPlot plot, XYDataset dataset, int pass,
                                         int series, int item, ValueAxis xAxis, ValueAxis yAxis, Rectangle2D dataArea) {
        if (item == 0) {
            points.clear();
        }
        double x = dataset.getXValue(series, item);
        double y = dataset.getYValue(series, item);
        if (!Double.isNaN(x) && !Double.isNaN(y)) {
            points.add(new Point2D.Double(xAxis.valueToJava2D(x, dataArea, plot.getDomainAxisEdge()),
                    yAxis.valueToJava2D(y, dataArea, plot.getRangeAxisEdge())));
        }
        if (item < dataset.getItemCount(series) - 1 || points.size() < 2) {
            return;
        }

        Path2D line = LinePaths.create(points, lineStyle);
        Paint paint = getItemPaint(series, item);
        if (areaFilled && areaPainter != null) {
            areaPainter.paint(g2, series, line, dataArea);
        }
        if (valueGradient && paint instanceof Color color) {
            // full color at the top of the value range, a light tint of it at the bottom
            paint = new GradientPaint(0, (float) dataArea.getMinY(), color,
                    0, (float) dataArea.getMaxY(), ChartStyle.mix(color, background, GRADIENT_LOW));
        }
        g2.setStroke(getItemStroke(series, item));
        g2.setPaint(paint);
        g2.draw(line);
    }
}
