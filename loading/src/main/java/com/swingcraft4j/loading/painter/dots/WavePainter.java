package com.swingcraft4j.loading.painter.dots;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Five dots riding a sine wave.
 */
public class WavePainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        float r = size / 14f;
        for (int i = 0; i < 5; i++) {
            float cx = size * (0.1f + i * 0.2f);
            float cy = size / 2 - (float) Math.sin(2 * Math.PI * (fraction - i * 0.12f)) * size * 0.25f;
            g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
        }
    }

    @Override
    public int getDuration() {
        return 1100;
    }
}
