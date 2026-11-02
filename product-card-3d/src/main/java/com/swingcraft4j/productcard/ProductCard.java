package com.swingcraft4j.productcard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Product card that tilts in 3D toward the mouse while its product image pops out of the card.
 */
public class ProductCard extends JComponent {

    /** Which way the toe (front) of the product points in the image; on hover the card turns it up. */
    public enum Facing {LEFT, RIGHT}

    // design size of the card; everything is drawn in these units, then scaled by cardScale
    private static final int CARD_WIDTH = 230;
    private static final int CARD_HEIGHT = 320;
    private static final int ARC = 22;
    private static final int MARGIN_X = 64;
    private static final int MARGIN_Y = 48;
    private static final int GLOW = 36;
    // room kept around the card when space is short, and the smallest size the card shrinks to
    private static final int MIN_MARGIN = 12;
    private static final double MIN_FIT = 0.5;
    private static final double CAMERA = 900;
    private static final double PRODUCT_WIDTH = CARD_WIDTH;
    private static final double PRODUCT_HEIGHT = 185;
    private static final double PRODUCT_GROW = 0.08;

    private String number;
    private String category;
    private String productName;
    private String price;
    private Color cardColor;
    private Image productImage;
    private Facing facing = Facing.LEFT;
    private double maxTilt = 14;
    private double depth = 1;
    private double cardScale = 1;

    private final Timer timer = new Timer(15, e -> step());
    private final BufferedImage[] layers = new BufferedImage[3];
    private BufferedImage canvas;
    private BufferedImage glow;
    private double glowScale;
    private BufferedImage scaledProduct;
    private double scaledProductScale;

    // animated state and its targets: hover 0-1, tilt -1..1 per axis, press 0-1
    private double hover, tiltX, tiltY, press;
    private double hoverTarget, tiltXTarget, tiltYTarget, pressTarget;

    /** Creates an empty blue card; fill it with the setters. */
    public ProductCard() {
        this("", "", "", "", new Color(0x1e88e5), null);
    }

    public ProductCard(String number, String category, String productName, String price, Color cardColor, Image productImage) {
        this.number = text(number);
        this.category = text(category);
        this.productName = text(productName);
        this.price = text(price);
        this.cardColor = Objects.requireNonNull(cardColor, "cardColor");
        this.productImage = productImage;
        setOpaque(false);
        setForeground(Color.WHITE);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                track(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                track(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverTarget = tiltXTarget = tiltYTarget = pressTarget = 0;
                setCursor(null);
                timer.start();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled() && SwingUtilities.isLeftMouseButton(e) && cardBounds().contains(e.getPoint())) {
                    pressTarget = 1;
                    timer.start();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                boolean click = pressTarget == 1 && cardBounds().contains(e.getPoint());
                pressTarget = 0;
                timer.start();
                if (click) {
                    fireActionPerformed();
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    // ---- content ----

    public String getNumber() {
        return number;
    }

    /** Big faded text at the top, e.g. "01". */
    public void setNumber(String number) {
        String old = this.number;
        this.number = text(number);
        changed("number", old, this.number, false);
    }

    public String getCategory() {
        return category;
    }

    /** Large faded word under the number, e.g. "SNEAKERS". */
    public void setCategory(String category) {
        String old = this.category;
        this.category = text(category);
        changed("category", old, this.category, false);
    }

    public String getProductName() {
        return productName;
    }

    /** Name at the bottom of the card; also the action command of click events. */
    public void setProductName(String productName) {
        String old = this.productName;
        this.productName = text(productName);
        changed("productName", old, this.productName, false);
    }

    public String getPrice() {
        return price;
    }

    /** Text of the price pill, e.g. "$39.9"; empty hides the pill. */
    public void setPrice(String price) {
        String old = this.price;
        this.price = text(price);
        changed("price", old, this.price, false);
    }

    public Color getCardColor() {
        return cardColor;
    }

    /** Base color of the card; the gradient and the glow under the card are made from it. */
    public void setCardColor(Color cardColor) {
        Color old = this.cardColor;
        this.cardColor = Objects.requireNonNull(cardColor, "cardColor");
        glow = null;
        changed("cardColor", old, cardColor, false);
    }

    public Image getProductImage() {
        return productImage;
    }

    /** Product picture, best a transparent PNG cropped to the product; {@code null} shows none. */
    public void setProductImage(Image productImage) {
        Image old = this.productImage;
        this.productImage = productImage;
        scaledProduct = null;
        changed("productImage", old, productImage, false);
    }

    // ---- effect ----

    public Facing getFacing() {
        return facing;
    }

    /** Which way the toe points in the product image (default {@link Facing#LEFT}). */
    public void setFacing(Facing facing) {
        Facing old = this.facing;
        this.facing = Objects.requireNonNull(facing, "facing");
        changed("facing", old, facing, false);
    }

    public double getMaxTilt() {
        return maxTilt;
    }

    /** Largest tilt angle in degrees, reached at the card edges (default 14; 0 keeps the card flat). */
    public void setMaxTilt(double maxTilt) {
        double old = this.maxTilt;
        this.maxTilt = Math.max(0, Math.min(45, maxTilt));
        changed("maxTilt", old, this.maxTilt, false);
    }

    public double getDepth() {
        return depth;
    }

    /** How far the title and the product pop out of the card on hover (default 1; 0 = no pop out). */
    public void setDepth(double depth) {
        double old = this.depth;
        this.depth = Math.max(0, Math.min(3, depth));
        changed("depth", old, this.depth, false);
    }

    public double getCardScale() {
        return cardScale;
    }

    /** Size of the card relative to its design size of 230 x 320 (default 1). */
    public void setCardScale(double cardScale) {
        double old = this.cardScale;
        this.cardScale = Math.max(0.25, Math.min(4, cardScale));
        changed("cardScale", old, this.cardScale, true);
    }

    // ---- events ----

    /** Adds a listener called when the card is clicked. */
    public void addActionListener(ActionListener listener) {
        listenerList.add(ActionListener.class, listener);
    }

    public void removeActionListener(ActionListener listener) {
        listenerList.remove(ActionListener.class, listener);
    }

    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    protected void fireActionPerformed() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, productName);
        for (ActionListener listener : getActionListeners()) {
            listener.actionPerformed(event);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return new Dimension((int) Math.ceil((CARD_WIDTH + MARGIN_X * 2) * cardScale),
                (int) Math.ceil((CARD_HEIGHT + MARGIN_Y * 2) * cardScale));
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return new Dimension((int) Math.ceil((CARD_WIDTH + MIN_MARGIN * 2) * cardScale * MIN_FIT),
                (int) Math.ceil((CARD_HEIGHT + MIN_MARGIN * 2) * cardScale * MIN_FIT));
    }

    /** Card scale that fits the current size: the room around the card shrinks first, then the card itself. */
    private double fitScale() {
        double k = Math.min(getWidth() / ((CARD_WIDTH + MIN_MARGIN * 2) * cardScale),
                getHeight() / ((CARD_HEIGHT + MIN_MARGIN * 2) * cardScale));
        return cardScale * Math.max(0, Math.min(1, k));
    }

    /** 1 with the full room around the card, down to 0 at the smallest room; the pop out shrinks with it. */
    private double room(double unit) {
        double mx = (getWidth() / unit - CARD_WIDTH) / 2;
        double my = (getHeight() / unit - CARD_HEIGHT) / 2;
        double rx = (mx - MIN_MARGIN) / (MARGIN_X - MIN_MARGIN);
        double ry = (my - MIN_MARGIN) / (MARGIN_Y - MIN_MARGIN);
        return Math.max(0, Math.min(1, Math.min(rx, ry)));
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            hoverTarget = tiltXTarget = tiltYTarget = pressTarget = 0;
            setCursor(null);
            timer.start();
        }
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        repaint();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    private void changed(String property, Object old, Object value, boolean resized) {
        firePropertyChange(property, old, value);
        if (resized) {
            revalidate();
        }
        repaint();
    }

    /** The card at rest in component coordinates; the mouse is tracked inside it. */
    private Rectangle2D cardBounds() {
        double unit = fitScale();
        double w = CARD_WIDTH * unit;
        double h = CARD_HEIGHT * unit;
        return new Rectangle2D.Double(Math.round((getWidth() - w) / 2), Math.round((getHeight() - h) / 2), w, h);
    }

    private void track(MouseEvent e) {
        Rectangle2D card = cardBounds();
        boolean inside = isEnabled() && card.contains(e.getPoint());
        hoverTarget = inside ? 1 : 0;
        tiltXTarget = inside ? clamp((e.getX() - card.getCenterX()) / (card.getWidth() / 2)) : 0;
        tiltYTarget = inside ? clamp((e.getY() - card.getCenterY()) / (card.getHeight() / 2)) : 0;
        if (!inside) {
            pressTarget = 0;
        }
        setCursor(inside ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : null);
        timer.start();
    }

    private void step() {
        hover = approach(hover, hoverTarget, 0.12);
        tiltX = approach(tiltX, tiltXTarget, 0.10);
        tiltY = approach(tiltY, tiltYTarget, 0.10);
        press = approach(press, pressTarget, 0.25);
        if (hover == hoverTarget && tiltX == tiltXTarget && tiltY == tiltYTarget && press == pressTarget) {
            timer.stop();
        }
        repaint();
    }

    // ---- painting ----

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        AffineTransform t = g2.getTransform();
        double scale = t.getScaleX();
        int dw = (int) Math.ceil(getWidth() * scale);
        int dh = (int) Math.ceil(getHeight() * scale);
        if (dw <= 0 || dh <= 0) {
            g2.dispose();
            return;
        }
        if (canvas == null || canvas.getWidth() != dw || canvas.getHeight() != dh) {
            canvas = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_ARGB_PRE);
        }
        int[] pixels = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();
        Arrays.fill(pixels, 0);

        // layers are drawn in design units at this scale, so they stay sharp on HiDPI screens
        double unit = fitScale();
        double ls = scale * unit;
        double room = room(unit);
        // a shrunk card caches its glow and product image in 2.5% steps, so resizing doesn't rebuild them on every pixel
        double cs = unit < cardScale ? Math.pow(1.025, Math.round(Math.log(ls) / Math.log(1.025))) : ls;
        Rectangle2D card = cardBounds();
        double tilt = Math.toRadians(maxTilt);
        View view = new View(card.getCenterX(), card.getCenterY(), scale, unit,
                tiltX * tilt, -tiltY * tilt, -24 * press);
        double h = ease(hover);
        double d = h * depth * room;
        double pop = h * room;

        // colored glow under the card, further back and lower as the card lifts
        view.draw(pixels, dw, dh, glow(cs), cs, -CARD_WIDTH / 2.0 - GLOW, -CARD_HEIGHT / 2.0 - GLOW + 12 + 14 * h,
                -30 * d, (int) (256 * (0.45 + 0.45 * h) * (0.3 + 0.7 * room)));

        BufferedImage cardLayer = layer(0, CARD_WIDTH, CARD_HEIGHT, ls, lg -> paintCard(lg, h));
        view.draw(pixels, dw, dh, cardLayer, ls, -CARD_WIDTH / 2.0, -CARD_HEIGHT / 2.0, 0, 256);

        BufferedImage titleLayer = layer(1, CARD_WIDTH, CARD_HEIGHT, ls, lg -> paintTitle(lg, h));
        view.draw(pixels, dw, dh, titleLayer, ls, -CARD_WIDTH / 2.0, -CARD_HEIGHT / 2.0, 30 * d, 256);

        if (productImage != null) {
            double pw = CARD_WIDTH + MARGIN_X * 2;
            double ph = CARD_HEIGHT * 0.85 + MARGIN_Y;
            BufferedImage productLayer = layer(2, pw, ph, ls, lg -> paintProduct(lg, pop, cs));
            view.draw(pixels, dw, dh, productLayer, ls, -pw / 2, -CARD_HEIGHT / 2.0 - MARGIN_Y, 60 * d, 256);
        }

        g2.setTransform(new AffineTransform(1, 0, 0, 1, t.getTranslateX(), t.getTranslateY()));
        g2.drawImage(canvas, 0, 0, null);
        g2.dispose();
    }

    /** Card face: gradient, a light that follows the mouse, the product shadow, the price and the name. */
    private void paintCard(Graphics2D g, double h) {
        Shape shape = new RoundRectangle2D.Double(0, 0, CARD_WIDTH, CARD_HEIGHT, ARC, ARC);
        g.setPaint(new GradientPaint(0, 0, mix(cardColor, Color.WHITE, 0.16), 0, CARD_HEIGHT, mix(cardColor, Color.BLACK, 0.12)));
        g.fill(shape);
        g.clip(shape);

        if (h > 0) {
            float lx = (float) ((tiltX + 1) / 2 * CARD_WIDTH);
            float ly = (float) ((tiltY + 1) / 2 * CARD_HEIGHT);
            g.setPaint(new RadialGradientPaint(lx, ly, CARD_WIDTH, new float[]{0, 1},
                    new Color[]{new Color(255, 255, 255, (int) (70 * h)), new Color(255, 255, 255, 0)}));
            g.fill(shape);
        }

        // soft shadow of the product on the card, spreading as the product lifts
        if (productImage != null) {
            double sx = CARD_WIDTH / 2.0 + 4 - tiltX * 10 * h;
            double sy = CARD_HEIGHT * 0.6 + 6 * h - tiltY * 6 * h;
            double sw = CARD_WIDTH * (0.78 + 0.12 * h);
            double sh = 26 + 10 * h;
            Graphics2D sg = (Graphics2D) g.create();
            sg.translate(sx, sy);
            sg.scale(1, sh / sw);
            sg.setPaint(new RadialGradientPaint(0, 0, (float) (sw / 2), new float[]{0, 1},
                    new Color[]{new Color(0, 0, 0, (int) (110 - 50 * h)), new Color(0, 0, 0, 0)}));
            sg.fill(new Ellipse2D.Double(-sw / 2, -sw / 2, sw, sw));
            sg.dispose();
        }

        // darker band behind the name
        g.setColor(new Color(0, 0, 0, 26));
        g.fill(new Rectangle2D.Double(0, CARD_HEIGHT - 62, CARD_WIDTH, 62));

        Font base = baseFont();
        Color text = getForeground();
        if (!price.isEmpty()) {
            Font priceFont = base.deriveFont(Font.BOLD, 13f);
            FontMetrics pm = g.getFontMetrics(priceFont);
            int pw = pm.stringWidth(price) + 22;
            int ph = 26;
            int px = CARD_WIDTH - pw - 16;
            int py = CARD_HEIGHT - 62 - ph - 12;
            g.setColor(withAlpha(text, 0.18));
            g.fill(new RoundRectangle2D.Double(px, py, pw, ph, ph, ph));
            g.setColor(text);
            g.setFont(priceFont);
            g.drawString(price, px + 11, py + (ph - pm.getHeight()) / 2 + pm.getAscent());
        }

        Font nameFont = base.deriveFont(Font.BOLD, 19f);
        FontMetrics nm = g.getFontMetrics(nameFont);
        g.setColor(text);
        g.setFont(nameFont);
        g.drawString(productName, (CARD_WIDTH - nm.stringWidth(productName)) / 2, CARD_HEIGHT - 31 + nm.getAscent() / 2 - 2);
    }

    /** The big faded number and category; they brighten when hovered. */
    private void paintTitle(Graphics2D g, double h) {
        g.clip(new RoundRectangle2D.Double(0, 0, CARD_WIDTH, CARD_HEIGHT, ARC, ARC));
        Font base = baseFont();
        Color text = getForeground();
        Color color = mix(mix(cardColor, text, 0.55), text, h);
        g.setFont(base.deriveFont(Font.BOLD, 66f));
        g.setColor(withAlpha(color, 0.45 + 0.55 * h));
        g.drawString(number, 20, 78);
        g.setFont(base.deriveFont(Font.BOLD, 40f));
        g.setColor(withAlpha(color, 0.35 + 0.45 * h));
        g.drawString(category, 20, 124);
    }

    /** The product image; it grows, turns toe up and rises out of the card as {@code h} goes from 0 to 1. */
    private void paintProduct(Graphics2D g, double h, double cs) {
        BufferedImage image = scaledProduct(cs);
        double side = facing == Facing.LEFT ? 1 : -1;
        double k = (1 + PRODUCT_GROW * (h - 1)) / cs;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.translate(MARGIN_X + CARD_WIDTH / 2.0 + side * (4 - 4 * h), MARGIN_Y + CARD_HEIGHT * 0.47 - 18 * h * depth);
        g.rotate(Math.toRadians(side * 8 * h));
        g.scale(k, k);
        g.drawImage(image, -image.getWidth() / 2, -image.getHeight() / 2, null);
    }

    private Font baseFont() {
        Font font = getFont();
        if (font == null) {
            font = UIManager.getFont("defaultFont");
        }
        return font != null ? font : new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    }

    /** The product scaled once to its hovered size at scale {@code ls}, in halving steps for a smooth result. */
    private BufferedImage scaledProduct(double ls) {
        if (scaledProduct != null && scaledProductScale == ls) {
            return scaledProduct;
        }
        int iw = Math.max(1, productImage.getWidth(null));
        int ih = Math.max(1, productImage.getHeight(null));
        double fit = Math.min(PRODUCT_WIDTH / iw, PRODUCT_HEIGHT / ih) * (1 + PRODUCT_GROW) * ls;
        int tw = Math.max(1, (int) Math.round(iw * fit));
        int th = Math.max(1, (int) Math.round(ih * fit));
        BufferedImage image = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = image.createGraphics();
        g.drawImage(productImage, 0, 0, null);
        g.dispose();
        do {
            int w = image.getWidth() < tw ? tw : Math.max(tw, image.getWidth() / 2);
            int h = image.getWidth() < tw ? th : Math.max(th, image.getHeight() / 2);
            BufferedImage next = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
            g = next.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(image, 0, 0, w, h, null);
            g.dispose();
            image = next;
        } while (image.getWidth() != tw || image.getHeight() != th);
        scaledProduct = image;
        scaledProductScale = ls;
        return image;
    }

    /** Returns layer {@code index} cleared and repainted in design units at the layer scale. */
    private BufferedImage layer(int index, double width, double height, double ls, Consumer<Graphics2D> painter) {
        int w = (int) Math.round(width * ls);
        int h = (int) Math.round(height * ls);
        BufferedImage image = layers[index];
        if (image == null || image.getWidth() != w || image.getHeight() != h) {
            image = layers[index] = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        } else {
            Arrays.fill(((DataBufferInt) image.getRaster().getDataBuffer()).getData(), 0);
        }
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(ls, ls);
        painter.accept(g);
        g.dispose();
        return image;
    }

    /** Blurred card shape in the card color, made once per scale. */
    private BufferedImage glow(double ls) {
        if (glow != null && glowScale == ls) {
            return glow;
        }
        int w = (int) Math.round((CARD_WIDTH + GLOW * 2) * ls);
        int h = (int) Math.round((CARD_HEIGHT + GLOW * 2) * ls);
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(ls, ls);
        g.setColor(cardColor);
        g.fill(new RoundRectangle2D.Double(GLOW + 8, GLOW + 8, CARD_WIDTH - 16, CARD_HEIGHT - 16, ARC, ARC));
        g.dispose();
        int[] p = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        int radius = Math.max(1, (int) Math.round(GLOW * ls / 3));
        for (int i = 0; i < 3; i++) {
            boxBlur(p, w, h, radius, true);
            boxBlur(p, w, h, radius, false);
        }
        glow = image;
        glowScale = ls;
        return image;
    }

    private static void boxBlur(int[] p, int w, int h, int r, boolean horizontal) {
        int len = horizontal ? w : h;
        int lines = horizontal ? h : w;
        int[] line = new int[len];
        int div = r * 2 + 1;
        for (int l = 0; l < lines; l++) {
            for (int i = 0; i < len; i++) {
                line[i] = p[horizontal ? l * w + i : i * w + l];
            }
            for (int i = 0; i < len; i++) {
                int a = 0, rr = 0, gg = 0, b = 0;
                for (int j = -r; j <= r; j++) {
                    int idx = i + j;
                    if (idx >= 0 && idx < len) {
                        int c = line[idx];
                        a += c >>> 24;
                        rr += (c >> 16) & 0xff;
                        gg += (c >> 8) & 0xff;
                        b += c & 0xff;
                    }
                }
                p[horizontal ? l * w + i : i * w + l] = (a / div) << 24 | (rr / div) << 16 | (gg / div) << 8 | b / div;
            }
        }
    }

    /** Camera: rotates design-unit points around the card center and projects them to device pixels. */
    private record View(double cx, double cy, double scale, double unit, double rotY, double rotX, double z) {

        void draw(int[] dst, int dw, int dh, BufferedImage layer, double ls, double x, double y, double depth, int opacity) {
            double w = layer.getWidth() / ls;
            double h = layer.getHeight() / ls;
            double[] q = new double[8];
            project(x, y, depth, q, 0);
            project(x + w, y, depth, q, 2);
            project(x + w, y + h, depth, q, 4);
            project(x, y + h, depth, q, 6);
            Perspective.warp(dst, dw, dh, layer, q, opacity);
        }

        private void project(double x, double y, double depth, double[] q, int i) {
            double z0 = depth + z;
            double x1 = x * Math.cos(rotY) + z0 * Math.sin(rotY);
            double z1 = -x * Math.sin(rotY) + z0 * Math.cos(rotY);
            double y2 = y * Math.cos(rotX) - z1 * Math.sin(rotX);
            double z2 = y * Math.sin(rotX) + z1 * Math.cos(rotX);
            double s = CAMERA / (CAMERA - z2) * unit;
            q[i] = (cx + x1 * s) * scale;
            q[i + 1] = (cy + y2 * s) * scale;
        }
    }

    private static String text(String s) {
        return s == null ? "" : s;
    }

    private static double approach(double value, double target, double speed) {
        double next = value + (target - value) * speed;
        return Math.abs(target - next) < 0.001 ? target : next;
    }

    private static double ease(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double clamp(double v) {
        return Math.max(-1, Math.min(1, v));
    }

    private static Color mix(Color a, Color b, double t) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static Color withAlpha(Color c, double alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.round(255 * Math.max(0, Math.min(1, alpha))));
    }
}
