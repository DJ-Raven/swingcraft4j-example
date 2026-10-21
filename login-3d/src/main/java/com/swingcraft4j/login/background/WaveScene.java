package com.swingcraft4j.login.background;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/**
 * A grid floating like the sea, rolling with waves.
 */
public class WaveScene extends Scene {

    private static final int COLS = 38;
    private static final int ROWS = 22;
    private static final double CAMERA = 4;
    private static final double LIFT_REACH = 170;

    private final double[] screenX = new double[COLS * ROWS];
    private final double[] screenY = new double[COLS * ROWS];
    private final double[] depth = new double[COLS * ROWS];
    // how much the mouse lifts each point, from 0 to 1
    private final double[] lift = new double[COLS * ROWS];
    private final double[] out = new double[3];

    public WaveScene() {
        super("Wave");
    }

    @Override
    public void paint(Graphics2D g2, View view) {
        double scale = Math.max(view.width, view.height) * 0.27;
        double cx = view.width / 2.0 - view.mouseX * 30;
        double cy = view.height * 0.58;
        paintGlow(g2, cx, cy, scale * 1.6, view.accent, 50);

        // seen from above, swaying with the spin
        view.setAngles(0.5 + view.tiltX * 0.5, Math.sin(view.spinY * 0.6) * 0.45 + view.tiltY * 0.6);
        double t = view.frame;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int i = r * COLS + c;
                double x = (c / (COLS - 1.0) - 0.5) * 4.4;
                double z = (r / (ROWS - 1.0) - 0.5) * 2.6;
                double y = 0.16 * Math.sin(x * 1.8 + t * 0.035) * Math.cos(z * 2.2 + t * 0.025)
                        + 0.07 * Math.sin((x + z) * 3 - t * 0.05);
                view.project(x, y, z, cx, cy, scale, CAMERA, out);
                double distance = view.mouseDistance(out[0], out[1]);
                lift[i] = 0;
                if (distance < LIFT_REACH) {
                    // up is negative y
                    lift[i] = 1 - distance / LIFT_REACH;
                    view.project(x, y - 0.45 * lift[i] * lift[i], z, cx, cy, scale, CAMERA, out);
                }
                screenX[i] = out[0];
                screenY[i] = out[1];
                depth[i] = (1.4 - out[2]) / 2.8;
            }
        }

        g2.setStroke(new BasicStroke(1f));
        Line2D.Double line = new Line2D.Double();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int i = r * COLS + c;
                if (c + 1 < COLS) {
                    link(g2, view, line, i, i + 1);
                }
                if (r + 1 < ROWS) {
                    link(g2, view, line, i, i + COLS);
                }
            }
        }
        for (int i = 0; i < lift.length; i++) {
            if (lift[i] > 0) {
                double size = 1.5 + lift[i] * 4;
                g2.setColor(view.node(depth[i] + lift[i] * 0.5));
                g2.fill(new Ellipse2D.Double(screenX[i] - size / 2, screenY[i] - size / 2, size, size));
            }
        }
    }

    private void link(Graphics2D g2, View view, Line2D.Double line, int a, int b) {
        double d = (depth[a] + depth[b]) / 2;
        double l = Math.max(lift[a], lift[b]);
        g2.setColor(l > 0 ? view.node(d * 0.6 + l * 0.4) : view.edge(d));
        line.setLine(screenX[a], screenY[a], screenX[b], screenY[b]);
        g2.draw(line);
    }
}
