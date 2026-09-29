package com.swingcraft4j.qrcode.shape;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Shape helpers for building body and eye shapes.
 */
public final class Shapes {

    // control point distance of a cubic curve that draws a quarter circle
    private static final double K = 0.5522847498;

    private Shapes() {
    }

    /**
     * Rectangle with its own radius at each corner: top left, top right, bottom right, bottom left.
     */
    public static Path2D roundRect(double x, double y, double w, double h, double tl, double tr, double br, double bl) {
        Path2D p = new Path2D.Double();
        p.moveTo(x + tl, y);
        p.lineTo(x + w - tr, y);
        if (tr > 0) {
            p.curveTo(x + w - tr * (1 - K), y, x + w, y + tr * (1 - K), x + w, y + tr);
        }
        p.lineTo(x + w, y + h - br);
        if (br > 0) {
            p.curveTo(x + w, y + h - br * (1 - K), x + w - br * (1 - K), y + h, x + w - br, y + h);
        }
        p.lineTo(x + bl, y + h);
        if (bl > 0) {
            p.curveTo(x + bl * (1 - K), y + h, x, y + h - bl * (1 - K), x, y + h - bl);
        }
        p.lineTo(x, y + tl);
        if (tl > 0) {
            p.curveTo(x, y + tl * (1 - K), x + tl * (1 - K), y, x + tl, y);
        }
        p.closePath();
        return p;
    }

    /**
     * Rectangle with the same radius at every corner.
     */
    public static Path2D roundRect(double x, double y, double w, double h, double r) {
        return roundRect(x, y, w, h, r, r, r, r);
    }

    /**
     * Rounded rectangle whose corner facing the QR code's center stays sharp, so eyes point inward like leaves.
     */
    public static Path2D leaf(Rectangle2D b, Corner corner, double r) {
        return roundRect(b.getX(), b.getY(), b.getWidth(), b.getHeight(),
                r, corner == Corner.BOTTOM_LEFT ? 0 : r, corner == Corner.TOP_LEFT ? 0 : r, corner == Corner.TOP_RIGHT ? 0 : r);
    }

    /**
     * Diamond touching the middle of each side of the bounds.
     */
    public static Path2D diamond(double x, double y, double w, double h) {
        Path2D p = new Path2D.Double();
        p.moveTo(x + w / 2, y);
        p.lineTo(x + w, y + h / 2);
        p.lineTo(x + w / 2, y + h);
        p.lineTo(x, y + h / 2);
        p.closePath();
        return p;
    }

    /**
     * Rectangle with its corners cut off at 45°, {@code cut} along each side.
     */
    public static Path2D octagon(double x, double y, double w, double h, double cut) {
        return polygon(x + cut, y, x + w - cut, y, x + w, y + cut, x + w, y + h - cut,
                x + w - cut, y + h, x + cut, y + h, x, y + h - cut, x, y + cut);
    }

    /**
     * Hexagon with a point at the top and bottom, filling the bounds.
     */
    public static Path2D hexagon(double x, double y, double w, double h) {
        return polygon(x + w / 2, y, x + w, y + h / 4, x + w, y + h * 3 / 4,
                x + w / 2, y + h, x, y + h * 3 / 4, x, y + h / 4);
    }

    /**
     * Star centered at (cx, cy) with its first point straight up.
     */
    public static Path2D star(double cx, double cy, double outer, double inner, int points) {
        double[] coords = new double[points * 4];
        for (int i = 0; i < points * 2; i++) {
            double r = i % 2 == 0 ? outer : inner;
            double angle = -Math.PI / 2 + Math.PI * i / points;
            coords[i * 2] = cx + r * Math.cos(angle);
            coords[i * 2 + 1] = cy + r * Math.sin(angle);
        }
        return polygon(coords);
    }

    /**
     * Closed polygon through the given x, y pairs.
     */
    public static Path2D polygon(double... coords) {
        Path2D p = new Path2D.Double();
        p.moveTo(coords[0], coords[1]);
        for (int i = 2; i < coords.length; i += 2) {
            p.lineTo(coords[i], coords[i + 1]);
        }
        p.closePath();
        return p;
    }

    /**
     * Ring between an outer and an inner shape.
     */
    public static Path2D ring(Shape outer, Shape inner) {
        Path2D p = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        p.append(outer, false);
        p.append(inner, false);
        return p;
    }

    /**
     * Inside fillet at a corner point: the square of side {@code r} toward (dx, dy), less a quarter circle.
     */
    public static Path2D fillet(double x, double y, double r, int dx, int dy) {
        Path2D p = new Path2D.Double();
        p.moveTo(x, y);
        p.lineTo(x + dx * r, y);
        p.curveTo(x + dx * r * (1 - K), y, x, y + dy * r * (1 - K), x, y + dy * r);
        p.closePath();
        return p;
    }
}
