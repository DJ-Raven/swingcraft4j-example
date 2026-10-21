package com.swingcraft4j.login.background;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A globe of points spread on a sphere, each linked to its nearest ones.
 */
public class GlobeScene extends Scene {

    private static final int POINTS = 170;
    private static final int NEIGHBORS = 3;
    private static final double CAMERA = 3;

    private final double[][] points = new double[POINTS][];
    private final int[][] edges;
    private final double[] screenX = new double[POINTS];
    private final double[] screenY = new double[POINTS];
    private final double[] depth = new double[POINTS];
    private final double[] out = new double[3];

    public GlobeScene() {
        super("Globe");
        for (int i = 0; i < POINTS; i++) {
            // spread the points evenly on the sphere (a Fibonacci sphere)
            double y = 1 - i / (POINTS - 1.0) * 2;
            double r = Math.sqrt(1 - y * y);
            double theta = Math.PI * (3 - Math.sqrt(5)) * i;
            points[i] = new double[]{Math.cos(theta) * r, y, Math.sin(theta) * r};
        }
        edges = nearestEdges();
    }

    /**
     * Links each point to its nearest points, each pair once.
     */
    private int[][] nearestEdges() {
        boolean[][] linked = new boolean[POINTS][POINTS];
        List<int[]> list = new ArrayList<>();
        for (int i = 0; i < POINTS; i++) {
            int[] nearest = new int[NEIGHBORS];
            double[] best = new double[NEIGHBORS];
            Arrays.fill(best, Double.MAX_VALUE);
            for (int j = 0; j < POINTS; j++) {
                if (i == j) {
                    continue;
                }
                double dx = points[i][0] - points[j][0];
                double dy = points[i][1] - points[j][1];
                double dz = points[i][2] - points[j][2];
                double d = dx * dx + dy * dy + dz * dz;
                for (int k = 0; k < NEIGHBORS; k++) {
                    if (d < best[k]) {
                        // shift the farther ones down to keep the list sorted
                        for (int m = NEIGHBORS - 1; m > k; m--) {
                            best[m] = best[m - 1];
                            nearest[m] = nearest[m - 1];
                        }
                        best[k] = d;
                        nearest[k] = j;
                        break;
                    }
                }
            }
            for (int j : nearest) {
                if (!linked[i][j]) {
                    linked[i][j] = linked[j][i] = true;
                    list.add(new int[]{i, j});
                }
            }
        }
        return list.toArray(new int[0][]);
    }

    @Override
    public void paint(Graphics2D g2, View view) {
        double radius = Math.min(view.width, view.height) * 0.42;
        // the globe floats a little against the mouse
        double cx = view.width / 2.0 - view.mouseX * 40;
        double cy = view.height / 2.0 - view.mouseY * 40;
        paintGlow(g2, cx, cy, radius * 1.4, view.accent, 70);

        view.setAngles(view.spinX + view.tiltX, view.spinY + view.tiltY);
        for (int i = 0; i < POINTS; i++) {
            double[] p = points[i];
            view.project(p[0], p[1], p[2], cx, cy, radius, CAMERA, out);
            screenX[i] = out[0];
            screenY[i] = out[1];
            depth[i] = (1 - out[2]) / 2;
        }

        g2.setStroke(new BasicStroke(1f));
        Line2D.Double line = new Line2D.Double();
        for (int[] edge : edges) {
            g2.setColor(view.edge((depth[edge[0]] + depth[edge[1]]) / 2));
            line.setLine(screenX[edge[0]], screenY[edge[0]], screenX[edge[1]], screenY[edge[1]]);
            g2.draw(line);
        }
        for (int i = 0; i < POINTS; i++) {
            double size = 1.5 + depth[i] * 3 + linkMouse(g2, view, screenX[i], screenY[i], depth[i], 150, line) * 4;
            g2.setColor(view.node(depth[i]));
            g2.fill(new Ellipse2D.Double(screenX[i] - size / 2, screenY[i] - size / 2, size, size));
        }
    }
}
