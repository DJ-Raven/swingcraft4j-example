package com.swingcraft4j.loading.painter.shape;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Path2D;

import static com.swingcraft4j.loading.LoadingPainter.alpha;
import static com.swingcraft4j.loading.painter.PainterMath.roundStroke;

/**
 * Segment travelling along a figure-eight track.
 */
public class InfinityPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        float stroke = size / 10f;
        float a = (size - stroke) / 2;
        Color color = g.getColor();
        g.setStroke(roundStroke(stroke));
        g.setColor(alpha(color, 0.2f));
        g.draw(lemniscate(size, a, 0, 2 * Math.PI, 64));
        g.setColor(color);
        double start = fraction * 2 * Math.PI;
        g.draw(lemniscate(size, a, start, start + 0.6 * Math.PI, 24));
    }

    @Override
    public int getDuration() {
        return 2000;
    }

    /**
     * Part of a lemniscate of Bernoulli (the figure-eight) between two parameter values.
     */
    private static Path2D lemniscate(float size, float a, double from, double to, int steps) {
        Path2D path = new Path2D.Float();
        for (int i = 0; i <= steps; i++) {
            double t = from + (to - from) * i / steps;
            double sin = Math.sin(t);
            double cos = Math.cos(t);
            double den = 1 + sin * sin;
            double x = size / 2.0 + a * cos / den;
            double y = size / 2.0 + a * sin * cos / den;
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        return path;
    }
}
