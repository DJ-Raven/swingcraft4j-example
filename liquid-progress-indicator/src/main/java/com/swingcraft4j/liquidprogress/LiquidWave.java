package com.swingcraft4j.liquidprogress;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * One layer of liquid: a sine wave that swells up and down while it scrolls sideways.
 */
public class LiquidWave {

    // distance between the points of the wave outline
    private static final double STEP = 1;

    private Color color;
    private float angularSpeed;
    private float maxAmplitude;
    private float frequency;
    private float horizontalSpeed;

    private double angle;
    private double position;

    LiquidWave(float angularSpeed, float maxAmplitude, float frequency, float horizontalSpeed) {
        this.angularSpeed = angularSpeed;
        this.maxAmplitude = maxAmplitude;
        this.frequency = frequency;
        this.horizontalSpeed = horizontalSpeed;
    }

    /**
     * Fill color; {@code null} uses the meter's default for this layer.
     */
    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * How fast the wave swells, in degrees per second; 0 keeps it flat.
     */
    public float getAngularSpeed() {
        return angularSpeed;
    }

    public void setAngularSpeed(float angularSpeed) {
        this.angularSpeed = angularSpeed;
    }

    /**
     * Largest height of the wave above or below the liquid level.
     */
    public float getMaxAmplitude() {
        return maxAmplitude;
    }

    public void setMaxAmplitude(float maxAmplitude) {
        this.maxAmplitude = maxAmplitude;
    }

    /**
     * Wave length divided by 2π; larger is wider waves.
     */
    public float getFrequency() {
        return frequency;
    }

    public void setFrequency(float frequency) {
        this.frequency = frequency;
    }

    /**
     * How fast the wave scrolls, per second; negative scrolls right, positive scrolls left.
     */
    public float getHorizontalSpeed() {
        return horizontalSpeed;
    }

    public void setHorizontalSpeed(float horizontalSpeed) {
        this.horizontalSpeed = horizontalSpeed;
    }

    void step(float dt) {
        angle = (angle + angularSpeed * dt) % 360;
        position += horizontalSpeed * dt;
        if (frequency > 0) {
            // one full period later the wave looks the same, so the position never grows large
            position %= 2 * Math.PI * frequency;
        }
    }

    /**
     * Liquid below the wave, across the given bounds, with its surface around {@code level}.
     */
    Path2D path(Rectangle2D bounds, double level) {
        double amplitude = frequency > 0 ? maxAmplitude * Math.sin(Math.toRadians(angle)) : 0;
        double minX = bounds.getX();
        double maxX = bounds.getMaxX();
        double bottom = bounds.getMaxY() + 1;

        Path2D path = new Path2D.Double();
        path.moveTo(minX, bottom);
        for (double x = minX; ; x = Math.min(x + STEP, maxX)) {
            double y = amplitude != 0 ? level + amplitude * Math.sin((x + position) / frequency) : level;
            path.lineTo(x, y);
            if (x >= maxX) {
                break;
            }
        }
        path.lineTo(maxX, bottom);
        path.closePath();
        return path;
    }
}
