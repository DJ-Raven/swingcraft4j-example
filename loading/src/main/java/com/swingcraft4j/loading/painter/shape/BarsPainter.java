package com.swingcraft4j.loading.painter.shape;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

import static com.swingcraft4j.loading.LoadingPainter.alpha;
import static com.swingcraft4j.loading.painter.PainterMath.pulse;

/**
 * Three bars stretching one after another.
 */
public class BarsPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        Color color = g.getColor();
        float w = size * 0.2f;
        for (int i = 0; i < 3; i++) {
            float pulse = pulse(fraction - i * 0.15f, 0.6f);
            float h = size * (0.35f + 0.65f * pulse);
            g.setColor(alpha(color, 0.4f + 0.6f * pulse));
            g.fill(new RoundRectangle2D.Float(i * size * 0.4f, (size - h) / 2, w, h, w * 0.6f, w * 0.6f));
        }
    }
}
