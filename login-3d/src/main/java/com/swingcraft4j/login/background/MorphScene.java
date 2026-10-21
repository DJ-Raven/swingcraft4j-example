package com.swingcraft4j.login.background;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Random;

/**
 * A cloud of particles flowing from one shape into the next: a sphere, a cube, a torus and a spring.
 */
public class MorphScene extends Scene {

    private static final int COUNT = 900;
    private static final int HOLD = 260;
    private static final double CAMERA = 3;
    private static final double PUSH_REACH = 130;

    private final double[][][] shapes = new double[4][COUNT][3];
    private final double[][] particles = new double[COUNT][3];
    // each particle flows at its own speed, so the shapes melt into each other
    private final double[] speeds = new double[COUNT];
    private final double[] out = new double[3];
    private int shape;
    private int frames;

    public MorphScene() {
        super("Morph");
        Random random = new Random(5);
        for (int i = 0; i < COUNT; i++) {
            // a sphere (a Fibonacci sphere)
            double y = 1 - i / (COUNT - 1.0) * 2;
            double r = Math.sqrt(1 - y * y);
            double theta = Math.PI * (3 - Math.sqrt(5)) * i;
            set(shapes[0][i], Math.cos(theta) * r, y, Math.sin(theta) * r);

            // the faces of a cube
            double u = random.nextDouble() * 1.5 - 0.75, v = random.nextDouble() * 1.5 - 0.75;
            double side = i % 2 == 0 ? 0.75 : -0.75;
            switch (i % 6 / 2) {
                case 0:
                    set(shapes[1][i], side, u, v);
                    break;
                case 1:
                    set(shapes[1][i], u, side, v);
                    break;
                default:
                    set(shapes[1][i], u, v, side);
                    break;
            }

            // a torus, 45 around by 20 across
            double a = Math.PI * 2 * (i % 45) / 45, b = Math.PI * 2 * (i / 45) / 20.0;
            double ring = 0.72 + Math.cos(b) * 0.3;
            set(shapes[2][i], Math.cos(a) * ring, Math.sin(b) * 0.3, Math.sin(a) * ring);

            // a spring
            double t = i / (double) COUNT;
            double spin = t * Math.PI * 12;
            set(shapes[3][i], Math.cos(spin) * 0.7 + random.nextGaussian() * 0.03,
                    (t - 0.5) * 2, Math.sin(spin) * 0.7 + random.nextGaussian() * 0.03);

            System.arraycopy(shapes[0][i], 0, particles[i], 0, 3);
            speeds[i] = 0.025 + random.nextDouble() * 0.05;
        }
    }

    private static void set(double[] p, double x, double y, double z) {
        p[0] = x;
        p[1] = y;
        p[2] = z;
    }

    @Override
    public void tick(View view) {
        if (++frames % HOLD == 0) {
            shape = (shape + 1) % shapes.length;
        }
        double[][] target = shapes[shape];
        for (int i = 0; i < COUNT; i++) {
            for (int m = 0; m < 3; m++) {
                particles[i][m] += (target[i][m] - particles[i][m]) * speeds[i];
            }
        }
    }

    @Override
    public void paint(Graphics2D g2, View view) {
        double scale = Math.min(view.width, view.height) * 0.38;
        double cx = view.width / 2.0 - view.mouseX * 40;
        double cy = view.height / 2.0 - view.mouseY * 40;
        paintGlow(g2, cx, cy, scale * 1.4, view.accent, 60);

        view.setAngles(view.spinX * 0.5 + view.tiltX + 0.3, view.spinY * 1.2 + view.tiltY);
        Ellipse2D.Double dot = new Ellipse2D.Double();
        // the far particles are too small to look round: squares draw much faster
        Rectangle2D.Double speck = new Rectangle2D.Double();
        for (double[] p : particles) {
            view.project(p[0], p[1], p[2], cx, cy, scale, CAMERA, out);
            double x = out[0], y = out[1];
            double depth = (1 - out[2]) / 2;
            double distance = view.mouseDistance(x, y);
            if (distance < PUSH_REACH && distance > 0) {
                double push = Math.pow(1 - distance / PUSH_REACH, 2) * 30;
                x += (x - view.mouse.x) / distance * push;
                y += (y - view.mouse.y) / distance * push;
            }
            double size = 1.4 + depth * 2.4;
            g2.setColor(view.node(depth));
            if (size < 2.8) {
                speck.setRect(x - size / 2, y - size / 2, size, size);
                g2.fill(speck);
            } else {
                dot.setFrame(x - size / 2, y - size / 2, size, size);
                g2.fill(dot);
            }
        }
    }
}
