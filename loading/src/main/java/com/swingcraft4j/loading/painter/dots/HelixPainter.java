package com.swingcraft4j.loading.painter.dots;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

import static com.swingcraft4j.loading.LoadingPainter.alpha;

/**
 * DNA double helix turning: two strands of dots with near dots bigger and brighter.
 */
public class HelixPainter implements LoadingPainter {

    private static final int PAIRS = 7;

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        Color color = g.getColor();
        float amplitude = size * 0.3f;
        float cy = size / 2;
        g.setStroke(new BasicStroke(size / 40f));
        for (int i = 0; i < PAIRS; i++) {
            double phase = 2 * Math.PI * (fraction + (double) i / PAIRS);
            float x = size * (0.5f + i) / PAIRS;
            float offset = amplitude * (float) Math.sin(phase);
            // depth: +1 is closest to the viewer; the two strands are always on opposite sides
            float depth = (float) Math.cos(phase);
            g.setColor(alpha(color, 0.2f));
            g.draw(new Line2D.Float(x, cy - offset, x, cy + offset));
            strandDot(g, color, x, cy - offset, size, depth);
            strandDot(g, color, x, cy + offset, size, -depth);
        }
    }

    @Override
    public int getDuration() {
        return 1600;
    }

    private static void strandDot(Graphics2D g, Color color, float x, float y, float size, float depth) {
        float r = size / 14f * (0.65f + 0.35f * depth);
        g.setColor(alpha(color, 0.4f + 0.6f * (depth + 1) / 2));
        g.fill(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2));
    }
}
