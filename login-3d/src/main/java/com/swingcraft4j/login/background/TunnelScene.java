package com.swingcraft4j.login.background;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/**
 * A flight through a twisting tunnel of rings.
 */
public class TunnelScene extends Scene {

    private static final int RINGS = 26;
    private static final int SIDES = 20;
    private static final double SPACING = 0.32;
    private static final double NEAR = 0.25;
    private static final double FAR = NEAR + (RINGS + 1) * SPACING;

    private final double[][] screenX = new double[RINGS][SIDES];
    private final double[][] screenY = new double[RINGS][SIDES];
    private final double[] depth = new double[RINGS];

    public TunnelScene() {
        super("Tunnel");
    }

    // the bend of the tunnel at the ring
    private static double curveX(double ring) {
        return Math.sin(ring * 0.21) * 0.6;
    }

    private static double curveY(double ring) {
        return Math.cos(ring * 0.15) * 0.4;
    }

    @Override
    public void paint(Graphics2D g2, View view) {
        double focal = Math.min(view.width, view.height) * 0.55;
        double cx = view.width / 2.0, cy = view.height / 2.0;

        // the spin moves the camera through the tunnel, so a drag flies faster
        double travel = Math.abs(view.spinY) * 8 + Math.abs(view.spinX) * 4;
        double phase = travel % SPACING;
        double base = Math.floor(travel / SPACING);
        double camera = base + phase / SPACING;
        for (int i = 0; i < RINGS; i++) {
            double z = NEAR + SPACING + i * SPACING - phase;
            double ring = base + i;
            // the bend seen from the camera, which flies along the middle of the tunnel
            double ox = curveX(ring) - curveX(camera) + view.mouseX * 1.8 * (z / FAR);
            double oy = curveY(ring) - curveY(camera) + view.mouseY * 1.8 * (z / FAR);
            double twist = ring * 0.18;
            for (int k = 0; k < SIDES; k++) {
                double a = Math.PI * 2 * k / SIDES + twist;
                screenX[i][k] = cx + (Math.cos(a) + ox) / z * focal;
                screenY[i][k] = cy + (Math.sin(a) + oy) / z * focal;
            }
            depth[i] = 1 - (z - NEAR) / (FAR - NEAR);
        }

        // the light at the end of the tunnel
        double endX = 0, endY = 0;
        for (int k = 0; k < SIDES; k++) {
            endX += screenX[RINGS - 1][k] / SIDES;
            endY += screenY[RINGS - 1][k] / SIDES;
        }
        paintGlow(g2, endX, endY, focal * 0.7, view.accent, 80);
        paintGlow(g2, endX, endY, focal * 0.12, view.glow, view.dark ? 120 : 70);

        g2.setStroke(new BasicStroke(1f));
        Line2D.Double line = new Line2D.Double();
        for (int i = RINGS - 1; i >= 0; i--) {
            for (int k = 0; k < SIDES; k++) {
                int next = (k + 1) % SIDES;
                g2.setColor(view.edge(depth[i]));
                line.setLine(screenX[i][k], screenY[i][k], screenX[i][next], screenY[i][next]);
                g2.draw(line);
                if (i + 1 < RINGS) {
                    g2.setColor(view.edge((depth[i] + depth[i + 1]) / 2 * 0.8));
                    line.setLine(screenX[i][k], screenY[i][k], screenX[i + 1][k], screenY[i + 1][k]);
                    g2.draw(line);
                }
            }
        }
        for (int i = RINGS - 1; i >= 0; i--) {
            for (int k = 0; k < SIDES; k++) {
                double x = screenX[i][k], y = screenY[i][k];
                if (x < -10 || y < -10 || x > view.width + 10 || y > view.height + 10) {
                    continue;
                }
                double size = 1 + depth[i] * 3 + linkMouse(g2, view, x, y, depth[i], 120, line) * 4;
                g2.setColor(view.node(depth[i]));
                g2.fill(new Ellipse2D.Double(x - size / 2, y - size / 2, size, size));
            }
        }
    }
}
