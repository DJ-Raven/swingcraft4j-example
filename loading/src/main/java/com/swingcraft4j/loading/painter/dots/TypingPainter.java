package com.swingcraft4j.loading.painter.dots;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Ellipse2D;

import static com.swingcraft4j.loading.LoadingPainter.alpha;
import static com.swingcraft4j.loading.painter.PainterMath.pulse;

/**
 * Three dots hopping in turn, like a typing indicator.
 */
public class TypingPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        Color color = g.getColor();
        float r = size / 10f;
        for (int i = 0; i < 3; i++) {
            float pulse = pulse(fraction - i * 0.15f, 0.4f);
            float cx = size * (1 + 2 * i) / 6f;
            float cy = size * 0.6f - pulse * size * 0.22f;
            g.setColor(alpha(color, 0.45f + 0.55f * pulse));
            g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
        }
    }

    @Override
    public int getDuration() {
        return 1200;
    }
}
