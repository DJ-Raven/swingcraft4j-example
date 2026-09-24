package com.swingcraft4j.profile;

import com.formdev.flatlaf.util.UIScale;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Fixed-size circular avatar; shows a faint circle until an image is set, so layout stays stable while loading.
 */
public class AvatarIcon implements Icon {

    private final int size;
    private BufferedImage image;

    public AvatarIcon(int size) {
        this.size = size;
    }

    /**
     * Sets the photo (center-cropped to a square, downscaled if large); the owning component must repaint.
     */
    public void setImage(BufferedImage image) {
        if (image == null) {
            this.image = null;
            return;
        }
        int side = Math.min(image.getWidth(), image.getHeight());
        BufferedImage square = image.getSubimage((image.getWidth() - side) / 2, (image.getHeight() - side) / 2, side, side);
        int target = UIScale.scale(size) * 2;
        if (side <= target) {
            this.image = square;
            return;
        }
        BufferedImage scaled = new BufferedImage(target, target, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(square, 0, 0, target, target, null);
        } finally {
            g2.dispose();
        }
        this.image = scaled;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            float d = getIconWidth();
            if (image != null) {
                g2.setPaint(new TexturePaint(image, new Rectangle2D.Float(x, y, d, d)));
            } else {
                Color fg = c != null ? c.getForeground() : Color.GRAY;
                g2.setPaint(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 30));
            }
            g2.fill(new Ellipse2D.Float(x, y, d, d));
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return UIScale.scale(size);
    }

    @Override
    public int getIconHeight() {
        return UIScale.scale(size);
    }
}
