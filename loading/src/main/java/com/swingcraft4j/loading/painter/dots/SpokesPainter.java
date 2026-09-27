package com.swingcraft4j.loading.painter.dots;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Line2D;

import static com.swingcraft4j.loading.LoadingPainter.alpha;
import static com.swingcraft4j.loading.painter.PainterMath.roundStroke;

/**
 * Twelve spokes with a fading trail, like a classic activity indicator.
 */
public class SpokesPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        Color color = g.getColor();
        float c = size / 2;
        g.setStroke(roundStroke(size / 12f));
        int head = (int) (fraction * 12);
        for (int i = 0; i < 12; i++) {
            double angle = 2 * Math.PI * i / 12 - Math.PI / 2;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float behind = Math.floorMod(head - i, 12);
            g.setColor(alpha(color, 1 - behind / 12f * 0.85f));
            g.draw(new Line2D.Float(c + cos * size * 0.22f, c + sin * size * 0.22f, c + cos * size * 0.44f, c + sin * size * 0.44f));
        }
    }
}
