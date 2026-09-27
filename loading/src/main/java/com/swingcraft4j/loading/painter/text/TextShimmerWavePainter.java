package com.swingcraft4j.loading.painter.text;

import com.swingcraft4j.loading.LoadingPainter;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;

import static com.swingcraft4j.loading.LoadingPainter.alpha;
import static com.swingcraft4j.loading.painter.PainterMath.wrap;

/**
 * Text whose letters brighten and lift one after another in a wave; the font size follows the icon size.
 */
public class TextShimmerWavePainter implements LoadingPainter {

    // one letter's rise and fall, and the pause per letter before the wave repeats
    private static final int LETTER_DURATION = 1000;
    private static final int PAUSE_PER_LETTER = 50;

    private static final float FONT_SCALE = 0.5f;
    private static final float PADDING = 0.15f;

    private final String text;
    private final Font font;

    public TextShimmerWavePainter() {
        this("Loading...");
    }

    public TextShimmerWavePainter(String text) {
        this(text, null);
    }

    /**
     * Text drawn in the family and style of {@code font}; {@code null} uses the look and feel's default font.
     */
    public TextShimmerWavePainter(String text, Font font) {
        this.text = text != null && !text.isEmpty() ? text : " ";
        this.font = font;
    }

    public String getText() {
        return text;
    }

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        Font f = font(size);
        FontRenderContext frc = g.getFontRenderContext();
        LineMetrics metrics = f.getLineMetrics(text, frc);
        float em = f.getSize2D();
        float baseline = (size - metrics.getAscent() - metrics.getDescent()) / 2 + metrics.getAscent();
        float cy = size / 2;
        Color color = g.getColor();
        g.setFont(f);

        int n = text.length();
        float letter = LETTER_DURATION / (float) (LETTER_DURATION + PAUSE_PER_LETTER * n);
        AffineTransform old = g.getTransform();
        float x = em * PADDING;
        for (int i = 0; i < n; i++) {
            String ch = String.valueOf(text.charAt(i));
            float advance = (float) f.getStringBounds(ch, frc).getWidth();
            // each letter starts a little after the previous one, then rests until the wave comes round again
            float t = wrap(fraction - i * letter / n) / letter;
            float v = t < 1 ? easeInOut(1 - Math.abs(2 * t - 1)) : 0;
            if (!ch.isBlank()) {
                float cx = x + advance / 2;
                // scale 1.1 plus the 10px z-lift under 500px perspective; rotateY 10deg narrows the letter
                float scale = 1 + v * (1.1f * 500 / 490f - 1);
                float narrow = (float) Math.cos(Math.toRadians(10 * v));
                g.translate(cx + v * 0.14f * em, cy - v * 0.14f * em);
                g.scale(scale * narrow, scale);
                g.translate(-cx, -cy);
                g.setColor(alpha(color, 0.55f + 0.45f * v));
                g.drawString(ch, x, baseline);
                g.setTransform(old);
            }
            x += advance;
        }
    }

    @Override
    public float getWidth(float size) {
        Font f = font(size);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        float width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += (float) f.getStringBounds(String.valueOf(text.charAt(i)), frc).getWidth();
        }
        return width + 2 * PADDING * f.getSize2D();
    }

    /**
     * One cycle is a letter's rise and fall plus a pause that grows with the text length.
     */
    @Override
    public int getDuration() {
        return LETTER_DURATION + PAUSE_PER_LETTER * text.length();
    }

    private Font font(float size) {
        Font base = font;
        if (base == null) {
            base = UIManager.getFont("defaultFont");
        }
        if (base == null) {
            base = UIManager.getFont("Label.font");
        }
        return base.deriveFont(size * FONT_SCALE);
    }

    /**
     * Ease-in-out timing, cubic-bezier(0.42, 0, 0.58, 1).
     */
    private static float easeInOut(float x) {
        float lo = 0;
        float hi = 1;
        float s = x;
        for (int i = 0; i < 20; i++) {
            s = (lo + hi) / 2;
            if (bezier(s, 0.42f, 0.58f) < x) {
                lo = s;
            } else {
                hi = s;
            }
        }
        return bezier(s, 0, 1);
    }

    private static float bezier(float s, float p1, float p2) {
        float u = 1 - s;
        return 3 * u * u * s * p1 + 3 * u * s * s * p2 + s * s * s;
    }
}
