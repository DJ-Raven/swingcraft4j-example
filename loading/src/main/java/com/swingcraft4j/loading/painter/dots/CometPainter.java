package com.swingcraft4j.loading.painter.dots;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;

/**
 * Comet circling with a tail of shrinking dots that stretches out and snaps back.
 */
public class CometPainter implements LoadingPainter {

    // dot diameters, head first, relative to the orbit radius unit
    private static final float[] DIAMETERS = {0.2f, 0.16f, 0.12f, 0.08f, 0.046f};

    // tail shapes: (x, y) of each dot on a unit orbit, head at the top and the tail trailing counterclockwise
    private static final float[][] COLLAPSED = {{0, -1}, {0, -1}, {0, -1}, {0, -1}, {0, -1}};
    private static final float[][] SHORT = {{0, -1}, {-0.105f, -0.994f}, {-0.208f, -0.978f}, {-0.308f, -0.95f}, {-0.358f, -0.934f}};
    private static final float[][] LONG = {{0, -1}, {-0.407f, -0.913f}, {-0.669f, -0.743f}, {-0.808f, -0.588f}, {-0.902f, -0.41f}};
    private static final float[][] LONGEST = {{0, -1}, {-0.454f, -0.892f}, {-0.777f, -0.629f}, {-0.934f, -0.358f}, {-0.988f, -0.108f}};

    private static final float[] TIMES = {0, 0.05f, 0.1f, 0.2f, 0.38f, 0.59f, 0.95f, 1};
    private static final float[][][] FRAMES = {COLLAPSED, COLLAPSED, SHORT, LONG, LONGEST, SHORT, COLLAPSED, COLLAPSED};

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        // head center sits on an orbit of 0.83 units, its edge touches the icon bounds at 0.93 units
        float unit = size / 2 / 0.93f;
        float orbit = unit * 0.83f;
        float c = size / 2;

        int k = 0;
        while (k < TIMES.length - 2 && fraction >= TIMES[k + 1]) {
            k++;
        }
        float t = easeCurve((fraction - TIMES[k]) / (TIMES[k + 1] - TIMES[k]));
        float[][] from = FRAMES[k];
        float[][] to = FRAMES[k + 1];

        AffineTransform old = g.getTransform();
        g.rotate(2 * Math.PI * easeCurve(fraction), c, c);
        // smallest dot first so the head is drawn on top
        for (int i = DIAMETERS.length - 1; i >= 0; i--) {
            float x = c + orbit * (from[i][0] + (to[i][0] - from[i][0]) * t);
            float y = c + orbit * (from[i][1] + (to[i][1] - from[i][1]) * t);
            float r = unit * DIAMETERS[i] / 2;
            g.fill(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2));
        }
        g.setTransform(old);
    }

    @Override
    public int getDuration() {
        return 1700;
    }

    /**
     * Ease timing, cubic-bezier(0.25, 0.1, 0.25, 1).
     */
    private static float easeCurve(float x) {
        float lo = 0;
        float hi = 1;
        float s = x;
        for (int i = 0; i < 20; i++) {
            s = (lo + hi) / 2;
            if (bezier(s, 0.25f, 0.25f) < x) {
                lo = s;
            } else {
                hi = s;
            }
        }
        return bezier(s, 0.1f, 1f);
    }

    private static float bezier(float s, float p1, float p2) {
        float u = 1 - s;
        return 3 * u * u * s * p1 + 3 * u * s * s * p2 + s * s * s;
    }
}
