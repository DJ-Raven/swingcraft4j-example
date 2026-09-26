package com.swingcraft4j.badgenotification;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.AnimatedIcon;
import com.formdev.flatlaf.util.Animator;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Paints and animates a {@link Badge} over a corner of the view, reserving room for it to overhang.
 */
class BadgeLayerUI<V extends JComponent> extends LayerUI<V> {

    private static final float MAX_PULSE = 1.2f;
    private static final float RIPPLE_GROW = 6f;
    private static final float RIPPLE_WIDTH = 1.5f;
    private static final String VISUAL_PADDING = "visualPadding";

    private final Badge badge;
    private final ShowAnimation showAnimation = new ShowAnimation();
    private final AlertAnimation alertAnimation = new AlertAnimation();
    private float showValue;
    private int iconWidth;
    private int iconHeight;

    BadgeLayerUI(Badge badge) {
        this.badge = badge;
    }

    // ---- layout: the view is shifted away from the badge's corner to leave room for it to overhang ----

    @Override
    public void doLayout(JLayer<? extends V> l) {
        super.doLayout(l);
        Insets o = overhang(l);
        l.getView().setBounds(o.left, o.top, l.getWidth() - o.left - o.right, l.getHeight() - o.top - o.bottom);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return grow(c, view(c).getPreferredSize());
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return grow(c, view(c).getMinimumSize());
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
        return grow(c, view(c).getMaximumSize());
    }

    @Override
    public int getBaseline(JComponent c, int width, int height) {
        Insets o = overhang(c);
        // layouts may ask before the layer has a size (e.g. width 0), so never pass a negative size
        int baseline = view(c).getBaseline(
                Math.max(0, width - o.left - o.right), Math.max(0, height - o.top - o.bottom));
        return baseline >= 0 ? baseline + o.top : baseline;
    }

    private Dimension grow(JComponent c, Dimension size) {
        Insets o = overhang(c);
        // MigLayout aligns the view itself and lets the reserved room spill into the surrounding gaps
        c.putClientProperty(VISUAL_PADDING, o);
        return new Dimension(clampAdd(size.width, o.left + o.right), clampAdd(size.height, o.top + o.bottom));
    }

    private static int clampAdd(int size, int add) {
        return size > Integer.MAX_VALUE - add ? Integer.MAX_VALUE : size + add;
    }

    /** Room around the view: enough on the badge corner's two sides for the pulse and ripple, plus the margin. */
    private Insets overhang(JComponent c) {
        JComponent view = view(c);
        float half = (textHeight() + ring() * 2) / 2;
        float extent = Math.max(half * MAX_PULSE, half + UIScale.scale(RIPPLE_GROW + RIPPLE_WIDTH / 2));
        float corner = cornerInset(view);
        int vertical = Math.max(0, (int) Math.ceil(extent - corner - offsetY()));
        int horizontal = Math.max(0, (int) Math.ceil(extent - corner - offsetX()));
        boolean top = isTop();
        boolean left = isLeft(view);
        Insets m = margin();
        return new Insets(
                m.top + (top ? vertical : 0),
                m.left + (left ? horizontal : 0),
                m.bottom + (top ? 0 : vertical),
                m.right + (left ? 0 : horizontal));
    }

    private boolean isTop() {
        Badge.Position p = badge.getPosition();
        return p == Badge.Position.TOP_TRAILING || p == Badge.Position.TOP_LEADING;
    }

    private boolean isLeft(JComponent view) {
        Badge.Position p = badge.getPosition();
        boolean leading = p == Badge.Position.TOP_LEADING || p == Badge.Position.BOTTOM_LEADING;
        return leading == view.getComponentOrientation().isLeftToRight();
    }

    /** Distance from the view's bounds to the point where its visible (rounded) outline turns the corner. */
    private static float cornerInset(JComponent view) {
        float focusWidth = FlatUIUtils.getBorderFocusWidth(view);
        float arc = FlatUIUtils.getBorderArc(view);
        return focusWidth + arc / 2 * (1 - (float) Math.sqrt(0.5));
    }

    @SuppressWarnings("unchecked")
    private JComponent view(JComponent c) {
        return ((JLayer<V>) c).getView();
    }

    // ---- painting ----

    @Override
    public void paint(Graphics g, JComponent c) {
        // paint the children directly: JLayer reports the view's border as its own, so letting the layer
        // paint itself would draw that border a second time around the larger layer bounds
        JLayer<?> l = (JLayer<?>) c;
        paintChild(g, l.getView());
        if (l.getGlassPane().isVisible()) {
            paintChild(g, l.getGlassPane());
        }
        iconWidth = c.getWidth();
        iconHeight = c.getHeight();
        showAnimation.paintIcon(c, g, 0, 0);
    }

    private static void paintChild(Graphics g, Component child) {
        if (child == null) {
            return;
        }
        Graphics cg = g.create(child.getX(), child.getY(), child.getWidth(), child.getHeight());
        try {
            child.paint(cg);
        } finally {
            cg.dispose();
        }
    }

    private void paintBadge(JComponent c, Graphics g, float show, float alert) {
        if (show <= 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FlatUIUtils.setRenderingHints(g2);
            JComponent view = view(c);
            Font font = font(c);
            FontMetrics fm = c.getFontMetrics(font);
            String text = badge.getText();
            boolean dot = badge.isDot();

            float ring = ring();
            float bh = dot ? UIScale.scale(value(badge.dotDiameter, -1, "Badge.dotDiameter", 8)) : textHeight();
            float bw = dot ? bh : Math.max(bh, fm.stringWidth(text) + bh * 0.7f);

            // the badge's outer end is centered on the view's visible corner, wider text grows inward
            float corner = cornerInset(view);
            boolean left = isLeft(view);
            float capX = left
                    ? view.getX() + corner + offsetX()
                    : view.getX() + view.getWidth() - corner - offsetX();
            float cy = isTop()
                    ? view.getY() + corner + offsetY()
                    : view.getY() + view.getHeight() - corner - offsetY();
            float cx = left ? capX + (bw - bh) / 2 : capX - (bw - bh) / 2;

            // alert: pulse, wobble that settles, and a fading ripple; both are damped by the badge's
            // aspect so a wide text badge moves as many pixels as a round one and stays centered/unclipped
            float round = bh / bw;
            float pulse = 1 + (MAX_PULSE - 1) * round * (float) Math.sin(Math.PI * alert);
            float wobble = (float) Math.sin(alert * Math.PI * 4) * (1 - alert) * 0.25f * round;
            Color bg = color(badge.background, "Badge.background", new Color(0xE5484D));

            g2.setComposite(AlphaComposite.SrcOver.derive(Math.min(show, 1)));
            g2.translate(cx, cy);
            if (alert > 0) {
                float grow = UIScale.scale(RIPPLE_GROW) * alert;
                g2.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), Math.round(160 * (1 - alert))));
                g2.setStroke(new BasicStroke(UIScale.scale(RIPPLE_WIDTH)));
                g2.draw(pill(bw + ring * 2 + grow * 2, bh + ring * 2 + grow * 2));
            }
            g2.rotate(wobble);
            float scale = show * pulse;
            g2.scale(scale, scale);

            Container parent = c.getParent();
            g2.setColor(color(badge.borderColor, "Badge.borderColor", parent != null ? parent.getBackground() : c.getBackground()));
            g2.fill(pill(bw + ring * 2, bh + ring * 2));
            g2.setColor(bg);
            g2.fill(pill(bw, bh));

            if (!dot) {
                g2.setColor(color(badge.foreground, "Badge.foreground", Color.WHITE));
                g2.setFont(font);
                // center on the ink (a "7" sits off-center in its advance) and snap to a device pixel once,
                // as rounding twice (here and when rasterizing at the fractional badge center) drifts it
                Rectangle2D ink = font.createGlyphVector(g2.getFontRenderContext(), text).getVisualBounds();
                Point2D p = new Point2D.Double(-ink.getCenterX(), (fm.getAscent() - fm.getDescent()) / 2f);
                AffineTransform t = g2.getTransform();
                t.transform(p, p);
                p.setLocation(Math.round(p.getX()), Math.round(p.getY()));
                try {
                    t.inverseTransform(p, p);
                } catch (NoninvertibleTransformException ex) {
                    // scaled to zero while hiding, nothing visible to draw
                    return;
                }
                g2.translate(p.getX(), p.getY());
                FlatUIUtils.drawString(c, g2, text, 0, 0);
            }
        } finally {
            g2.dispose();
        }
    }

    private static Shape pill(float w, float h) {
        return new RoundRectangle2D.Float(-w / 2, -h / 2, w, h, h, h);
    }

    // ---- style values: badge's own style first, then UIManager, then a fallback ----

    private Font font(JComponent c) {
        Font font = badge.font != null ? badge.font : UIManager.getFont("Badge.font");
        return font != null ? font : c.getFont();
    }

    private float textHeight() {
        return UIScale.scale(value(badge.height, -1, "Badge.height", 16));
    }

    private float ring() {
        return UIScale.scale(value(badge.borderWidth, -1, "Badge.borderWidth", 2));
    }

    private Insets margin() {
        Insets m = badge.margin != null ? badge.margin : UIManager.getInsets("Badge.margin");
        return m != null ? UIScale.scale(m) : new Insets(0, 0, 0, 0);
    }

    private float offsetX() {
        return UIScale.scale(value(badge.offsetX, Integer.MIN_VALUE, "Badge.offsetX", 0));
    }

    private float offsetY() {
        return UIScale.scale(value(badge.offsetY, Integer.MIN_VALUE, "Badge.offsetY", 0));
    }

    private static int value(int value, int unset, String key, int fallback) {
        if (value != unset) {
            return value;
        }
        return UIManager.get(key) instanceof Integer i ? i : fallback;
    }

    private static Color color(Color value, String key, Color fallback) {
        Color c = value != null ? value : UIManager.getColor(key);
        return c != null ? c : fallback;
    }

    // ---- animations ----

    /** Animates the show/hide scale; drives {@link AlertAnimation} from inside its paint. */
    private class ShowAnimation implements AnimatedIcon {

        @Override
        public void paintIconAnimated(Component c, Graphics g, int x, int y, float animatedValue) {
            showValue = animatedValue;
            alertAnimation.paintIcon(c, g, x, y);
        }

        @Override
        public float getValue(Component c) {
            return badge.isBadgeVisible() ? 1 : 0;
        }

        @Override
        public int getAnimationDuration() {
            return badge.isBadgeVisible() ? 400 : 200;
        }

        @Override
        public Animator.Interpolator getAnimationInterpolator() {
            // overshoot when popping in, ease in when hiding
            return badge.isBadgeVisible()
                    ? f -> {
                        float t = f - 1;
                        return t * t * (2.7f * t + 1.7f) + 1;
                    }
                    : f -> f * f;
        }

        @Override
        public int getIconWidth() {
            return iconWidth;
        }

        @Override
        public int getIconHeight() {
            return iconHeight;
        }
    }

    /** Animates the alert effect; each {@link Badge#alert()} moves the value to the next integer. */
    private class AlertAnimation implements AnimatedIcon {

        @Override
        public void paintIconAnimated(Component c, Graphics g, int x, int y, float animatedValue) {
            paintBadge((JComponent) c, g, showValue, animatedValue - (float) Math.floor(animatedValue));
        }

        @Override
        public float getValue(Component c) {
            return badge.getAlertCount();
        }

        @Override
        public int getAnimationDuration() {
            return 800;
        }

        @Override
        public Animator.Interpolator getAnimationInterpolator() {
            return f -> f;
        }

        @Override
        public int getIconWidth() {
            return iconWidth;
        }

        @Override
        public int getIconHeight() {
            return iconHeight;
        }
    }
}
