package com.swingcraft4j.loading.painter.dots;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Two dots circling each other while growing and shrinking in turn.
 */
public class ChasingDotsPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        float c = size / 2;
        float orbit = size * 0.28f;
        double rotation = 2 * Math.PI * fraction;
        // opposite phases: while one dot grows the other shrinks
        float wave = (float) Math.cos(2 * Math.PI * fraction);
        dot(g, c, orbit, rotation, size * 0.22f * (1 - wave) / 2);
        dot(g, c, orbit, rotation + Math.PI, size * 0.22f * (1 + wave) / 2);
    }

    @Override
    public int getDuration() {
        return 2000;
    }

    private static void dot(Graphics2D g, float c, float orbit, double angle, float r) {
        float x = c + orbit * (float) Math.sin(angle);
        float y = c - orbit * (float) Math.cos(angle);
        g.fill(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2));
    }
}
