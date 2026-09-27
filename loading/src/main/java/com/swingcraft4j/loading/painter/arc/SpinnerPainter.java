package com.swingcraft4j.loading.painter.arc;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Arc2D;

import static com.swingcraft4j.loading.painter.PainterMath.ease;
import static com.swingcraft4j.loading.painter.PainterMath.roundStroke;

/**
 * Arc that grows and shrinks while rotating.
 */
public class SpinnerPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        float stroke = size / 8f;
        float head = 270 * ease(Math.min(fraction * 2, 1));
        float tail = 270 * ease(Math.max(fraction * 2 - 1, 0));
        // rotating 450° per cycle makes the arc end where the next cycle starts
        float rotation = fraction * 450;
        g.setStroke(roundStroke(stroke));
        g.draw(new Arc2D.Float(stroke / 2, stroke / 2, size - stroke, size - stroke,
                90 - rotation - tail, -(head - tail + 10), Arc2D.OPEN));
    }

    @Override
    public int getDuration() {
        return 1500;
    }
}
