package com.swingcraft4j.loading.painter.shape;

import com.swingcraft4j.loading.LoadingPainter;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

import static com.swingcraft4j.loading.LoadingPainter.alpha;
import static com.swingcraft4j.loading.painter.PainterMath.ease;
import static com.swingcraft4j.loading.painter.PainterMath.wrap;

/**
 * Rounded square flipping over its vertical, then its horizontal axis.
 */
public class FlipPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        float w = size * 0.6f;
        // cos of the flip angle squashes one axis like a 3D turn; the first half-turn ends on
        // the back side and the second one brings the front back, so the dimmed side follows that
        float flip = (float) Math.cos(Math.PI * ease(wrap(fraction * 2)));
        boolean firstHalf = fraction < 0.5f;
        float sx = firstHalf ? flip : 1;
        float sy = firstHalf ? 1 : flip;
        boolean back = firstHalf ? flip < 0 : flip > 0;
        Color color = g.getColor();
        g.setColor(back ? alpha(color, 0.6f) : color);
        g.translate(size / 2, size / 2);
        g.scale(Math.max(Math.abs(sx), 0.01), Math.max(Math.abs(sy), 0.01));
        g.fill(new RoundRectangle2D.Float(-w / 2, -w / 2, w, w, w * 0.3f, w * 0.3f));
    }

    @Override
    public int getDuration() {
        return 1400;
    }
}
