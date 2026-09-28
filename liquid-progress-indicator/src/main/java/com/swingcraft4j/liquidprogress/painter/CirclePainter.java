package com.swingcraft4j.liquidprogress.painter;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.ColorFunctions;
import com.swingcraft4j.liquidprogress.LiquidProgress;
import com.swingcraft4j.liquidprogress.util.ImageCache;
import com.swingcraft4j.liquidprogress.util.Shadow;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Round meter: the liquid in a circular track, framed by a thick ring with a soft drop shadow.
 */
public class CirclePainter implements LiquidPainter {

    // the meter fills 90% of the space, leaving room for the shadow
    private static final float DIAMETER = LiquidProgress.METER_SIZE * 0.9f;
    private static final Rectangle2D SPACE = new Rectangle2D.Float(0, 0, LiquidProgress.METER_SIZE, LiquidProgress.METER_SIZE);
    private static final float SHADOW_OFFSET = 4;
    private static final float SHADOW_BLUR = 3;
    // the ring reaches this far over the liquid's edge, so no seam shows between them
    private static final float OVERLAP = 1;

    private float borderWidth = 19;
    private Color ringColor;
    private Color trackColor;
    private boolean shadowPainted = true;

    // shadow, track and ring don't change between frames, so they're drawn once and reused as images
    private final ImageCache background = new ImageCache();
    private final ImageCache foreground = new ImageCache();

    /**
     * Width of the ring around the liquid.
     */
    public float getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(float borderWidth) {
        this.borderWidth = Math.max(borderWidth, 0);
    }

    /**
     * Color of the ring; {@code null} uses a shade of the meter background.
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

    public boolean isShadowPainted() {
        return shadowPainted;
    }

    public void setShadowPainted(boolean shadowPainted) {
        this.shadowPainted = shadowPainted;
    }

    @Override
    public Shape getFluidShape(LiquidProgress c) {
        return circle(DIAMETER / 2 - borderWidth);
    }

    @Override
    public void paintBackground(Graphics2D g, LiquidProgress c) {
        boolean dark = FlatLaf.isLafDark();
        Color track = trackColor;
        if (track == null) {
            Color base = c.getMeterBackground();
            track = dark ? ColorFunctions.darken(base, 0.04f) : ColorFunctions.darken(base, 0.09f);
        }
        Color shadow = shadowPainted ? new Color(0, 0, 0, dark ? 90 : 26) : null;
        Shape fluid = getFluidShape(c);
        Color trackFill = track;
        background.paint(g, SPACE, new BackgroundKey(trackFill, shadow, borderWidth), ig -> {
            if (shadow != null) {
                new Shadow(circle(DIAMETER / 2), SHADOW_BLUR, shadow, Shadow.deviceScale(ig)).paint(ig, 0, SHADOW_OFFSET);
            }
            ig.setColor(trackFill);
            ig.fill(fluid);
        });
    }

    @Override
    public void paintForeground(Graphics2D g, LiquidProgress c) {
        if (borderWidth <= 0) {
            return;
        }
        Color ring = ringColor;
        if (ring == null) {
            Color base = c.getMeterBackground();
            ring = FlatLaf.isLafDark() ? ColorFunctions.lighten(base, 0.08f) : ColorFunctions.lighten(base, 0.02f);
        }
        Color ringFill = ring;
        foreground.paint(g, SPACE, new ForegroundKey(ringFill, borderWidth), ig -> {
            Path2D area = new Path2D.Float(Path2D.WIND_EVEN_ODD);
            area.append(circle(DIAMETER / 2), false);
            area.append(circle(DIAMETER / 2 - borderWidth - OVERLAP), false);
            ig.setColor(ringFill);
            ig.fill(area);
        });
    }

    private static Shape circle(float radius) {
        float center = LiquidProgress.METER_SIZE / 2;
        return new Ellipse2D.Float(center - radius, center - radius, radius * 2, radius * 2);
    }

    private record BackgroundKey(Color track, Color shadow, float borderWidth) {
    }

    private record ForegroundKey(Color ring, float borderWidth) {
    }
}
