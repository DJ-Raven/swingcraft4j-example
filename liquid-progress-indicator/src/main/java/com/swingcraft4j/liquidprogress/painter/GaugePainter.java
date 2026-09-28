package com.swingcraft4j.liquidprogress.painter;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.ColorFunctions;
import com.swingcraft4j.liquidprogress.LiquidProgress;
import com.swingcraft4j.liquidprogress.util.ImageCache;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Liquid fill gauge: a thin colored ring, a gap, then the liquid circle, with text that turns white inside the liquid.
 */
public class GaugePainter implements LiquidPainter {

    private static final Rectangle2D SPACE = new Rectangle2D.Float(0, 0, LiquidProgress.METER_SIZE, LiquidProgress.METER_SIZE);
    private static final float OUTER_RADIUS = 132;
    // how much darker than the liquid the text over the track is
    private static final float TEXT_DARKEN = 0.2f;

    private float ringWidth = 8;
    private float gap = 10;
    private Color ringColor;
    private Color trackColor;

    // ring and track don't change between frames, so they're drawn once and reused as images
    private final ImageCache background = new ImageCache();
    private final ImageCache foreground = new ImageCache();

    /**
     * Width of the outer ring.
     */
    public float getRingWidth() {
        return ringWidth;
    }

    public void setRingWidth(float ringWidth) {
        this.ringWidth = Math.max(ringWidth, 0);
    }

    /**
     * Space between the ring and the liquid.
     */
    public float getGap() {
        return gap;
    }

    public void setGap(float gap) {
        this.gap = Math.max(gap, 0);
    }

    /**
     * Color of the ring; {@code null} uses the front wave color.
     */
    public Color getRingColor() {
        return ringColor;
    }

    public void setRingColor(Color ringColor) {
        this.ringColor = ringColor;
    }

    /**
     * Color of the empty part of the meter; {@code null} uses a shade of the meter background.
     */
    public Color getTrackColor() {
        return trackColor;
    }

    public void setTrackColor(Color trackColor) {
        this.trackColor = trackColor;
    }

    @Override
    public Shape getFluidShape(LiquidProgress c) {
        return circle(OUTER_RADIUS - ringWidth - gap);
    }

    @Override
    public void paintBackground(Graphics2D g, LiquidProgress c) {
        Color track = trackColor;
        if (track == null) {
            Color base = c.getMeterBackground();
            track = FlatLaf.isLafDark() ? ColorFunctions.darken(base, 0.04f) : ColorFunctions.lighten(base, 0.03f);
        }
        Color trackFill = track;
        Shape fluid = getFluidShape(c);
        background.paint(g, SPACE, new Key(trackFill, ringWidth, gap), ig -> {
            ig.setColor(trackFill);
            ig.fill(fluid);
        });
    }

    @Override
    public void paintForeground(Graphics2D g, LiquidProgress c) {
        if (ringWidth <= 0) {
            return;
        }
        Color ring = ringColor != null ? ringColor : c.getFrontColor();
        foreground.paint(g, SPACE, new Key(ring, ringWidth, gap), ig -> {
            Path2D area = new Path2D.Float(Path2D.WIND_EVEN_ODD);
            area.append(circle(OUTER_RADIUS), false);
            area.append(circle(OUTER_RADIUS - ringWidth), false);
            ig.setColor(ring);
            ig.fill(area);
        });
    }

    @Override
    public float getFontSize(LiquidProgress c) {
        return 62;
    }

    @Override
    public Color getTrackTextColor(LiquidProgress c) {
        Color front = c.getFrontColor();
        return FlatLaf.isLafDark() ? ColorFunctions.lighten(front, TEXT_DARKEN) : ColorFunctions.darken(front, TEXT_DARKEN);
    }

    private static Shape circle(float radius) {
        float center = LiquidProgress.METER_SIZE / 2;
        return new Ellipse2D.Float(center - radius, center - radius, radius * 2, radius * 2);
    }

    private record Key(Color color, float ringWidth, float gap) {
    }
}
