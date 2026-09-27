package com.swingcraft4j.loading.painter.shape;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

import static com.swingcraft4j.loading.LoadingPainter.alpha;
import static com.swingcraft4j.loading.painter.PainterMath.wrap;

/**
 * Pac-Man chomping on a row of dots sliding into its mouth.
 */
public class PacmanPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        Color color = g.getColor();
        float cx = size * 0.34f;
        float cy = size / 2;
        float r = size * 0.3f;
        float spacing = size * 0.18f;
        float dot = size / 20f;

        // two chomps per cycle; the dots move one spacing per chomp
        float step = wrap(fraction * 2);
        for (int i = 0; i < 4; i++) {
            float x = cx + size * 0.1f + i * spacing - step * spacing;
            // the dot entering on the right fades in so it doesn't pop into view
            float a = i == 3 ? step : 1;
            g.setColor(alpha(color, a));
            g.fill(new Ellipse2D.Float(x - dot, cy - dot, dot * 2, dot * 2));
        }

        float mouth = 40 * (float) Math.abs(Math.sin(Math.PI * step));
        g.setColor(color);
        g.fill(new Arc2D.Float(cx - r, cy - r, r * 2, r * 2, mouth, 360 - mouth * 2, Arc2D.PIE));
    }

    @Override
    public int getDuration() {
        return 1000;
    }
}
