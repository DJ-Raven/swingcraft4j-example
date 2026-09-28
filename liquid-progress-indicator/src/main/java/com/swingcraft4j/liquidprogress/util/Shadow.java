package com.swingcraft4j.liquidprogress.util;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Blurred shadow of a shape, rendered once at a device scale and drawn as an image.
 */
public final class Shadow {

    private static final int PASSES = 3;

    private final BufferedImage image;
    private final double x;
    private final double y;

    /**
     * Shadow of {@code shape} blurred like a Gaussian with standard deviation {@code blur}, in the shape's units.
     */
    public Shadow(Shape shape, float blur, Color color, double scale) {
        Rectangle2D bounds = shape.getBounds2D();
        int pad = (int) Math.ceil(blur * scale * 3);
        int w = (int) Math.ceil(bounds.getWidth() * scale) + pad * 2;
        int h = (int) Math.ceil(bounds.getHeight() * scale) + pad * 2;

        BufferedImage mask = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = mask.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(pad, pad);
        g.scale(scale, scale);
        g.translate(-bounds.getX(), -bounds.getY());
        g.setColor(Color.BLACK);
        g.fill(shape);
        g.dispose();

        // pixels are read and written in place, not copied through getRGB/setRGB
        int[] pixels = ((DataBufferInt) mask.getRaster().getDataBuffer()).getData();
        int[] alpha = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            alpha[i] = pixels[i] >>> 24;
        }
        int[] temp = new int[alpha.length];
        for (int size : boxSizes(blur * scale)) {
            int r = (size - 1) / 2;
            blurH(alpha, temp, w, h, r);
            blurV(temp, alpha, w, h, r);
        }
        // premultiplied, which blends faster when drawn every frame
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        int[] out = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < out.length; i++) {
            int a = alpha[i] * color.getAlpha() / 255;
            out[i] = a << 24 | (color.getRed() * a / 255) << 16 | (color.getGreen() * a / 255) << 8 | color.getBlue() * a / 255;
        }

        this.image = image;
        this.x = bounds.getX() - pad / scale;
        this.y = bounds.getY() - pad / scale;
    }

    /**
     * Draws the shadow moved by {@code dx}, {@code dy} from the shape, 1:1 at the nearest device pixel.
     */
    public void paint(Graphics2D g, double dx, double dy) {
        // the image is already at device scale, so resampling it would only cost time
        Point2D p = g.getTransform().transform(new Point2D.Double(x + dx, y + dy), null);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setTransform(AffineTransform.getTranslateInstance(Math.round(p.getX()), Math.round(p.getY())));
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
    }

    /**
     * Device scale of the graphics, used to render shadows at full resolution.
     */
    public static double deviceScale(Graphics2D g) {
        AffineTransform t = g.getTransform();
        return Math.max(Math.hypot(t.getScaleX(), t.getShearY()), 0.01);
    }

    /**
     * Widths of the box blurs that together approximate a Gaussian blur.
     */
    private static int[] boxSizes(double sigma) {
        double ideal = Math.sqrt(12 * sigma * sigma / PASSES + 1);
        int lower = (int) Math.floor(ideal);
        if (lower % 2 == 0) {
            lower--;
        }
        int upper = lower + 2;
        double m = (12 * sigma * sigma - PASSES * lower * lower - 4 * PASSES * lower - 3 * PASSES) / (-4.0 * lower - 4);
        int[] sizes = new int[PASSES];
        for (int i = 0; i < PASSES; i++) {
            sizes[i] = i < Math.round(m) ? lower : upper;
        }
        return sizes;
    }

    private static void blurH(int[] in, int[] out, int w, int h, int r) {
        int div = r * 2 + 1;
        for (int y = 0; y < h; y++) {
            int row = y * w;
            int sum = 0;
            for (int i = 0; i < Math.min(r, w); i++) {
                sum += in[row + i];
            }
            for (int x = 0; x < w; x++) {
                if (x + r < w) {
                    sum += in[row + x + r];
                }
                out[row + x] = (sum + div / 2) / div;
                if (x - r >= 0) {
                    sum -= in[row + x - r];
                }
            }
        }
    }

    private static void blurV(int[] in, int[] out, int w, int h, int r) {
        int div = r * 2 + 1;
        for (int x = 0; x < w; x++) {
            int sum = 0;
            for (int i = 0; i < Math.min(r, h); i++) {
                sum += in[i * w + x];
            }
            for (int y = 0; y < h; y++) {
                if (y + r < h) {
                    sum += in[(y + r) * w + x];
                }
                out[y * w + x] = (sum + div / 2) / div;
                if (y - r >= 0) {
                    sum -= in[(y - r) * w + x];
                }
            }
        }
    }
}
