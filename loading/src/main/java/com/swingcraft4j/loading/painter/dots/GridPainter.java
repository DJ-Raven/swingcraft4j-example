package com.swingcraft4j.loading.painter.dots;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Ellipse2D;

import static com.swingcraft4j.loading.LoadingPainter.alpha;
import static com.swingcraft4j.loading.painter.PainterMath.pulse;

/**
 * Three-by-three dots pulsing in a diagonal wave.
 */
public class GridPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        Color color = g.getColor();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                float pulse = pulse(fraction - (row + col) * 0.1f, 0.5f);
                float r = size / 12f * (0.5f + 0.5f * pulse) + size / 24f;
                float cx = size * (1 + 2 * col) / 6f;
                float cy = size * (1 + 2 * row) / 6f;
                g.setColor(alpha(color, 0.3f + 0.7f * pulse));
                g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
            }
        }
    }

    @Override
    public int getDuration() {
        return 1200;
    }
}
