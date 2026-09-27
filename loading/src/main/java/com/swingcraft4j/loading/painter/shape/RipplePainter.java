package com.swingcraft4j.loading.painter.shape;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Ellipse2D;

import static com.swingcraft4j.loading.LoadingPainter.alpha;
import static com.swingcraft4j.loading.painter.PainterMath.wrap;

/**
 * Two outlined rings expanding and fading, half a cycle apart.
 */
public class RipplePainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        Color color = g.getColor();
        float stroke = size / 12f;
        g.setStroke(new BasicStroke(stroke));
        for (int i = 0; i < 2; i++) {
            float p = wrap(fraction + i * 0.5f);
            float r = (size - stroke) / 2 * (1 - (1 - p) * (1 - p));
            g.setColor(alpha(color, 1 - p));
            g.draw(new Ellipse2D.Float(size / 2 - r, size / 2 - r, r * 2, r * 2));
        }
    }

    @Override
    public int getDuration() {
        return 1500;
    }
}
