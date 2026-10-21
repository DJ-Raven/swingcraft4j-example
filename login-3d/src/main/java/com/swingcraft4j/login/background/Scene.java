package com.swingcraft4j.login.background;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/**
 * A 3D scene of the background, drawn over the stars.
 */
public abstract class Scene {

    private final String name;

    protected Scene(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Moves the scene one frame forward, on each frame while it is shown.
     */
    public void tick(View view) {
    }

    public abstract void paint(Graphics2D g2, View view);

    protected static Color alpha(Color c, int alpha) {
        return Palette.alpha(c, alpha);
    }

    /**
     * A soft round light fading out to the radius.
     */
    protected static void paintGlow(Graphics2D g2, double cx, double cy, double radius, Color color, int alpha) {
        if (radius <= 0) {
            return;
        }
        g2.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) radius,
                new float[]{0f, 1f},
                new Color[]{alpha(color, alpha), alpha(color, 0)}));
        g2.fill(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
    }

    /**
     * Draws a line from the mouse to the point in reach, and gives how near it is from 0 to 1.
     */
    protected static double linkMouse(Graphics2D g2, View view, double x, double y, double depth,
            double reach, Line2D.Double line) {
        double distance = view.mouseDistance(x, y);
        if (distance >= reach) {
            return 0;
        }
        double power = 1 - distance / reach;
        g2.setColor(alpha(view.light, (int) (power * 170 * (0.3 + depth * 0.7))));
        line.setLine(view.mouse.x, view.mouse.y, x, y);
        g2.draw(line);
        return power;
    }
}
