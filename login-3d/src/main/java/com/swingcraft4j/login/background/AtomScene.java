package com.swingcraft4j.login.background;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.Arrays;
import java.util.Random;

/**
 * An atom: a nucleus of shaded spheres with electrons racing on three tilted orbits.
 */
public class AtomScene extends Scene {

    private static final int ORBITS = 3;
    private static final int SEGMENTS = 90;
    private static final int NUCLEONS = 14;
    private static final double ORBIT_TILT = 1.2;
    private static final double CAMERA = 3;

    private final double[][] nucleons = new double[NUCLEONS][3];
    private final double[] out = new double[3];
    private final double[] point = new double[3];
    // how far the electrons went, and the boost the mouse gives them from 0 to 1
    private double phase;
    private double boost;

    public AtomScene() {
        super("Atom");
        Random random = new Random(11);
        for (double[] n : nucleons) {
            // in a small ball, a little apart from each other
            double r = 0.06 + random.nextDouble() * 0.09;
            double theta = random.nextDouble() * Math.PI * 2;
            double y = random.nextDouble() * 2 - 1;
            double s = Math.sqrt(1 - y * y);
            n[0] = Math.cos(theta) * s * r;
            n[1] = y * r;
            n[2] = Math.sin(theta) * s * r;
        }
    }

    @Override
    public void tick(View view) {
        phase += 0.022 * (1 + boost * 3);
    }

    /**
     * The point of the orbit at the angle, in the space of the scene.
     */
    private void orbitPoint(int orbit, double angle, double[] p) {
        double x = Math.cos(angle), y = Math.sin(angle), z = 0;
        // tilt the circle, then turn it around Y to its place
        double cosT = Math.cos(ORBIT_TILT), sinT = Math.sin(ORBIT_TILT);
        double y1 = y * cosT - z * sinT;
        double z1 = y * sinT + z * cosT;
        double yaw = orbit * Math.PI / ORBITS;
        p[0] = x * Math.cos(yaw) + z1 * Math.sin(yaw);
        p[1] = y1;
        p[2] = -x * Math.sin(yaw) + z1 * Math.cos(yaw);
    }

    @Override
    public void paint(Graphics2D g2, View view) {
        double scale = Math.min(view.width, view.height) * 0.40;
        double cx = view.width / 2.0 - view.mouseX * 40;
        double cy = view.height / 2.0 - view.mouseY * 40;
        double distance = view.mouseDistance(cx, cy);
        boost += ((distance < 260 ? 1 - distance / 260 : 0) - boost) * 0.05;
        paintGlow(g2, cx, cy, scale * 1.3, view.accent, 60);
        paintGlow(g2, cx, cy, scale * 0.35, view.glow, view.dark ? 70 + (int) (boost * 60) : 50);

        view.setAngles(view.tiltX + 0.25, view.spinY * 0.6 + view.tiltY);
        Line2D.Double line = new Line2D.Double();
        g2.setStroke(new BasicStroke(1.4f));
        for (int k = 0; k < ORBITS; k++) {
            double px = 0, py = 0, pd = 0;
            for (int s = 0; s <= SEGMENTS; s++) {
                orbitPoint(k, Math.PI * 2 * s / SEGMENTS, point);
                view.project(point[0], point[1], point[2], cx, cy, scale, CAMERA, out);
                double d = (1 - out[2]) / 2;
                if (s > 0) {
                    g2.setColor(view.edge((d + pd) / 2 + 0.15));
                    line.setLine(px, py, out[0], out[1]);
                    g2.draw(line);
                }
                px = out[0];
                py = out[1];
                pd = d;
            }
        }

        paintNucleus(g2, view, cx, cy, scale);

        for (int k = 0; k < ORBITS; k++) {
            for (int e = 0; e < 2; e++) {
                double angle = phase * (1 + k * 0.17) + e * Math.PI + k;
                // the trail, fading behind the electron
                for (int j = 14; j >= 1; j--) {
                    orbitPoint(k, angle - j * 0.05, point);
                    view.project(point[0], point[1], point[2], cx, cy, scale, CAMERA, out);
                    double size = 4 * (1 - j / 15.0) + 1;
                    g2.setColor(alpha(view.light, (int) (160 * (1 - j / 15.0))));
                    g2.fill(new Ellipse2D.Double(out[0] - size / 2, out[1] - size / 2, size, size));
                }
                orbitPoint(k, angle, point);
                view.project(point[0], point[1], point[2], cx, cy, scale, CAMERA, out);
                double d = (1 - out[2]) / 2;
                paintGlow(g2, out[0], out[1], 16 + d * 8, view.glow, 140);
                double size = 5 + d * 4;
                g2.setColor(view.dark ? Color.WHITE : view.light);
                g2.fill(new Ellipse2D.Double(out[0] - size / 2, out[1] - size / 2, size, size));
            }
        }
    }

    private void paintNucleus(Graphics2D g2, View view, double cx, double cy, double scale) {
        double[][] projected = new double[NUCLEONS][];
        for (int i = 0; i < NUCLEONS; i++) {
            double[] n = nucleons[i];
            view.project(n[0], n[1], n[2], cx, cy, scale, CAMERA, out);
            projected[i] = new double[]{out[0], out[1], out[2], i};
        }
        // far ones first
        Arrays.sort(projected, (a, b) -> Double.compare(b[2], a[2]));
        for (double[] p : projected) {
            double f = CAMERA / (CAMERA + p[2]);
            double r = 0.075 * scale * f;
            Color body = ((int) p[3]) % 2 == 0 ? view.accent : view.near;
            // lit from the top left
            g2.setPaint(new RadialGradientPaint((float) (p[0] - r * 0.35), (float) (p[1] - r * 0.35), (float) (r * 1.3),
                    new float[]{0f, 0.45f, 1f},
                    new Color[]{Palette.mix(body, Color.WHITE, 0.6), body, Palette.mix(body, Color.BLACK, 0.45)}));
            g2.fill(new Ellipse2D.Double(p[0] - r, p[1] - r, r * 2, r * 2));
        }
    }
}
