package com.swingcraft4j.animatedchart.chart;

import com.formdev.flatlaf.util.UIScale;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Plot that paints its data only up to a fraction of the width, to draw it in from the left.
 */
class RevealPlot extends XYPlot {

    private double reveal = 1;

    RevealPlot(XYDataset dataset, ValueAxis domainAxis, ValueAxis rangeAxis, XYItemRenderer renderer) {
        super(dataset, domainAxis, rangeAxis, renderer);
    }

    double getReveal() {
        return reveal;
    }

    /**
     * Visible fraction of the width, from 0 to 1; overshooting easings are clamped.
     */
    void setReveal(double reveal) {
        this.reveal = Math.clamp(reveal, 0, 1);
        fireChangeEvent();
    }

    /**
     * Whether a panel x inside the data area has been revealed.
     */
    boolean isRevealed(double x, Rectangle2D dataArea) {
        return x <= dataArea.getX() + dataArea.getWidth() * reveal;
    }

    @Override
    public boolean render(Graphics2D g2, Rectangle2D dataArea, int index, PlotRenderingInfo info, CrosshairState crosshairState) {
        if (reveal >= 1) {
            return super.render(g2, dataArea, index, info, crosshairState);
        }
        Shape clip = g2.getClip();
        // room above and below for the stroke and points at the edges
        double pad = UIScale.scale(8);
        g2.clip(new Rectangle2D.Double(dataArea.getX() - pad, dataArea.getY() - pad,
                pad + dataArea.getWidth() * reveal, dataArea.getHeight() + pad * 2));
        try {
            return super.render(g2, dataArea, index, info, crosshairState);
        } finally {
            g2.setClip(clip);
        }
    }
}
