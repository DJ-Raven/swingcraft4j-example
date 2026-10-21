package com.swingcraft4j.login.background;

import java.awt.Color;
import java.awt.Point;

/**
 * What a scene gets to draw a frame: size, mouse, spin, tilt and theme colors.
 */
public class View {

    public int width;
    public int height;
    public long frame;
    // the mouse in the panel, null when it is outside
    public Point mouse;
    // the mouse from -0.5 to 0.5 across the panel, eased so the scene follows it smoothly
    public double mouseX;
    public double mouseY;
    // the turn of the slow spin of its own and of the drag
    public double spinX;
    public double spinY;
    // the tilt toward the mouse
    public double tiltX;
    public double tiltY;

    public Color far;
    public Color near;
    public Color accent;
    public boolean dark;
    // the color of the links to the mouse
    public Color light;
    // the color of a bright light, that shows on the background
    public Color glow;
    // the colors from far to near, with more alpha for the near ones
    private final Color[] edgeColors = new Color[64];
    private final Color[] nodeColors = new Color[64];

    private double cosX = 1, sinX, cosY = 1, sinY;

    void setColors(Color far, Color near, Color accent, boolean dark) {
        this.far = far;
        this.near = near;
        this.accent = accent;
        this.dark = dark;
        light = dark ? Palette.mix(near, Color.WHITE, 0.5) : Palette.mix(near, Color.BLACK, 0.15);
        glow = dark ? light : accent;
        for (int i = 0; i < 64; i++) {
            double d = i / 63.0;
            Color c = Palette.mix(far, near, d);
            edgeColors[i] = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (25 + 110 * d));
            nodeColors[i] = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (90 + 165 * d));
        }
    }

    /**
     * The angles {@link #project} turns the points by: around X, then around Y.
     */
    public void setAngles(double angleX, double angleY) {
        cosX = Math.cos(angleX);
        sinX = Math.sin(angleX);
        cosY = Math.cos(angleY);
        sinY = Math.sin(angleY);
    }

    /**
     * Turns the point and puts it in out, still in 3D, to light it or test which way a face looks.
     */
    public void rotate(double x, double y, double z, double[] out) {
        double rx = x * cosY + z * sinY;
        double rz = -x * sinY + z * cosY;
        out[0] = rx;
        out[1] = y * cosX - rz * sinX;
        out[2] = y * sinX + rz * cosX;
    }

    /**
     * Turns and projects the point: puts its x and y on screen and its z after the turn (negative is near) in out.
     */
    public void project(double x, double y, double z, double cx, double cy, double scale, double camera, double[] out) {
        double rx = x * cosY + z * sinY;
        double rz = -x * sinY + z * cosY;
        double ry = y * cosX - rz * sinX;
        rz = y * sinX + rz * cosX;
        double f = camera / (camera + rz);
        out[0] = cx + rx * scale * f;
        out[1] = cy + ry * scale * f;
        out[2] = rz;
    }

    /**
     * The color of a line at the depth from 0 (far) to 1 (near).
     */
    public Color edge(double depth) {
        return edgeColors[index(depth)];
    }

    /**
     * The color of a point at the depth from 0 (far) to 1 (near).
     */
    public Color node(double depth) {
        return nodeColors[index(depth)];
    }

    private static int index(double depth) {
        return (int) (Math.max(0, Math.min(1, depth)) * 63);
    }

    /**
     * The distance on screen from the mouse, or {@code Double.MAX_VALUE} when the mouse is outside.
     */
    public double mouseDistance(double x, double y) {
        if (mouse == null) {
            return Double.MAX_VALUE;
        }
        double dx = x - mouse.x, dy = y - mouse.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
