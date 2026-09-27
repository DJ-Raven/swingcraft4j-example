package com.swingcraft4j.loading.painter.shape;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

import static com.swingcraft4j.loading.LoadingPainter.alpha;
import static com.swingcraft4j.loading.painter.PainterMath.ease;

/**
 * Segment sliding along a horizontal track, like an indeterminate progress bar.
 */
public class LinePainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        Color color = g.getColor();
        float h = size / 7f;
        Shape track = new RoundRectangle2D.Float(0, (size - h) / 2, size, h, h, h);
        g.setColor(alpha(color, 0.2f));
        g.fill(track);
        float length = size * 0.45f;
        float x = -length + (size + length) * ease(fraction);
        // intersecting areas (instead of clipping) keeps the rounded ends antialiased
        Area segment = new Area(new RoundRectangle2D.Float(x, (size - h) / 2, length, h, h, h));
        segment.intersect(new Area(track));
        g.setColor(color);
        g.fill(segment);
    }

    @Override
    public int getDuration() {
        return 1400;
    }
}
