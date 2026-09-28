package com.swingcraft4j.liquidprogress.painter;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.ColorFunctions;
import com.swingcraft4j.liquidprogress.LiquidProgress;
import com.swingcraft4j.liquidprogress.util.ImageCache;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Outlined container of any shape (box, battery, flask, test tube, heart, droplet or your own) with a glass highlight.
 */
public class ShapePainter implements LiquidPainter {

    private static final Rectangle2D SPACE = new Rectangle2D.Float(0, 0, LiquidProgress.METER_SIZE, LiquidProgress.METER_SIZE);
    private static final Color HIGHLIGHT_LIGHT = new Color(255, 255, 255, 70);
    private static final Color HIGHLIGHT_DARK = new Color(255, 255, 255, 40);

    private final Shape shape;
    private final Shape parts;
    private final Shape highlight;
    private final float fontSize;

    private float outlineWidth;
    private Color outlineColor;
    private Color trackColor;
    private boolean highlightPainted = true;

    // track, outline and highlight don't change between frames, so they're drawn once and reused as images
    private final ImageCache background = new ImageCache();
    private final ImageCache foreground = new ImageCache();

    /**
     * Container filled by the liquid, in the 300 × 300 space; {@code parts} (cap, rim) and {@code highlight} (glare) may be {@code null}.
     */
    public ShapePainter(Shape shape, Shape parts, Shape highlight, float outlineWidth, float fontSize) {
        this.shape = shape;
        this.parts = parts;
        this.highlight = highlight;
        this.outlineWidth = outlineWidth;
        this.fontSize = fontSize;
    }

    /**
     * Rounded square tank.
     */
    public static ShapePainter box() {
        return new ShapePainter(round(42, 42, 216, 216, 48), null, round(62, 62, 18, 120, 18), 10, 64);
    }

    /**
     * Upright battery with a cap on top.
     */
    public static ShapePainter battery() {
        return new ShapePainter(round(88, 50, 124, 222, 28), round(124, 24, 52, 22, 10), round(104, 70, 14, 110, 14), 10, 44);
    }

    /**
     * Laboratory flask with a rim on its neck.
     */
    public static ShapePainter flask() {
        Path2D p = new Path2D.Float();
        p.moveTo(124, 38);
        p.lineTo(176, 38);
        p.lineTo(176, 110);
        p.lineTo(248, 236);
        p.quadTo(264, 268, 228, 268);
        p.lineTo(72, 268);
        p.quadTo(36, 268, 52, 236);
        p.lineTo(124, 110);
        p.closePath();
        return new ShapePainter(p, round(112, 26, 76, 16, 8), null, 9, 40);
    }

    /**
     * Narrow test tube with a rounded bottom and a rim.
     */
    public static ShapePainter testTube() {
        return new ShapePainter(round(112, 34, 76, 244, 76), round(100, 22, 100, 16, 8), round(126, 60, 12, 150, 12), 9, 30);
    }

    /**
     * Heart.
     */
    public static ShapePainter heart() {
        // one smooth path: each curve leaves in the direction the previous one arrived, except at the notch and tip
        Path2D p = new Path2D.Float();
        p.moveTo(150, 268);
        p.curveTo(110, 235, 30, 180, 30, 112);
        p.curveTo(30, 68, 62, 40, 98, 40);
        p.curveTo(124, 40, 142, 56, 150, 74);
        p.curveTo(158, 56, 176, 40, 202, 40);
        p.curveTo(238, 40, 270, 68, 270, 112);
        p.curveTo(270, 180, 190, 235, 150, 268);
        p.closePath();
        return new ShapePainter(p, null, null, 9, 54);
    }

    /**
     * Water drop.
     */
    public static ShapePainter droplet() {
        Area a = new Area(new Ellipse2D.Float(65, 105, 170, 170));
        // lines from the tip touch the circle where they meet it at a right angle
        Path2D tip = new Path2D.Float();
        tip.moveTo(150, 22);
        tip.lineTo(223.6f, 147.5f);
        tip.lineTo(76.4f, 147.5f);
        tip.closePath();
        a.add(new Area(tip));
        return new ShapePainter(new Path2D.Float(a), null, null, 9, 54);
    }

    /**
     * Width of the outline, centered on the shape's edge.
     */
    public float getOutlineWidth() {
        return outlineWidth;
    }

    public void setOutlineWidth(float outlineWidth) {
        this.outlineWidth = Math.max(outlineWidth, 0);
    }

    /**
     * Color of the outline and parts; {@code null} uses a shade of the meter background.
     */
    public Color getOutlineColor() {
        return outlineColor;
    }

    public void setOutlineColor(Color outlineColor) {
        this.outlineColor = outlineColor;
    }

    /**
     * Color of the empty part of the container; {@code null} uses a shade of the meter background.
     */
    public Color getTrackColor() {
        return trackColor;
    }

    public void setTrackColor(Color trackColor) {
        this.trackColor = trackColor;
    }

    public boolean isHighlightPainted() {
        return highlightPainted;
    }

    public void setHighlightPainted(boolean highlightPainted) {
        this.highlightPainted = highlightPainted;
    }

    @Override
    public Shape getFluidShape(LiquidProgress c) {
        return shape;
    }

    @Override
    public void paintBackground(Graphics2D g, LiquidProgress c) {
        Color track = trackColor;
        if (track == null) {
            Color base = c.getMeterBackground();
            track = FlatLaf.isLafDark() ? ColorFunctions.darken(base, 0.04f) : ColorFunctions.darken(base, 0.07f);
        }
        Color trackFill = track;
        background.paint(g, SPACE, trackFill, ig -> {
            ig.setColor(trackFill);
            ig.fill(shape);
        });
    }

    @Override
    public void paintForeground(Graphics2D g, LiquidProgress c) {
        boolean dark = FlatLaf.isLafDark();
        Color outline = resolveOutline(c);
        Color glare = highlightPainted && highlight != null ? (dark ? HIGHLIGHT_DARK : HIGHLIGHT_LIGHT) : null;
        foreground.paint(g, SPACE, new Key(outline, glare, outlineWidth), ig -> {
            if (glare != null) {
                ig.setColor(glare);
                ig.fill(highlight);
            }
            ig.setColor(outline);
            if (outlineWidth > 0) {
                ig.setStroke(new BasicStroke(outlineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                ig.draw(shape);
            }
            if (parts != null) {
                ig.fill(parts);
            }
        });
    }

    @Override
    public float getFontSize(LiquidProgress c) {
        return fontSize;
    }

    @Override
    public Color getTrackTextColor(LiquidProgress c) {
        Color base = c.getMeterBackground();
        return FlatLaf.isLafDark() ? ColorFunctions.lighten(base, 0.55f) : ColorFunctions.darken(base, 0.5f);
    }

    private Color resolveOutline(LiquidProgress c) {
        if (outlineColor != null) {
            return outlineColor;
        }
        Color base = c.getMeterBackground();
        return FlatLaf.isLafDark() ? ColorFunctions.lighten(base, 0.2f) : ColorFunctions.darken(base, 0.22f);
    }

    private static Shape round(float x, float y, float w, float h, float arc) {
        return new RoundRectangle2D.Float(x, y, w, h, arc, arc);
    }

    private record Key(Color outline, Color glare, float outlineWidth) {
    }
}
