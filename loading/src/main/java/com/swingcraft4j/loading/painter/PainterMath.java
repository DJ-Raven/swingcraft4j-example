package com.swingcraft4j.loading.painter;

import java.awt.*;

/**
 * Shared easing and timing helpers for painters.
 */
public final class PainterMath {

    private PainterMath() {
    }

    /**
     * Cubic ease-in-out.
     */
    public static float ease(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }

    /**
     * Cubic ease-out.
     */
    public static float easeOut(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    /**
     * Wraps any value into 0..1.
     */
    public static float wrap(float value) {
        return value - (float) Math.floor(value);
    }

    /**
     * Single sine bump over the first {@code width} of the cycle, 0 for the rest.
     */
    public static float pulse(float fraction, float width) {
        float p = wrap(fraction);
        return p < width ? (float) Math.sin(p / width * Math.PI) : 0;
    }

    /**
     * Stroke with round caps and joins.
     */
    public static BasicStroke roundStroke(float width) {
        return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }
}
