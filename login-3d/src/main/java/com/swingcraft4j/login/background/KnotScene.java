package com.swingcraft4j.login.background;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/**
 * A torus knot: a wire tube tied around itself, turning in the space.
 */
public class KnotScene extends Scene {

    private static final int SEGMENTS = 150;
    private static final int SIDES = 7;
    private static final double TUBE = 0.12;
    private static final double CAMERA = 3;

    private final double[][] points = new double[SEGMENTS * SIDES][3];
    private final double[] screenX = new double[SEGMENTS * SIDES];
    private final double[] screenY = new double[SEGMENTS * SIDES];
    private final double[] depth = new double[SEGMENTS * SIDES];
    private final double[] out = new double[3];

    public KnotScene() {
        super("Knot");
        double e = 0.001;
        for (int i = 0; i < SEGMENTS; i++) {
            double t = Math.PI * 2 * i / SEGMENTS;
            double[] p = knot(t);
            double[] before = knot(t - e), after = knot(t + e);
            // the tangent, and two directions across it to draw the ring of the tube
            double[] tangent = normalize(new double[]{after[0] - before[0], after[1] - before[1], after[2] - before[2]});
            double[] normal = normalize(cross(tangent, new double[]{0, 0, 1}));
            double[] binormal = cross(tangent, normal);
            for (int k = 0; k < SIDES; k++) {
                double a = Math.PI * 2 * k / SIDES;
                double cos = Math.cos(a) * TUBE, sin = Math.sin(a) * TUBE;
                double[] q = points[i * SIDES + k];
                for (int m = 0; m < 3; m++) {
                    q[m] = p[m] + normal[m] * cos + binormal[m] * sin;
                }
            }
        }
    }

    // the (2, 3) torus knot, in a sphere of radius about 1
    private static double[] knot(double t) {
        double r = (Math.cos(3 * t) + 2.2) / 3.2;
        return new double[]{r * Math.cos(2 * t), r * Math.sin(2 * t), -Math.sin(3 * t) * 0.38};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[]{
            a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalize(double[] v) {
        double length = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[]{v[0] / length, v[1] / length, v[2] / length};
    }

    @Override
    public void paint(Graphics2D g2, View view) {
        double radius = Math.min(view.width, view.height) * 0.42;
        double cx = view.width / 2.0 - view.mouseX * 40;
        double cy = view.height / 2.0 - view.mouseY * 40;
        paintGlow(g2, cx, cy, radius * 1.3, view.accent, 60);

        view.setAngles(view.spinX + view.tiltX + 0.4, view.spinY * 1.2 + view.tiltY);
        for (int i = 0; i < points.length; i++) {
            double[] p = points[i];
            view.project(p[0], p[1], p[2], cx, cy, radius, CAMERA, out);
            screenX[i] = out[0];
            screenY[i] = out[1];
            depth[i] = (1 - out[2]) / 2;
        }

        g2.setStroke(new BasicStroke(1f));
        Line2D.Double line = new Line2D.Double();
        for (int i = 0; i < SEGMENTS; i++) {
            int next = (i + 1) % SEGMENTS;
            for (int k = 0; k < SIDES; k++) {
                int a = i * SIDES + k;
                link(g2, view, line, a, i * SIDES + (k + 1) % SIDES);
                link(g2, view, line, a, next * SIDES + k);
            }
        }
        for (int i = 0; i < points.length; i += 2) {
            double power = linkMouse(g2, view, screenX[i], screenY[i], depth[i], 130, line);
            if (power > 0) {
                double size = 2 + power * 4;
                g2.setColor(view.node(depth[i]));
                g2.fill(new Ellipse2D.Double(screenX[i] - size / 2, screenY[i] - size / 2, size, size));
            }
        }
    }

    private void link(Graphics2D g2, View view, Line2D.Double line, int a, int b) {
        g2.setColor(view.edge((depth[a] + depth[b]) / 2));
        line.setLine(screenX[a], screenY[a], screenX[b], screenY[b]);
        g2.draw(line);
    }
}
