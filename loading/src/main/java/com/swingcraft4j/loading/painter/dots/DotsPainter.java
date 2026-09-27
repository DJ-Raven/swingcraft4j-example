package com.swingcraft4j.loading.painter.dots;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Ellipse2D;

import static com.swingcraft4j.loading.LoadingPainter.alpha;
import static com.swingcraft4j.loading.painter.PainterMath.pulse;

/**
 * Three dots pulsing one after another.
 */
public class DotsPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        Color color = g.getColor();
        for (int i = 0; i < 3; i++) {
            float pulse = pulse(fraction - i * 0.16f, 0.6f);
            float r = size / 8f * (0.55f + 0.45f * pulse);
            float cx = size * (1 + 2 * i) / 6f;
            g.setColor(alpha(color, 0.3f + 0.7f * pulse));
            g.fill(new Ellipse2D.Float(cx - r, size / 2 - r, r * 2, r * 2));
        }
    }
}
