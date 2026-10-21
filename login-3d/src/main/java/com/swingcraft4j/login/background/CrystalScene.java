package com.swingcraft4j.login.background;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A solid low poly crystal with flat shaded faces, and three small crystals circling it.
 */
public class CrystalScene extends Scene {

    private static final double CAMERA = 4;
    private static final double[][] ICOSAHEDRON;
    private static final int[][] ICOSAHEDRON_FACES = {
        {0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11},
        {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6}, {7, 1, 8},
        {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9},
        {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1}
    };

    static {
        double t = (1 + Math.sqrt(5)) / 2;
        double[][] v = {
            {-1, t, 0}, {1, t, 0}, {-1, -t, 0}, {1, -t, 0},
            {0, -1, t}, {0, 1, t}, {0, -1, -t}, {0, 1, -t},
            {t, 0, -1}, {t, 0, 1}, {-t, 0, -1}, {-t, 0, 1}
        };
        for (double[] p : v) {
            double length = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
            for (int m = 0; m < 3; m++) {
                p[m] /= length;
            }
        }
        ICOSAHEDRON = v;
    }

    private static class Mesh {

        final double[][] vertices;
        final int[][] faces;
        double size = 1;

        Mesh(double[][] vertices, int[][] faces) {
            this.vertices = vertices;
            this.faces = faces;
        }
        double x, y, z;
        double angleX, angleY;
    }

    // a face to draw: its corners on screen, its depth and its color
    private static class Face {

        final double[] xs = new double[3], ys = new double[3];
        double depth;
        Color color;
    }

    private final Mesh crystal;
    private final Mesh[] moons = new Mesh[3];

    public CrystalScene() {
        super("Crystal");
        crystal = subdivided(new Random(9));
        for (int i = 0; i < moons.length; i++) {
            Mesh moon = new Mesh(ICOSAHEDRON, ICOSAHEDRON_FACES);
            moon.size = 0.16;
            moons[i] = moon;
        }
    }

    /**
     * The icosahedron with each face cut in four, its corners pushed out a little at random for the look of a cut stone.
     */
    private static Mesh subdivided(Random random) {
        List<double[]> vertices = new ArrayList<>();
        for (double[] p : ICOSAHEDRON) {
            vertices.add(p.clone());
        }
        // the middle of each edge, made once and shared by its two faces
        Map<Long, Integer> middles = new HashMap<>();
        List<int[]> faces = new ArrayList<>();
        for (int[] f : ICOSAHEDRON_FACES) {
            int a = middle(vertices, middles, f[0], f[1]);
            int b = middle(vertices, middles, f[1], f[2]);
            int c = middle(vertices, middles, f[2], f[0]);
            faces.add(new int[]{f[0], a, c});
            faces.add(new int[]{f[1], b, a});
            faces.add(new int[]{f[2], c, b});
            faces.add(new int[]{a, b, c});
        }
        for (double[] p : vertices) {
            double r = 1 + random.nextDouble() * 0.14;
            for (int m = 0; m < 3; m++) {
                p[m] *= r;
            }
        }
        return new Mesh(vertices.toArray(new double[0][]), faces.toArray(new int[0][]));
    }

    private static int middle(List<double[]> vertices, Map<Long, Integer> middles, int a, int b) {
        long key = Math.min(a, b) * 1000L + Math.max(a, b);
        Integer index = middles.get(key);
        if (index == null) {
            double[] p = vertices.get(a), q = vertices.get(b);
            double[] m = {(p[0] + q[0]) / 2, (p[1] + q[1]) / 2, (p[2] + q[2]) / 2};
            double length = Math.sqrt(m[0] * m[0] + m[1] * m[1] + m[2] * m[2]);
            for (int i = 0; i < 3; i++) {
                m[i] /= length;
            }
            vertices.add(m);
            index = vertices.size() - 1;
            middles.put(key, index);
        }
        return index;
    }

    @Override
    public void tick(View view) {
        for (int i = 0; i < moons.length; i++) {
            Mesh moon = moons[i];
            double angle = view.frame * 0.012 + i * Math.PI * 2 / moons.length;
            moon.x = Math.cos(angle) * 1.65;
            moon.z = Math.sin(angle) * 1.65;
            moon.y = Math.sin(angle * 2) * 0.25 + (i - 1) * 0.35;
            moon.angleX += 0.02;
            moon.angleY += 0.03;
        }
    }

    @Override
    public void paint(Graphics2D g2, View view) {
        double scale = Math.min(view.width, view.height) * 0.30;
        double cx = view.width / 2.0 - view.mouseX * 40;
        double cy = view.height / 2.0 - view.mouseY * 40;
        paintGlow(g2, cx, cy, scale * 2.2, view.accent, 70);

        view.setAngles(view.spinX * 0.6 + view.tiltX * 0.5, view.spinY * 0.8 + view.tiltY * 0.5);
        // the light comes from the mouse, in front of the scene
        double lx = view.mouseX * 3, ly = view.mouseY * 3, lz = -1.4;
        double length = Math.sqrt(lx * lx + ly * ly + lz * lz);
        double[] light = {lx / length, ly / length, lz / length};

        List<Face> faces = new ArrayList<>();
        addFaces(faces, crystal, view, light, cx, cy, scale);
        for (Mesh moon : moons) {
            addFaces(faces, moon, view, light, cx, cy, scale);
        }
        // far faces first, so the near ones cover them
        faces.sort((a, b) -> Double.compare(b.depth, a.depth));

        // the shading tells the faces apart; outlining them too would halve the frame rate
        Path2D.Double path = new Path2D.Double();
        for (Face face : faces) {
            path.reset();
            path.moveTo(face.xs[0], face.ys[0]);
            path.lineTo(face.xs[1], face.ys[1]);
            path.lineTo(face.xs[2], face.ys[2]);
            path.closePath();
            g2.setColor(face.color);
            g2.fill(path);
        }
    }

    private void addFaces(List<Face> faces, Mesh mesh, View view, double[] light, double cx, double cy, double scale) {
        double cosX = Math.cos(mesh.angleX), sinX = Math.sin(mesh.angleX);
        double cosY = Math.cos(mesh.angleY), sinY = Math.sin(mesh.angleY);
        double[][] turned = new double[mesh.vertices.length][3];
        for (int i = 0; i < mesh.vertices.length; i++) {
            double[] p = mesh.vertices[i];
            // its own turn, its place, then the turn of the view
            double x = p[0] * cosY + p[2] * sinY;
            double z = -p[0] * sinY + p[2] * cosY;
            double y = p[1] * cosX - z * sinX;
            z = p[1] * sinX + z * cosX;
            view.rotate(mesh.x + x * mesh.size, mesh.y + y * mesh.size, mesh.z + z * mesh.size, turned[i]);
        }
        double[] center = new double[3];
        view.rotate(mesh.x, mesh.y, mesh.z, center);
        Color shadow = view.dark ? Palette.mix(view.far, Color.BLACK, 0.35) : view.near;
        Color lit = view.dark ? view.near : view.far;
        for (int[] f : mesh.faces) {
            double[] a = turned[f[0]], b = turned[f[1]], c = turned[f[2]];
            double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
            double vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
            double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
            nx /= nl;
            ny /= nl;
            nz /= nl;
            double mx = (a[0] + b[0] + c[0]) / 3, my = (a[1] + b[1] + c[1]) / 3, mz = (a[2] + b[2] + c[2]) / 3;
            // make the normal point out of the mesh, away from its center
            if (nx * (mx - center[0]) + ny * (my - center[1]) + nz * (mz - center[2]) < 0) {
                nx = -nx;
                ny = -ny;
                nz = -nz;
            }
            // seen only when it looks toward the camera, at z = -CAMERA
            if (nx * mx + ny * my + nz * (mz + CAMERA) >= 0) {
                continue;
            }
            double intensity = Math.max(0, nx * light[0] + ny * light[1] + nz * light[2]);
            Color color = Palette.mix(shadow, lit, 0.12 + 0.88 * intensity);
            color = Palette.mix(color, Color.WHITE, Math.pow(intensity, 18) * (view.dark ? 0.75 : 0.5));
            Face face = new Face();
            face.color = color;
            face.depth = mz;
            for (int k = 0; k < 3; k++) {
                double[] p = turned[f[k]];
                double scaleZ = CAMERA / (CAMERA + p[2]);
                face.xs[k] = cx + p[0] * scale * scaleZ;
                face.ys[k] = cy + p[1] * scale * scaleZ;
            }
            faces.add(face);
        }
    }
}
