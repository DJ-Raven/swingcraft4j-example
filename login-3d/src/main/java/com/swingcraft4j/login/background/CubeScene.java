package com.swingcraft4j.login.background;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.Arrays;
import java.util.Random;

/**
 * Wire cubes floating up at different depths.
 */
public class CubeScene extends Scene {

    private static final int COUNT = 26;
    private static final double[][] CORNERS = {
        {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1},
        {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}
    };
    private static final int[][] EDGES = {
        {0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };
    private static final int[][] FACES = {
        {0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 5, 4}, {2, 3, 7, 6}, {1, 2, 6, 5}, {0, 3, 7, 4}
    };

    private final Cube[] cubes = new Cube[COUNT];
    private final Random random = new Random(3);

    private static class Cube {

        double x, y, z, size;
        double angleX, angleY, speedX, speedY;
        // how lit by the mouse, from 0 to 1
        double glow;
    }

    public CubeScene() {
        super("Cubes");
        for (int i = 0; i < COUNT; i++) {
            Cube cube = new Cube();
            cube.y = random.nextDouble() * 5.6 - 2.8;
            place(cube);
            cubes[i] = cube;
        }
    }

    private void place(Cube cube) {
        cube.x = random.nextDouble() * 7 - 3.5;
        cube.z = 1.5 + random.nextDouble() * 5.5;
        cube.size = 0.22 + random.nextDouble() * 0.35;
        cube.speedX = (random.nextDouble() - 0.5) * 0.02;
        cube.speedY = (random.nextDouble() - 0.5) * 0.02;
    }

    @Override
    public void tick(View view) {
        for (Cube cube : cubes) {
            cube.y -= 0.004;
            if (cube.y < -2.8) {
                cube.y = 2.8;
                place(cube);
            }
            cube.angleX += cube.speedX * (1 + cube.glow * 6);
            cube.angleY += cube.speedY * (1 + cube.glow * 6);
        }
    }

    @Override
    public void paint(Graphics2D g2, View view) {
        double focal = Math.min(view.width, view.height) * 0.9;
        double cx = view.width / 2.0, cy = view.height / 2.0;
        paintGlow(g2, cx, cy, focal * 0.8, view.accent, 45);

        // far cubes first, so the near ones are drawn over them
        Cube[] sorted = cubes.clone();
        Arrays.sort(sorted, (a, b) -> Double.compare(b.z, a.z));
        double[] sx = new double[8], sy = new double[8];
        Line2D.Double line = new Line2D.Double();
        for (Cube cube : sorted) {
            // the near cubes move more with the mouse
            double px = cube.x - view.mouseX * 1.2;
            double py = cube.y - view.mouseY * 1.2;
            double ccx = cx + px / cube.z * focal, ccy = cy + py / cube.z * focal;
            double reach = 50 + cube.size / cube.z * focal;
            double target = view.mouseDistance(ccx, ccy) < reach ? 1 : 0;
            cube.glow += (target - cube.glow) * 0.08;

            double ax = cube.angleX + view.tiltX, ay = cube.angleY + view.tiltY;
            double cosX = Math.cos(ax), sinX = Math.sin(ax), cosY = Math.cos(ay), sinY = Math.sin(ay);
            for (int i = 0; i < 8; i++) {
                double[] c = CORNERS[i];
                double x = c[0] * cosY + c[2] * sinY;
                double z = -c[0] * sinY + c[2] * cosY;
                double y = c[1] * cosX - z * sinX;
                z = c[1] * sinX + z * cosX;
                double wz = cube.z + z * cube.size;
                sx[i] = cx + (px + x * cube.size) / wz * focal;
                sy[i] = cy + (py + y * cube.size) / wz * focal;
            }
            double depth = 1 - (cube.z - 1.5) / 5.5;

            if (cube.glow > 0.02) {
                g2.setColor(alpha(view.accent, (int) (cube.glow * 40)));
                for (int[] face : FACES) {
                    Path2D.Double path = new Path2D.Double();
                    path.moveTo(sx[face[0]], sy[face[0]]);
                    for (int k = 1; k < 4; k++) {
                        path.lineTo(sx[face[k]], sy[face[k]]);
                    }
                    path.closePath();
                    g2.fill(path);
                }
            }
            g2.setStroke(new BasicStroke((float) (1 + cube.glow * 1.2)));
            g2.setColor(view.node(depth * 0.8 + cube.glow * 0.4));
            for (int[] edge : EDGES) {
                line.setLine(sx[edge[0]], sy[edge[0]], sx[edge[1]], sy[edge[1]]);
                g2.draw(line);
            }
        }
    }
}
