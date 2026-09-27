package com.swingcraft4j.loading.painter.arc;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Arc2D;

import static com.swingcraft4j.loading.painter.PainterMath.ease;
import static com.swingcraft4j.loading.painter.PainterMath.roundStroke;

/**
 * Two opposite arcs spinning with easing.
 */
public class DualRingPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        float stroke = size / 8f;
        float d = size - stroke;
        float rotation = 360 * ease(fraction);
        g.setStroke(roundStroke(stroke));
        for (int i = 0; i < 2; i++) {
            g.draw(new Arc2D.Float(stroke / 2, stroke / 2, d, d, 90 - rotation - i * 180, -80, Arc2D.OPEN));
        }
    }

    @Override
    public int getDuration() {
        return 1100;
    }
}
