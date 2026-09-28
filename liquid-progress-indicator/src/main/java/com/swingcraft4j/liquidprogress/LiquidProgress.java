package com.swingcraft4j.liquidprogress;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.ColorFunctions;
import com.formdev.flatlaf.util.UIScale;
import com.swingcraft4j.liquidprogress.painter.CirclePainter;
import com.swingcraft4j.liquidprogress.painter.LiquidPainter;
import com.swingcraft4j.liquidprogress.util.Shadow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * Progress indicator drawn as liquid filling a meter, with moving waves and rising bubbles.
 */
public class LiquidProgress extends JComponent {

    /**
     * Side of the square space all lengths are given in; it's scaled to fit the component.
     */
    public static final float METER_SIZE = 300;

    private static final int DEFAULT_SIZE = 150;
    private static final int FRAME_DELAY = 16;
    // longest time step, so the animation doesn't jump after a stall
    private static final float MAX_STEP = 0.1f;
    // how much of the front color the back wave keeps, blended into the meter background
    private static final float BACK_WEIGHT = 0.4f;
    private static final float TEXT_SHADOW_BLUR = 2.5f;
    // widest the text may be, as a fraction of the fluid shape's width
    private static final float TEXT_MAX_WIDTH = 0.8f;
    private static final Color TEXT_SHADOW_COLOR = new Color(0, 0, 0, 102);
    private static final Color BUBBLE_COLOR = new Color(255, 255, 255, 220);
    private static final Color BUBBLE_FILL = new Color(255, 255, 255, 50);
    private static final int BUBBLE_COUNT = 12;
    private static final float BUBBLE_SPEED = 20;
    // diameter range is BUBBLE_SIZE to twice that
    private static final float BUBBLE_SIZE = 4;
    private static final float BUBBLE_STROKE = 1.2f;
    // smallest bubble radius on screen, in device pixels
    private static final float MIN_BUBBLE_PIXELS = 2.5f;

    // one timer drives every meter on screen, so they step and repaint together
    private static final Set<LiquidProgress> ANIMATED = new LinkedHashSet<>();
    private static final Timer TIMER = new Timer(FRAME_DELAY, e -> tickAll());
    private static long lastTick;

    private final LiquidWave frontWave = new LiquidWave(100, 12, 30, -150);
    private final LiquidWave backWave = new LiquidWave(100, 9, 30, 150);
    private final Bubble[] bubbles = new Bubble[BUBBLE_COUNT];
    private final Random random = new Random();

    private LiquidPainter painter = new CirclePainter();
    private float value;
    private float displayedValue;
    private float fillSpeed = 30;
    private float fontSize;
    private Color meterBackground;
    private Color textColor;
    private Color trackTextColor;
    private boolean textPainted = true;
    private boolean percentSignPainted;
    private IntFunction<String> textFormatter;
    private boolean bubblesPainted = true;

    // whole meter, then the liquid layer and the fluid-shape mask it's cut with, reused across frames
    private BufferedImage frame;
    private BufferedImage layer;
    private BufferedImage mask;
    private Shape maskShape;
    private AffineTransform maskTransform;
    // front liquid alone and a text layer, for text in two colors
    private BufferedImage frontMask;
    private BufferedImage textLayer;

    // text shadow is blurred once per text, not every frame
    private Shadow textShadow;
    private TextKey textShadowKey;

    public LiquidProgress() {
        this(0);
    }

    public LiquidProgress(float value) {
        for (int i = 0; i < bubbles.length; i++) {
            bubbles[i] = new Bubble();
            resetBubble(bubbles[i], 0);
        }
        setValue(value);
        setOpaque(true);
        updateUI();
        // animates only while on screen
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                updateTimer();
            }
        });
    }

    /**
     * Target fill in percent, 0 to 100; the liquid rises or falls to it at {@link #getFillSpeed()}.
     */
    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = Math.clamp(value, 0f, 100f);
        if (fillSpeed <= 0) {
            displayedValue = this.value;
        }
        repaint();
    }

    /**
     * Fill currently shown, moving toward {@link #getValue()}.
     */
    public float getDisplayedValue() {
        return displayedValue;
    }

    /**
     * How fast the liquid moves to a new value, in percent per second; 0 or less jumps right away.
     */
    public float getFillSpeed() {
        return fillSpeed;
    }

    public void setFillSpeed(float fillSpeed) {
        this.fillSpeed = fillSpeed;
        if (fillSpeed <= 0) {
            displayedValue = value;
            repaint();
        }
    }

    public LiquidPainter getPainter() {
        return painter;
    }

    /**
     * Style of the meter around the liquid; {@link CirclePainter} by default.
     */
    public void setPainter(LiquidPainter painter) {
        this.painter = painter != null ? painter : new CirclePainter();
        repaint();
    }

    /**
     * Wave in front; its color defaults to {@code Component.accentColor}.
     */
    public LiquidWave getFrontWave() {
        return frontWave;
    }

    /**
     * Wave behind; its color defaults to the front color blended into the meter background.
     */
    public LiquidWave getBackWave() {
        return backWave;
    }

    /**
     * Color the front wave is painted with: its own color, or {@code Component.accentColor}.
     */
    public Color getFrontColor() {
        return frontWave.getColor() != null ? frontWave.getColor() : UIManager.getColor("Component.accentColor");
    }

    /**
     * Color the back wave is painted with: its own color, or the front color blended into the meter background.
     */
    public Color getBackColor() {
        return backWave.getColor() != null ? backWave.getColor() : ColorFunctions.mix(getFrontColor(), getMeterBackground(), BACK_WEIGHT);
    }

    /**
     * Size of the percentage text; the painter's default unless set.
     */
    public float getFontSize() {
        return fontSize > 0 ? fontSize : painter.getFontSize(this);
    }

    /**
     * 0 or less uses the painter's default.
     */
    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
        repaint();
    }

    /**
     * Color of the text where it's over the empty track, so it stays readable above the liquid; the painter's default unless set.
     */
    public Color getTrackTextColor() {
        return trackTextColor != null ? trackTextColor : painter.getTrackTextColor(this);
    }

    /**
     * {@code null} uses the painter's default, which may be {@code null} for text in one color.
     */
    public void setTrackTextColor(Color trackTextColor) {
        this.trackTextColor = trackTextColor;
        repaint();
    }

    /**
     * Base color the ring, track and default back wave are shaded from; separate from {@link #getBackground()}, which only fills the component.
     */
    public Color getMeterBackground() {
        if (meterBackground != null) {
            return meterBackground;
        }
        Color color = UIManager.getColor("Panel.background");
        return color != null ? color : getBackground();
    }

    /**
     * {@code null} uses {@code Panel.background}, so the meter follows the theme.
     */
    public void setMeterBackground(Color meterBackground) {
        this.meterBackground = meterBackground;
        repaint();
    }

    /**
     * Color of the percentage text; {@code null} uses white.
     */
    public Color getTextColor() {
        return textColor;
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
        repaint();
    }

    public boolean isTextPainted() {
        return textPainted;
    }

    public void setTextPainted(boolean textPainted) {
        this.textPainted = textPainted;
        repaint();
    }

    public boolean isPercentSignPainted() {
        return percentSignPainted;
    }

    public void setPercentSignPainted(boolean percentSignPainted) {
        this.percentSignPainted = percentSignPainted;
        repaint();
    }

    public IntFunction<String> getTextFormatter() {
        return textFormatter;
    }

    /**
     * Text shown for the displayed percent (rounded), e.g. {@code v -> "Loading " + v + "%"}; {@code null} shows the percentage.
     */
    public void setTextFormatter(IntFunction<String> textFormatter) {
        this.textFormatter = textFormatter;
        repaint();
    }

    public boolean isBubblesPainted() {
        return bubblesPainted;
    }

    public void setBubblesPainted(boolean bubblesPainted) {
        this.bubblesPainted = bubblesPainted;
        repaint();
    }

    /**
     * Takes the panel colors from the current theme, unless set by the application.
     */
    @Override
    public void updateUI() {
        super.updateUI();
        LookAndFeel.installColors(this, "Panel.background", "Panel.foreground");
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        Insets insets = getInsets();
        int size = UIScale.scale(DEFAULT_SIZE);
        return new Dimension(size + insets.left + insets.right, size + insets.top + insets.bottom);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        Insets insets = getInsets();
        float w = getWidth() - insets.left - insets.right;
        float h = getHeight() - insets.top - insets.bottom;
        float size = Math.min(w, h);
        if (size <= 0) {
            return;
        }
        // the meter is painted into one image in software and sent to the screen with a single draw;
        // antialiased shapes drawn straight to an accelerated (e.g. Direct3D) surface cost far more
        AffineTransform screen = ((Graphics2D) g).getTransform();
        Rectangle2D meter = new Rectangle2D.Float(insets.left + (w - size) / 2, insets.top + (h - size) / 2, size, size);
        Rectangle device = screen.createTransformedShape(meter).getBounds();
        if (device.isEmpty()) {
            return;
        }
        // an opaque meter needs no alpha channel, which is cheaper to send to the screen
        int type = isOpaque() ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB_PRE;
        if (frame == null || frame.getWidth() != device.width || frame.getHeight() != device.height || frame.getType() != type) {
            frame = new BufferedImage(device.width, device.height, type);
        }
        Graphics2D g2 = frame.createGraphics();
        try {
            if (isOpaque()) {
                g2.setColor(getBackground());
            } else {
                g2.setComposite(AlphaComposite.Clear);
            }
            g2.fillRect(0, 0, device.width, device.height);
            g2.setComposite(AlphaComposite.SrcOver);
            FlatUIUtils.setRenderingHints(g2);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            // everything below is drawn in the METER_SIZE square, scaled to fit
            g2.translate(-device.x, -device.y);
            g2.transform(screen);
            g2.translate(meter.getX(), meter.getY());
            g2.scale(size / METER_SIZE, size / METER_SIZE);

            Shape fluid = painter.getFluidShape(this);
            Rectangle2D bounds = fluid.getBounds2D();
            painter.paintBackground(g2, this);
            Liquid liquid = paintLiquid(g2, fluid, bounds);
            if (textPainted) {
                paintText(g2, bounds, liquid);
            }
            painter.paintForeground(g2, this);
        } finally {
            g2.dispose();
        }

        Graphics2D sg = (Graphics2D) g.create();
        sg.setTransform(AffineTransform.getTranslateInstance(device.x, device.y));
        sg.drawImage(frame, 0, 0, null);
        sg.dispose();
    }

    /**
     * Waves and bubbles are drawn into a layer image, then cut to the fluid shape with a cached antialiased mask.
     */
    private Liquid paintLiquid(Graphics2D g2, Shape fluid, Rectangle2D bounds) {
        // the layer covers the fluid bounds in whole device pixels, so it's drawn without resampling
        AffineTransform deviceTransform = g2.getTransform();
        Rectangle device = deviceTransform.createTransformedShape(bounds).getBounds();
        device.grow(1, 1);
        if (device.isEmpty()) {
            return null;
        }
        AffineTransform toLayer = AffineTransform.getTranslateInstance(-device.x, -device.y);
        toLayer.concatenate(deviceTransform);
        updateLayer(device.width, device.height, fluid, toLayer);

        double level = bounds.getMaxY() - bounds.getHeight() * displayedValue / 100;
        Path2D frontPath = frontWave.path(bounds, level);
        Graphics2D lg = beginLayer(layer, toLayer);
        try {
            lg.setColor(getBackColor());
            lg.fill(backWave.path(bounds, level));
            lg.setColor(getFrontColor());
            lg.fill(frontPath);
            if (bubblesPainted) {
                paintBubbles(lg, frontPath, bounds, 1 / Shadow.deviceScale(g2));
            }

            // keep only what's inside the fluid shape
            lg.setTransform(new AffineTransform());
            lg.setComposite(AlphaComposite.DstIn);
            lg.drawImage(mask, 0, 0, null);
        } finally {
            lg.dispose();
        }
        drawLayer(g2, layer, device);
        return new Liquid(device, toLayer, frontPath);
    }

    /**
     * Clears a layer image and returns its graphics, drawing in meter units through {@code toLayer}.
     */
    private static Graphics2D beginLayer(BufferedImage image, AffineTransform toLayer) {
        Graphics2D lg = image.createGraphics();
        lg.setComposite(AlphaComposite.Clear);
        lg.fillRect(0, 0, image.getWidth(), image.getHeight());
        lg.setComposite(AlphaComposite.SrcOver);
        FlatUIUtils.setRenderingHints(lg);
        lg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        lg.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        lg.setTransform(toLayer);
        return lg;
    }

    /**
     * Draws a layer image 1:1 at its whole-pixel device position.
     */
    private static void drawLayer(Graphics2D g2, BufferedImage image, Rectangle device) {
        Graphics2D dg = (Graphics2D) g2.create();
        dg.setTransform(AffineTransform.getTranslateInstance(device.x, device.y));
        dg.drawImage(image, 0, 0, null);
        dg.dispose();
    }

    private static BufferedImage reuse(BufferedImage image, int w, int h) {
        return image != null && image.getWidth() == w && image.getHeight() == h
                ? image : new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
    }

    /**
     * Resizes the layer and redraws the mask only when the size, fluid shape or transform change.
     */
    private void updateLayer(int w, int h, Shape fluid, AffineTransform toLayer) {
        if (layer == null || layer.getWidth() != w || layer.getHeight() != h) {
            layer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
            mask = null;
        }
        if (mask != null && fluid.equals(maskShape) && toLayer.equals(maskTransform)) {
            return;
        }
        mask = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D mg = mask.createGraphics();
        FlatUIUtils.setRenderingHints(mg);
        mg.setTransform(toLayer);
        mg.setColor(Color.BLACK);
        mg.fill(fluid);
        mg.dispose();
        maskShape = fluid;
        maskTransform = toLayer;
    }

    /**
     * Bubbles as filled rings, clipped to the front wave; {@code pixel} is one device pixel in meter units.
     */
    private void paintBubbles(Graphics2D g2, Shape liquid, Rectangle2D bounds, double pixel) {
        Shape oldClip = g2.getClip();
        g2.clip(liquid);
        // keeps bubbles a visible size on small meters
        double stroke = Math.max(BUBBLE_STROKE, pixel);
        for (Bubble b : bubbles) {
            double x = bounds.getX() + b.x * bounds.getWidth();
            double y = bounds.getMaxY() - b.rise;
            double r = Math.max(b.r, pixel * MIN_BUBBLE_PIXELS);
            Ellipse2D inner = circle(x, y, r - stroke);
            g2.setColor(BUBBLE_FILL);
            g2.fill(inner);
            Path2D ring = new Path2D.Double(Path2D.WIND_EVEN_ODD);
            ring.append(circle(x, y, r), false);
            ring.append(inner, false);
            g2.setColor(BUBBLE_COLOR);
            g2.fill(ring);
        }
        g2.setClip(oldClip);
    }

    private static Ellipse2D circle(double x, double y, double r) {
        return new Ellipse2D.Double(x - r, y - r, r * 2, r * 2);
    }

    private void paintText(Graphics2D g2, Rectangle2D bounds, Liquid liquid) {
        int percent = Math.round(displayedValue);
        String text = textFormatter != null ? textFormatter.apply(percent) : percent + (percentSignPainted ? "%" : "");
        if (text == null || text.isEmpty()) {
            return;
        }
        Font base = getFont() != null ? getFont() : UIManager.getFont("Label.font");
        Font font = base.deriveFont(getFontSize());
        GlyphVector glyphs = font.createGlyphVector(g2.getFontRenderContext(), text);
        Rectangle2D visual = glyphs.getVisualBounds();
        // longer text shrinks to fit across the container
        double maxWidth = bounds.getWidth() * TEXT_MAX_WIDTH;
        if (visual.getWidth() > maxWidth) {
            font = font.deriveFont((float) (font.getSize2D() * maxWidth / visual.getWidth()));
            glyphs = font.createGlyphVector(g2.getFontRenderContext(), text);
            visual = glyphs.getVisualBounds();
        }
        float x = (float) (bounds.getCenterX() - visual.getCenterX());
        float y = (float) (bounds.getCenterY() - visual.getCenterY());

        double scale = Shadow.deviceScale(g2);
        TextKey key = new TextKey(text, font, scale);
        if (!key.equals(textShadowKey)) {
            textShadow = new Shadow(glyphs.getOutline(), TEXT_SHADOW_BLUR, TEXT_SHADOW_COLOR, scale);
            textShadowKey = key;
        }
        Color inside = textColor != null ? textColor : Color.WHITE;
        Color outside = getTrackTextColor();
        if (outside == null || liquid == null) {
            textShadow.paint(g2, x, y);
            g2.setColor(inside);
            g2.drawGlyphVector(glyphs, x, y);
            return;
        }

        // two colors: the text is split along the front wave with an antialiased mask of the front liquid
        int w = liquid.device.width;
        int h = liquid.device.height;
        frontMask = reuse(frontMask, w, h);
        Graphics2D mg = beginLayer(frontMask, liquid.toLayer);
        mg.setColor(Color.BLACK);
        mg.fill(liquid.front);
        mg.dispose();
        textLayer = reuse(textLayer, w, h);

        // over the track: plain text, removed where the liquid is
        Graphics2D tg = beginLayer(textLayer, liquid.toLayer);
        tg.setColor(outside);
        tg.drawGlyphVector(glyphs, x, y);
        maskLayer(tg, AlphaComposite.DstOut);
        drawLayer(g2, textLayer, liquid.device);

        // in the liquid: shadowed text, kept only where the liquid is
        tg = beginLayer(textLayer, liquid.toLayer);
        textShadow.paint(tg, x, y);
        tg.setColor(inside);
        tg.drawGlyphVector(glyphs, x, y);
        maskLayer(tg, AlphaComposite.DstIn);
        drawLayer(g2, textLayer, liquid.device);
    }

    private void maskLayer(Graphics2D tg, AlphaComposite rule) {
        tg.setTransform(new AffineTransform());
        tg.setComposite(rule);
        tg.drawImage(frontMask, 0, 0, null);
        tg.dispose();
    }

    private void updateTimer() {
        if (isShowing()) {
            ANIMATED.add(this);
            if (!TIMER.isRunning()) {
                lastTick = System.nanoTime();
                TIMER.start();
            }
        } else {
            ANIMATED.remove(this);
            // drop the images while hidden; they're rebuilt on the next paint
            frame = null;
            layer = null;
            mask = null;
            frontMask = null;
            textLayer = null;
            if (ANIMATED.isEmpty()) {
                TIMER.stop();
            }
        }
    }

    private static void tickAll() {
        long now = System.nanoTime();
        float dt = Math.min((now - lastTick) / 1e9f, MAX_STEP);
        lastTick = now;
        for (LiquidProgress p : ANIMATED) {
            p.step(dt);
        }
    }

    private void step(float dt) {
        if (fillSpeed <= 0) {
            displayedValue = value;
        } else if (displayedValue < value) {
            displayedValue = Math.min(displayedValue + fillSpeed * dt, value);
        } else if (displayedValue > value) {
            displayedValue = Math.max(displayedValue - fillSpeed * dt, value);
        }
        frontWave.step(dt);
        backWave.step(dt);

        // bubbles rise from the bottom and start over once they reach the surface
        double depth = painter.getFluidShape(this).getBounds2D().getHeight() * displayedValue / 100;
        for (Bubble b : bubbles) {
            b.rise += b.speed * dt;
            if (b.rise >= depth) {
                resetBubble(b, depth);
            }
        }
        repaint();
    }

    private void resetBubble(Bubble b, double depth) {
        b.x = random.nextDouble();
        b.rise = depth > 0 ? random.nextDouble(depth) : 0;
        b.r = random.nextDouble(BUBBLE_SIZE, BUBBLE_SIZE * 2) / 2;
        b.speed = random.nextDouble(BUBBLE_SPEED, BUBBLE_SPEED * 2);
    }

    /**
     * Bubble in the liquid: x as a fraction of the fluid width, rise as the height above its bottom.
     */
    private static class Bubble {
        double x;
        double rise;
        double r;
        double speed;
    }

    private record TextKey(String text, Font font, double scale) {
    }

    /**
     * Where the liquid layer was drawn this frame, and the front wave, for the text that follows it.
     */
    private record Liquid(Rectangle device, AffineTransform toLayer, Path2D front) {
    }
}
