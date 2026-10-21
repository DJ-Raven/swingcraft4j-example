package com.swingcraft4j.login.background;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Random;

/**
 * A spiral galaxy of stars turning around its bright core.
 */
public class GalaxyScene extends Scene {

    private static final int COUNT = 2200;
    private static final int ARMS = 3;
    private static final double CAMERA = 3;
    private static final double PUSH_REACH = 140;

    // x, y, z and the distance from the core from 0 to 1
    private final double[][] stars = new double[COUNT][4];
    private final double[] out = new double[3];

    public GalaxyScene() {
        super("Galaxy");
        Random random = new Random(7);
        for (int i = 0; i < COUNT; i++) {
            double r;
            double angle;
            if (i % 5 == 0) {
                // the core
                r = Math.pow(random.nextDouble(), 2) * 0.25;
                angle = random.nextDouble() * Math.PI * 2;
            } else {
                r = Math.min(1, Math.abs(random.nextDouble() + random.nextGaussian() * 0.05));
                angle = (i % ARMS) * Math.PI * 2 / ARMS + r * 4 + random.nextGaussian() * (0.18 + 0.3 * (1 - r));
            }
            double[] star = stars[i];
            star[0] = Math.cos(angle) * r;
            star[1] = random.nextGaussian() * 0.04 * (1.2 - r);
            star[2] = Math.sin(angle) * r;
            star[3] = r;
        }
    }

    @Override
    public void paint(Graphics2D g2, View view) {
        double radius = Math.max(view.width, view.height) * 0.34;
        double cx = view.width / 2.0 - view.mouseX * 50;
        double cy = view.height / 2.0 - view.mouseY * 50;
        paintGlow(g2, cx, cy, radius * 0.9, view.accent, 70);
        paintGlow(g2, cx, cy, radius * 0.18, view.glow, view.dark ? 150 : 90);

        // seen from above, turning around its core
        view.setAngles(1.05 + view.tiltX * 0.6, view.spinY * 0.8 + view.tiltY * 0.3);
        Rectangle2D.Double dot = new Rectangle2D.Double();
        for (double[] star : stars) {
            view.project(star[0], star[1], star[2], cx, cy, radius, CAMERA, out);
            double x = out[0], y = out[1];
            double depth = (1 - out[2]) / 2;
            double distance = view.mouseDistance(x, y);
            if (distance < PUSH_REACH && distance > 0) {
                double push = Math.pow(1 - distance / PUSH_REACH, 2) * 35;
                x += (x - view.mouse.x) / distance * push;
                y += (y - view.mouse.y) / distance * push;
            }
            double size = 1 + depth * 1.6 + (star[3] < 0.25 ? 0.6 : 0);
            // the core in the near color, the arms toward the far one
            g2.setColor(view.node(0.4 + 0.6 * (1 - star[3]) * (0.6 + 0.4 * depth)));
            dot.setRect(x - size / 2, y - size / 2, size, size);
            g2.fill(dot);
        }
    }
}
