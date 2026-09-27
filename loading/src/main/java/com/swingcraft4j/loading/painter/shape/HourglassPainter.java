package com.swingcraft4j.loading.painter.shape;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

import static com.swingcraft4j.loading.LoadingPainter.alpha;
import static com.swingcraft4j.loading.painter.PainterMath.ease;
import static com.swingcraft4j.loading.painter.PainterMath.roundStroke;

/**
 * Hourglass draining its sand, then flipping over.
 */
public class HourglassPainter implements LoadingPainter {

    private static final float FLOW_END = 0.8f;

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        Color color = g.getColor();
        float stroke = size / 14f;
        float c = size / 2;
        // small enough that its corners stay inside the square while it's tilted mid-flip
        float halfWidth = size * 0.26f;
        float top = size * 0.12f;
        float bottom = size - top;

        // flip half a turn at the end: a full bottom bulb becomes a full top bulb, so the loop is seamless
        float flip = fraction < FLOW_END ? 0 : ease((fraction - FLOW_END) / (1 - FLOW_END));
        g.rotate(Math.PI * flip, c, c);

        float flow = Math.min(fraction / FLOW_END, 1);
        float inner = halfWidth - stroke;
        float bulb = c - top - stroke;
        g.setColor(alpha(color, 0.75f));
        // top sand: a shrinking triangle resting on the neck
        float topHeight = bulb * (1 - flow);
        float topWidth = inner * (topHeight / bulb);
        Path2D topSand = new Path2D.Float();
        topSand.moveTo(c - topWidth, c - topHeight);
        topSand.lineTo(c + topWidth, c - topHeight);
        topSand.lineTo(c, c);
        topSand.closePath();
        g.fill(topSand);
        // bottom sand: a pile growing up from the base
        float pileHeight = bulb * flow;
        float pileTop = inner * (1 - pileHeight / bulb);
        float base = bottom - stroke;
        Path2D pile = new Path2D.Float();
        pile.moveTo(c - inner, base);
        pile.lineTo(c + inner, base);
        pile.lineTo(c + pileTop, base - pileHeight);
        pile.lineTo(c - pileTop, base - pileHeight);
        pile.closePath();
        g.fill(pile);
        if (flow < 1) {
            g.setStroke(new BasicStroke(stroke / 2));
            g.draw(new Line2D.Float(c, c, c, base - pileHeight));
        }

        g.setColor(color);
        g.setStroke(roundStroke(stroke));
        Path2D glass = new Path2D.Float();
        glass.moveTo(c - halfWidth, top);
        glass.lineTo(c + halfWidth, top);
        glass.lineTo(c, c);
        glass.lineTo(c + halfWidth, bottom);
        glass.lineTo(c - halfWidth, bottom);
        glass.lineTo(c, c);
        glass.closePath();
        g.draw(glass);
    }

    @Override
    public int getDuration() {
        return 2400;
    }
}
