package com.swingcraft4j.loading.painter.arc;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

import static com.swingcraft4j.loading.LoadingPainter.alpha;
import static com.swingcraft4j.loading.painter.PainterMath.roundStroke;

/**
 * Radar screen with a sweeping beam and a fading trail.
 */
public class RadarPainter implements LoadingPainter {

    private static final int TRAIL_SLICES = 18;
    private static final float SLICE_DEGREES = 5;

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        Color color = g.getColor();
        float stroke = size / 16f;
        float c = size / 2;
        float r = (size - stroke) / 2;
        g.setStroke(roundStroke(stroke));
        g.setColor(alpha(color, 0.3f));
        g.draw(new Ellipse2D.Float(c - r, c - r, r * 2, r * 2));
        g.draw(new Ellipse2D.Float(c - r / 2, c - r / 2, r, r));

        // the beam turns clockwise, so the trail sits at larger (counterclockwise) Java angles
        float beam = 90 - fraction * 360;
        for (int i = 0; i < TRAIL_SLICES; i++) {
            g.setColor(alpha(color, 0.45f * (1 - (float) i / TRAIL_SLICES)));
            g.fill(new Arc2D.Float(c - r, c - r, r * 2, r * 2, beam + i * SLICE_DEGREES, SLICE_DEGREES + 0.5f, Arc2D.PIE));
        }
        double angle = Math.toRadians(beam);
        g.setColor(color);
        g.draw(new Line2D.Float(c, c, c + r * (float) Math.cos(angle), c - r * (float) Math.sin(angle)));
    }

    @Override
    public int getDuration() {
        return 2000;
    }
}
