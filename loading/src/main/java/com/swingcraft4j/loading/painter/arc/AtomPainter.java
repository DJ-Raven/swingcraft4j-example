package com.swingcraft4j.loading.painter.arc;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;

import static com.swingcraft4j.loading.LoadingPainter.alpha;

/**
 * Nucleus with three tilted orbits, each carrying an electron.
 */
public class AtomPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        Color color = g.getColor();
        float c = size / 2;
        float electron = size / 14f;
        // the long radius leaves room for the electron at the orbit's tip
        float a = c - electron;
        float b = size * 0.16f;
        g.setStroke(new BasicStroke(size / 24f));
        for (int i = 0; i < 3; i++) {
            double tilt = Math.PI * i / 3;
            AffineTransform orbit = AffineTransform.getRotateInstance(tilt, c, c);
            g.setColor(alpha(color, 0.35f));
            g.draw(orbit.createTransformedShape(new Ellipse2D.Float(c - a, c - b, a * 2, b * 2)));
            // electrons are a third of a cycle apart so they never bunch up
            double t = 2 * Math.PI * (fraction + i / 3.0);
            double x = a * Math.cos(t);
            double y = b * Math.sin(t);
            float ex = c + (float) (x * Math.cos(tilt) - y * Math.sin(tilt));
            float ey = c + (float) (x * Math.sin(tilt) + y * Math.cos(tilt));
            g.setColor(color);
            g.fill(new Ellipse2D.Float(ex - electron, ey - electron, electron * 2, electron * 2));
        }
        float nucleus = size / 10f;
        g.fill(new Ellipse2D.Float(c - nucleus, c - nucleus, nucleus * 2, nucleus * 2));
    }

    @Override
    public int getDuration() {
        return 1500;
    }
}
