package com.swingcraft4j.qrcode.shape;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Built-in eye frame shapes: a one-module ring around the 7 × 7 bounds.
 */
public enum EyeFrameStyle implements EyeShape {

    SQUARE {
        @Override
        public Shape getShape(Rectangle2D b, Corner corner) {
            return Shapes.ring(b, inner(b));
        }
    },

    ROUNDED {
        @Override
        public Shape getShape(Rectangle2D b, Corner corner) {
            Rectangle2D in = inner(b);
            return Shapes.ring(Shapes.roundRect(b.getX(), b.getY(), b.getWidth(), b.getHeight(), 2.5),
                    Shapes.roundRect(in.getX(), in.getY(), in.getWidth(), in.getHeight(), 1.5));
        }
    },

    CIRCLE {
        @Override
        public Shape getShape(Rectangle2D b, Corner corner) {
            Rectangle2D in = inner(b);
            return Shapes.ring(new Ellipse2D.Double(b.getX(), b.getY(), b.getWidth(), b.getHeight()),
                    new Ellipse2D.Double(in.getX(), in.getY(), in.getWidth(), in.getHeight()));
        }
    },

    /**
     * Rounded except at the corner facing the QR code's center.
     */
    LEAF {
        @Override
        public Shape getShape(Rectangle2D b, Corner corner) {
            return Shapes.ring(Shapes.leaf(b, corner, 2.5), Shapes.leaf(inner(b), corner, 1.5));
        }
    },

    /**
     * Corners cut off at 45°.
     */
    OCTAGON {
        @Override
        public Shape getShape(Rectangle2D b, Corner corner) {
            Rectangle2D in = inner(b);
            // the inner cut is smaller by 2 - √2, leaving √2, so the ring is one module wide on the diagonals too
            return Shapes.ring(Shapes.octagon(b.getX(), b.getY(), b.getWidth(), b.getHeight(), 2),
                    Shapes.octagon(in.getX(), in.getY(), in.getWidth(), in.getHeight(), Math.sqrt(2)));
        }
    },

    /**
     * A ring of small rounded squares, one per module; not allowed with the star ball, and with the diamond ball it may not scan at every size.
     */
    DOTTED {
        @Override
        public Shape getShape(Rectangle2D b, Corner corner) {
            Path2D dots = new Path2D.Double();
            int cells = (int) Math.round(b.getWidth());
            for (int i = 0; i < cells; i++) {
                for (int j = 0; j < cells; j++) {
                    if (i == 0 || j == 0 || i == cells - 1 || j == cells - 1) {
                        dots.append(Shapes.roundRect(b.getX() + i + DOT_INSET, b.getY() + j + DOT_INSET, 1 - DOT_INSET * 2, 1 - DOT_INSET * 2, DOT_RADIUS), false);
                    }
                }
            }
            return dots;
        }
    };

    // gap around each DOTTED dot inside its module, and its corner radius
    private static final double DOT_INSET = 0.03;
    private static final double DOT_RADIUS = 0.25;

    private static Rectangle2D inner(Rectangle2D b) {
        return new Rectangle2D.Double(b.getX() + 1, b.getY() + 1, b.getWidth() - 2, b.getHeight() - 2);
    }
}
