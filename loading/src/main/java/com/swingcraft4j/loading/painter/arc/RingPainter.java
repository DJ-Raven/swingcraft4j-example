package com.swingcraft4j.loading.painter.arc;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

import static com.swingcraft4j.loading.LoadingPainter.alpha;
import static com.swingcraft4j.loading.painter.PainterMath.ease;
import static com.swingcraft4j.loading.painter.PainterMath.roundStroke;

/**
 * Arc spinning with easing around a faint track.
 */
public class RingPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        float stroke = size / 8f;
        float d = size - stroke;
        Color color = g.getColor();
        g.setStroke(roundStroke(stroke));
        g.setColor(alpha(color, 0.2f));
        g.draw(new Ellipse2D.Float(stroke / 2, stroke / 2, d, d));
        g.setColor(color);
        float extent = 70 + 60 * (float) Math.sin(fraction * Math.PI);
        g.draw(new Arc2D.Float(stroke / 2, stroke / 2, d, d, 90 - 360 * ease(fraction), -extent, Arc2D.OPEN));
    }
}
