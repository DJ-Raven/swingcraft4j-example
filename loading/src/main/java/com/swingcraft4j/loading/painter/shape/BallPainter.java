package com.swingcraft4j.loading.painter.shape;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Ball bouncing with a squash on landing.
 */
public class BallPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        float d = size * 0.36f;
        // parabola: 0 on the ground, 1 at the top
        float height = 4 * fraction * (1 - fraction);
        float squash = Math.max(0, 1 - height / 0.12f);
        float w = d * (1 + 0.3f * squash);
        float h = d * (1 - 0.3f * squash);
        float y = size - h - height * (size - d);
        g.fill(new Ellipse2D.Float((size - w) / 2, y, w, h));
    }

    @Override
    public int getDuration() {
        return 900;
    }
}
