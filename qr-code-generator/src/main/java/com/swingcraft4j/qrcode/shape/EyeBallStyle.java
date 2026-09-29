package com.swingcraft4j.qrcode.shape;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/**
 * Built-in eye ball shapes, filling the 3 × 3 bounds.
 */
public enum EyeBallStyle implements EyeShape {

    SQUARE {
        @Override
        public Shape getShape(Rectangle2D b, Corner corner) {
            return b;
        }
    },

    ROUNDED {
        @Override
        public Shape getShape(Rectangle2D b, Corner corner) {
            return Shapes.roundRect(b.getX(), b.getY(), b.getWidth(), b.getHeight(), 0.9);
        }
    },

    CIRCLE {
        @Override
        public Shape getShape(Rectangle2D b, Corner corner) {
            return new Ellipse2D.Double(b.getX(), b.getY(), b.getWidth(), b.getHeight());
        }
    },

    /**
     * Rounded except at the corner facing the QR code's center.
     */
    LEAF {
        @Override
        public Shape getShape(Rectangle2D b, Corner corner) {
            return Shapes.leaf(b, corner, 1.5);
        }
    },

    DIAMOND {
        @Override
        public Shape getShape(Rectangle2D b, Corner corner) {
            // a little larger than the bounds, so it stays as heavy as a square for scanners
            return Shapes.diamond(b.getX() - 0.4, b.getY() - 0.4, b.getWidth() + 0.8, b.getHeight() + 0.8);
        }
    },

    /**
     * Corners cut off at 45°.
     */
    OCTAGON {
        @Override
        public Shape getShape(Rectangle2D b, Corner corner) {
            return Shapes.octagon(b.getX(), b.getY(), b.getWidth(), b.getHeight(), 0.9);
        }
    },

    /**
     * Five-point star, a little larger than the bounds and set slightly low, so scanners find the eye.
     */
    STAR {
        @Override
        public Shape getShape(Rectangle2D b, Corner corner) {
            return Shapes.star(b.getCenterX(), b.getCenterY() + 0.15, 2.1, 1.3, 5);
        }
    }
}
