package com.swingcraft4j.liquidprogress.util;

import com.formdev.flatlaf.ui.FlatUIUtils;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Static painting kept as a device-resolution image, redrawn only when its key or the transform changes.
 */
public final class ImageCache {

    private BufferedImage image;
    private Object key;
    private AffineTransform transform;

    /**
     * Draws {@code painter}'s output over {@code area}, reusing the image while {@code key} and the transform are unchanged.
     */
    public void paint(Graphics2D g, Rectangle2D area, Object key, Consumer<Graphics2D> painter) {
        AffineTransform deviceTransform = g.getTransform();
        Rectangle device = deviceTransform.createTransformedShape(area).getBounds();
        if (device.isEmpty()) {
            return;
        }
        // relative to the image's whole-pixel origin, so moving by whole pixels keeps the cache
        AffineTransform toImage = AffineTransform.getTranslateInstance(-device.x, -device.y);
        toImage.concatenate(deviceTransform);

        if (image == null || image.getWidth() != device.width || image.getHeight() != device.height
                || !Objects.equals(key, this.key) || !toImage.equals(transform)) {
            image = new BufferedImage(device.width, device.height, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D ig = image.createGraphics();
            FlatUIUtils.setRenderingHints(ig);
            ig.setTransform(toImage);
            painter.accept(ig);
            ig.dispose();
            this.key = key;
            this.transform = toImage;
        }

        Graphics2D dg = (Graphics2D) g.create();
        dg.setTransform(AffineTransform.getTranslateInstance(device.x, device.y));
        dg.drawImage(image, 0, 0, null);
        dg.dispose();
    }
}
