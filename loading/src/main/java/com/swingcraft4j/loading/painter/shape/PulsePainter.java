package com.swingcraft4j.loading.painter.shape;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Ellipse2D;

import static com.swingcraft4j.loading.LoadingPainter.alpha;
import static com.swingcraft4j.loading.painter.PainterMath.easeOut;
import static com.swingcraft4j.loading.painter.PainterMath.wrap;

/**
 * Two discs growing from the center and fading out, half a cycle apart.
 */
public class PulsePainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        Color color = g.getColor();
        for (int i = 0; i < 2; i++) {
            float p = wrap(fraction + i * 0.5f);
            float r = size / 2 * easeOut(p);
            g.setColor(alpha(color, 0.85f * (1 - p)));
            g.fill(new Ellipse2D.Float(size / 2 - r, size / 2 - r, r * 2, r * 2));
        }
    }

    @Override
    public int getDuration() {
        return 1600;
    }
}
