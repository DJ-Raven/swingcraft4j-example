package com.swingcraft4j.loading;

import java.awt.*;

/**
 * Paints one frame of a loading animation, {@code size} high and {@link #getWidth(float)} wide.
 */
@FunctionalInterface
public interface LoadingPainter {

    /**
     * Paints the frame at {@code fraction} (0..1) of the cycle; {@code g} is antialiased, translated and colored.
     */
    void paint(Graphics2D g, float size, float fraction);

    /**
     * Width of the frame for the given (scaled) height; square by default.
     */
    default float getWidth(float size) {
        return size;
    }

    /**
     * Default duration of one animation cycle in milliseconds.
     */
    default int getDuration() {
        return 1000;
    }

    /**
     * Returns the color with its alpha multiplied by {@code alpha} (0..1).
     */
    static Color alpha(Color color, float alpha) {
        int a = Math.round(color.getAlpha() * Math.clamp(alpha, 0f, 1f));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }
}
