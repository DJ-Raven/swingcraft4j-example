package com.swingcraft4j.loading.painter.dots;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Ellipse2D;

import static com.swingcraft4j.loading.painter.PainterMath.ease;

/**
 * Three dots scrolling to the right: one grows in on the left while one shrinks away on the right.
 */
public class EllipsisPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        float t = ease(fraction);
        float r = size / 10f;
        float y = size / 2;
        float left = size * 0.2f;
        float middle = size * 0.5f;
        float right = size * 0.8f;
        // the cycle ends with dots on the same three spots it started from, so the loop is seamless
        dot(g, left, y, r * t);
        dot(g, left + (middle - left) * t, y, r);
        dot(g, middle + (right - middle) * t, y, r);
        dot(g, right, y, r * (1 - t));
    }

    @Override
    public int getDuration() {
        return 700;
    }

    private static void dot(Graphics2D g, float x, float y, float r) {
        g.fill(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2));
    }
}
