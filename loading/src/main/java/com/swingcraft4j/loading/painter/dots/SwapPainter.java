package com.swingcraft4j.loading.painter.dots;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Ellipse2D;

import static com.swingcraft4j.loading.painter.PainterMath.ease;

/**
 * Two dots swapping places around the center.
 */
public class SwapPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        float r = size * 0.14f;
        float radius = size / 2 - r;
        // half a turn per cycle: the dots look identical, so the loop is seamless
        double angle = Math.PI * ease(fraction);
        float dx = radius * (float) Math.cos(angle);
        float dy = radius * (float) Math.sin(angle);
        g.fill(new Ellipse2D.Float(size / 2 + dx - r, size / 2 + dy - r, r * 2, r * 2));
        g.fill(new Ellipse2D.Float(size / 2 - dx - r, size / 2 - dy - r, r * 2, r * 2));
    }
}
