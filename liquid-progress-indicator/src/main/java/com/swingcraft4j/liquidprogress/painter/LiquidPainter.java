package com.swingcraft4j.liquidprogress.painter;

import com.swingcraft4j.liquidprogress.LiquidProgress;

import java.awt.*;

/**
 * A {@link LiquidProgress} style, painted every frame inside the {@link LiquidProgress#METER_SIZE} square; anything outside it is cut off.
 */
public interface LiquidPainter {

    /**
     * Area the liquid fills, from the bottom (0%) to the top (100%) of its bounds.
     */
    Shape getFluidShape(LiquidProgress c);

    /**
     * Painted under the liquid.
     */
    void paintBackground(Graphics2D g, LiquidProgress c);

    /**
     * Painted over the liquid and text.
     */
    void paintForeground(Graphics2D g, LiquidProgress c);

    /**
     * Default size of the percentage text; {@link LiquidProgress#setFontSize} overrides it.
     */
    default float getFontSize(LiquidProgress c) {
        return 70;
    }

    /**
     * Default color of the text where it's over the empty track; {@code null} paints the text in one color.
     */
    default Color getTrackTextColor(LiquidProgress c) {
        return null;
    }
}
