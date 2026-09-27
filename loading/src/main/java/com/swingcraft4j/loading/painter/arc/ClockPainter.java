package com.swingcraft4j.loading.painter.arc;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

import static com.swingcraft4j.loading.painter.PainterMath.roundStroke;

/**
 * Clock face with a fast minute hand and a slow hour hand.
 */
public class ClockPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        float stroke = size / 12f;
        float c = size / 2;
        float r = (size - stroke) / 2;
        g.setStroke(roundStroke(stroke));
        g.draw(new Ellipse2D.Float(stroke / 2, stroke / 2, r * 2, r * 2));
        // the minute hand turns four times per hour-hand turn, so both meet at the top each cycle
        hand(g, c, r * 0.65f, fraction * 4);
        hand(g, c, r * 0.4f, fraction);
    }

    @Override
    public int getDuration() {
        return 4000;
    }

    private static void hand(Graphics2D g, float c, float length, float turns) {
        double angle = 2 * Math.PI * turns - Math.PI / 2;
        g.draw(new Line2D.Float(c, c, c + length * (float) Math.cos(angle), c + length * (float) Math.sin(angle)));
    }
}
