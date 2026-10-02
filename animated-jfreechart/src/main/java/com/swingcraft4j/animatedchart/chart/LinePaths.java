package com.swingcraft4j.animatedchart.chart;

import com.swingcraft4j.animatedchart.chart.LineChart.LineStyle;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Builds the path of a line through screen points, in each line style.
 */
final class LinePaths {

    // spacing between points, in pixels, under which a curve through them can't be told from straight segments
    private static final double DENSE_SPACING = 3;

    private LinePaths() {
    }

    /**
     * Path through two or more points, left to right or right to left.
     */
    static Path2D create(List<Point2D.Double> points, LineStyle style) {
        Path2D path = new Path2D.Double();
        path.moveTo(points.getFirst().x, points.getFirst().y);
        // points this close look the same joined by straight segments, which draw twice as fast as curves
        boolean dense = Math.abs(points.getLast().x - points.getFirst().x) / (points.size() - 1) < DENSE_SPACING;
        switch (dense && style != LineStyle.STEP ? LineStyle.STRAIGHT : style) {
            case SMOOTH -> smooth(path, points);
            case MONOTONE -> monotone(path, points);
            case STRAIGHT -> {
                for (int i = 1; i < points.size(); i++) {
                    path.lineTo(points.get(i).x, points.get(i).y);
                }
            }
            case STEP -> {
                // each value is held across its category; the step falls halfway to the next point
                for (int i = 1; i < points.size(); i++) {
                    double middle = (points.get(i - 1).x + points.get(i).x) / 2;
                    path.lineTo(middle, points.get(i - 1).y);
                    path.lineTo(middle, points.get(i).y);
                }
                path.lineTo(points.getLast().x, points.getLast().y);
            }
        }
        return path;
    }

    /**
     * Closed path through three or more points around a center, as a round curve or as straight segments.
     */
    static Path2D closed(List<Point2D.Double> points, boolean smooth) {
        int count = points.size();
        Path2D path = new Path2D.Double();
        path.moveTo(points.getFirst().x, points.getFirst().y);
        for (int i = 0; i < count; i++) {
            Point2D.Double to = points.get((i + 1) % count);
            if (smooth) {
                // the same spline as an open smooth line, with the points before the first and after the last wrapping around
                Point2D.Double before = points.get((i - 1 + count) % count);
                Point2D.Double from = points.get(i);
                Point2D.Double after = points.get((i + 2) % count);
                path.curveTo(from.x + (to.x - before.x) / 6, from.y + (to.y - before.y) / 6,
                        to.x - (after.x - from.x) / 6, to.y - (after.y - from.y) / 6, to.x, to.y);
            } else {
                path.lineTo(to.x, to.y);
            }
        }
        path.closePath();
        return path;
    }

    /**
     * Catmull-Rom spline as Bézier segments: a round curve that may overshoot the points.
     */
    private static void smooth(Path2D path, List<Point2D.Double> points) {
        int last = points.size() - 1;
        for (int i = 0; i < last; i++) {
            Point2D.Double before = points.get(Math.max(i - 1, 0));
            Point2D.Double from = points.get(i);
            Point2D.Double to = points.get(i + 1);
            Point2D.Double after = points.get(Math.min(i + 2, last));
            path.curveTo(from.x + (to.x - before.x) / 6, from.y + (to.y - before.y) / 6,
                    to.x - (after.x - from.x) / 6, to.y - (after.y - from.y) / 6, to.x, to.y);
        }
    }

    /**
     * Monotone cubic (Fritsch-Butland): a curve that never goes above or below the points it joins.
     */
    private static void monotone(Path2D path, List<Point2D.Double> points) {
        int count = points.size();
        double[] width = new double[count - 1];
        double[] slope = new double[count - 1];
        for (int i = 0; i < count - 1; i++) {
            width[i] = points.get(i + 1).x - points.get(i).x;
            slope[i] = (points.get(i + 1).y - points.get(i).y) / width[i];
        }
        double[] tangent = new double[count];
        tangent[0] = slope[0];
        tangent[count - 1] = slope[count - 2];
        for (int i = 1; i < count - 1; i++) {
            // flat at a peak or valley, else the weighted harmonic mean of the slopes on both sides
            tangent[i] = slope[i - 1] * slope[i] <= 0 ? 0
                    : 3 * (width[i - 1] + width[i])
                    / ((2 * width[i] + width[i - 1]) / slope[i - 1] + (width[i] + 2 * width[i - 1]) / slope[i]);
        }
        for (int i = 0; i < count - 1; i++) {
            Point2D.Double from = points.get(i);
            Point2D.Double to = points.get(i + 1);
            double third = width[i] / 3;
            path.curveTo(from.x + third, from.y + tangent[i] * third, to.x - third, to.y - tangent[i + 1] * third, to.x, to.y);
        }
    }
}
