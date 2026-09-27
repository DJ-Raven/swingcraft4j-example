package com.swingcraft4j.loading.painter.arc;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;

import static com.swingcraft4j.loading.LoadingPainter.alpha;

/**
 * Open ring spinning steadily, fading from a solid head to a transparent tail.
 */
public class FadeArcPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        // geometry from a 24-unit square: ring radius 9, thickness 3
        float s = size / 24f;
        float c = 12 * s;
        float radius = 9 * s;
        Color color = g.getColor();
        g.rotate(2 * Math.PI * fraction, c, c);

        // leading half: from its round head left of the top, down the left side to the bottom
        Shape leading = arc(c, radius, s, 99.6f, 170.4f, 99.6f);
        g.setPaint(new GradientPaint(0, 2.73f * s, color, 0, 20.79f * s, alpha(color, 0.55f)));
        g.fill(leading);

        // trailing half: from the bottom, up the right side to its faded round tail
        Shape trailing = arc(c, radius, s, 270, 138.1f, 48.1f);
        g.setPaint(new GradientPaint(0, 6.65f * s, alpha(color, 0), 0, 20.1f * s, alpha(color, 0.55f)));
        g.fill(trailing);
    }

    /**
     * Ring segment with a flat cut at the bottom and a round cap at {@code capAngle} (degrees, counterclockwise from 3 o'clock).
     */
    private static Shape arc(float c, float radius, float s, float start, float extent, float capAngle) {
        float d = radius * 2;
        Arc2D arc = new Arc2D.Float(c - radius, c - radius, d, d, start, extent, Arc2D.OPEN);
        Area area = new Area(new BasicStroke(3 * s, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND).createStrokedShape(arc));
        double angle = Math.toRadians(capAngle);
        float x = c + radius * (float) Math.cos(angle);
        float y = c - radius * (float) Math.sin(angle);
        float r = 1.5f * s;
        area.add(new Area(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2)));
        return area;
    }
}
