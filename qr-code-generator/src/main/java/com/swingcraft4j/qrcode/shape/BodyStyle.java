package com.swingcraft4j.qrcode.shape;

import com.swingcraft4j.qrcode.QrMatrix;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Built-in body shapes.
 */
public enum BodyStyle implements BodyShape {

    /**
     * Plain squares that touch, the classic look.
     */
    SQUARE {
        @Override
        public Shape getShape(QrMatrix m, int x, int y) {
            return new Rectangle2D.Double(x, y, 1, 1);
        }
    },

    /**
     * Separate rounded squares.
     */
    ROUNDED {
        @Override
        public Shape getShape(QrMatrix m, int x, int y) {
            return Shapes.roundRect(x + 0.05, y + 0.05, 0.9, 0.9, 0.3);
        }
    },

    /**
     * Separate round dots.
     */
    DOTS {
        @Override
        public Shape getShape(QrMatrix m, int x, int y) {
            return new Ellipse2D.Double(x + 0.07, y + 0.07, 0.86, 0.86);
        }
    },

    /**
     * Joined modules with round outer corners and filleted inner corners, like liquid.
     */
    FLUID {
        @Override
        public Shape getShape(QrMatrix m, int x, int y) {
            boolean top = m.isDark(x, y - 1);
            boolean right = m.isDark(x + 1, y);
            boolean bottom = m.isDark(x, y + 1);
            boolean left = m.isDark(x - 1, y);
            // a corner is round only where neither side it touches has a neighbor
            return Shapes.roundRect(x, y, 1, 1,
                    top || left ? 0 : R, top || right ? 0 : R, bottom || right ? 0 : R, bottom || left ? 0 : R);
        }

        @Override
        public Shape getGapShape(QrMatrix m, int x, int y) {
            // fill a corner of the light module where its two sides and the diagonal are dark
            Path2D gap = null;
            for (int dy = -1; dy <= 1; dy += 2) {
                for (int dx = -1; dx <= 1; dx += 2) {
                    if (m.isDark(x + dx, y) && m.isDark(x, y + dy) && m.isDark(x + dx, y + dy)) {
                        if (gap == null) {
                            gap = new Path2D.Double();
                        }
                        // corner point of the module, filleted toward its center
                        gap.append(Shapes.fillet(x + (dx > 0 ? 1 : 0), y + (dy > 0 ? 1 : 0), FILLET, -dx, -dy), false);
                    }
                }
            }
            return gap;
        }
    },

    /**
     * Horizontal bars joining modules in a row, with round ends.
     */
    HORIZONTAL {
        @Override
        public Shape getShape(QrMatrix m, int x, int y) {
            double left = m.isDark(x - 1, y) ? 0 : LINE / 2;
            double right = m.isDark(x + 1, y) ? 0 : LINE / 2;
            return Shapes.roundRect(x, y + (1 - LINE) / 2, 1, LINE, left, right, right, left);
        }
    },

    /**
     * Vertical bars joining modules in a column, with round ends.
     */
    VERTICAL {
        @Override
        public Shape getShape(QrMatrix m, int x, int y) {
            double top = m.isDark(x, y - 1) ? 0 : LINE / 2;
            double bottom = m.isDark(x, y + 1) ? 0 : LINE / 2;
            return Shapes.roundRect(x + (1 - LINE) / 2, y, LINE, 1, top, top, bottom, bottom);
        }
    },

    /**
     * Separate leaves: squares rounded at the top left and bottom right.
     */
    LEAF {
        @Override
        public Shape getShape(QrMatrix m, int x, int y) {
            return Shapes.roundRect(x + 0.04, y + 0.04, 0.92, 0.92, 0.46, 0, 0.46, 0);
        }
    },

    /**
     * Diamonds that touch their diagonal neighbors at the points.
     */
    DIAMOND {
        @Override
        public Shape getShape(QrMatrix m, int x, int y) {
            return Shapes.diamond(x - 0.05, y - 0.05, 1.1, 1.1);
        }
    },

    /**
     * Dots joined to their dark neighbors by bars, like beads on a string.
     */
    CONNECTED {
        @Override
        public Shape getShape(QrMatrix m, int x, int y) {
            double dot = 0.86;
            double bar = 0.44;
            double inset = (1 - dot) / 2;
            double barInset = (1 - bar) / 2;
            Area area = new Area(new Ellipse2D.Double(x + inset, y + inset, dot, dot));
            // each bar reaches only this module's edge, and the neighbor draws the other half
            if (m.isDark(x - 1, y)) {
                area.add(new Area(new Rectangle2D.Double(x, y + barInset, 0.5, bar)));
            }
            if (m.isDark(x + 1, y)) {
                area.add(new Area(new Rectangle2D.Double(x + 0.5, y + barInset, 0.5, bar)));
            }
            if (m.isDark(x, y - 1)) {
                area.add(new Area(new Rectangle2D.Double(x + barInset, y, bar, 0.5)));
            }
            if (m.isDark(x, y + 1)) {
                area.add(new Area(new Rectangle2D.Double(x + barInset, y + 0.5, bar, 0.5)));
            }
            return area;
        }
    },

    /**
     * Separate hexagons.
     */
    HEXAGON {
        @Override
        public Shape getShape(QrMatrix m, int x, int y) {
            return Shapes.hexagon(x + 0.04, y - 0.02, 0.92, 1.04);
        }
    },

    /**
     * Plus signs whose arms join their neighbors into a lattice.
     */
    CROSS {
        @Override
        public Shape getShape(QrMatrix m, int x, int y) {
            double arm = 0.46;
            double inset = (1 - arm) / 2;
            Area area = new Area(new Rectangle2D.Double(x, y + inset, 1, arm));
            area.add(new Area(new Rectangle2D.Double(x + inset, y, arm, 1)));
            return area;
        }
    },

    /**
     * Separate five-point stars.
     */
    STAR {
        @Override
        public Shape getShape(QrMatrix m, int x, int y) {
            return Shapes.star(x + 0.5, y + 0.54, 0.6, 0.3, 5);
        }
    };

    // outer corner radius and inner fillet of FLUID, and bar thickness of the line styles
    private static final double R = 0.5;
    private static final double FILLET = 0.3;
    private static final double LINE = 0.8;
}
