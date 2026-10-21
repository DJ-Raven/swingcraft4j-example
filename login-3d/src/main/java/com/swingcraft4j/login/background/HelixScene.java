package com.swingcraft4j.login.background;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/**
 * A DNA double helix, leaning across the screen and twisting around its axis.
 */
public class HelixScene extends Scene {

    private static final int STEPS = 64;
    private static final double CAMERA = 3;
    private static final double LEAN = 0.5;

    // the two strands on screen, and their depth from 0 (far) to 1 (near)
    private final double[] ax = new double[STEPS], ay = new double[STEPS], ad = new double[STEPS];
    private final double[] bx = new double[STEPS], by = new double[STEPS], bd = new double[STEPS];
    private final double[] out = new double[3];

    public HelixScene() {
        super("Helix");
    }

    @Override
    public void paint(Graphics2D g2, View view) {
        double scale = Math.min(view.width, view.height) * 0.45;
        double cx = view.width / 2.0 - view.mouseX * 40;
        double cy = view.height / 2.0 - view.mouseY * 40;
        paintGlow(g2, cx, cy, scale * 1.2, view.accent, 55);

        view.setAngles(view.tiltX, view.tiltY);
        double cosLean = Math.cos(LEAN), sinLean = Math.sin(LEAN);
        for (int i = 0; i < STEPS; i++) {
            double t = i / (STEPS - 1.0);
            double y = (t - 0.5) * 2.8;
            // the twist turns with the spin and the drag
            double a = t * Math.PI * 5 + view.spinY * 2 + view.spinX;
            for (int strand = 0; strand < 2; strand++) {
                double angle = a + strand * Math.PI;
                double x = Math.cos(angle) * 0.5, z = Math.sin(angle) * 0.5;
                // lean the axis across the screen
                view.project(x * cosLean - y * sinLean, x * sinLean + y * cosLean, z, cx, cy, scale, CAMERA, out);
                double depth = (1 - out[2] * 2) / 2;
                if (strand == 0) {
                    ax[i] = out[0];
                    ay[i] = out[1];
                    ad[i] = depth;
                } else {
                    bx[i] = out[0];
                    by[i] = out[1];
                    bd[i] = depth;
                }
            }
        }

        Line2D.Double line = new Line2D.Double();
        g2.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < STEPS; i += 2) {
            // a base pair, lit when the mouse is near its middle
            double mx = (ax[i] + bx[i]) / 2, my = (ay[i] + by[i]) / 2;
            double d = (ad[i] + bd[i]) / 2;
            double power = linkMouse(g2, view, mx, my, d, 140, line);
            g2.setColor(power > 0 ? view.node(d + power * 0.5) : view.edge(d));
            line.setLine(ax[i], ay[i], bx[i], by[i]);
            g2.draw(line);
        }
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i + 1 < STEPS; i++) {
            g2.setColor(view.node((ad[i] + ad[i + 1]) / 2 * 0.8));
            line.setLine(ax[i], ay[i], ax[i + 1], ay[i + 1]);
            g2.draw(line);
            g2.setColor(view.node((bd[i] + bd[i + 1]) / 2 * 0.8));
            line.setLine(bx[i], by[i], bx[i + 1], by[i + 1]);
            g2.draw(line);
        }
        for (int i = 0; i < STEPS; i += 2) {
            dot(g2, view, ax[i], ay[i], ad[i]);
            dot(g2, view, bx[i], by[i], bd[i]);
        }
    }

    private static void dot(Graphics2D g2, View view, double x, double y, double depth) {
        double size = 2.5 + depth * 4;
        if (view.mouseDistance(x, y) < 140) {
            size += (1 - view.mouseDistance(x, y) / 140) * 4;
        }
        g2.setColor(view.node(depth));
        g2.fill(new Ellipse2D.Double(x - size / 2, y - size / 2, size, size));
    }
}
