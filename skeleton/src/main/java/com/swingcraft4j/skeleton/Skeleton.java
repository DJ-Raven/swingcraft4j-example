package com.swingcraft4j.skeleton;

import com.formdev.flatlaf.util.UIScale;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.LayerUI;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * JLayer UI that swaps its view for shimmering skeleton placeholders while loading, and blocks input.
 */
public class Skeleton extends LayerUI<JComponent> {

    /**
     * Client property: draw the whole label as a circle placeholder (e.g. an avatar), icon or not.
     */
    public static final String CIRCLE = "Skeleton.circle";

    private static final int PERIOD_MS = 1500;

    private final Timer timer = new Timer(16, e -> tick());
    private JLayer<?> layer;
    private boolean loading;

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        if (this.loading == loading) {
            return;
        }
        this.loading = loading;
        updateTimer();
        if (layer != null) {
            layer.repaint();
        }
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        layer = (JLayer<?>) c;
        layer.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
                | AWTEvent.MOUSE_WHEEL_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        updateTimer();
    }

    @Override
    public void uninstallUI(JComponent c) {
        timer.stop();
        layer.setLayerEventMask(0);
        layer = null;
        super.uninstallUI(c);
    }

    @Override
    public void eventDispatched(AWTEvent e, JLayer<? extends JComponent> l) {
        if (loading && e instanceof InputEvent input) {
            input.consume();
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Component view = ((JLayer<?>) c).getView();
        if (!loading || view == null) {
            super.paint(g, c);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Path2D shapes = new Path2D.Float();
            collect(view, 0, 0, new Rectangle(0, 0, c.getWidth(), c.getHeight()), g2, shapes);
            g2.setPaint(shimmer(c.getWidth()));
            g2.fill(shapes);
        } finally {
            g2.dispose();
        }
    }

    private void updateTimer() {
        if (loading && layer != null) {
            timer.start();
        } else {
            timer.stop();
        }
    }

    private void tick() {
        if (layer != null && layer.isShowing()) {
            layer.repaint();
        }
    }

    /**
     * Paints backgrounds/borders of the view tree and gathers a placeholder shape per leaf component.
     */
    private void collect(Component comp, int ox, int oy, Rectangle clip, Graphics2D g, Path2D shapes) {
        if (!comp.isVisible() || comp instanceof JScrollBar) {
            return;
        }
        int x = ox + comp.getX();
        int y = oy + comp.getY();
        Rectangle bounds = new Rectangle(x, y, comp.getWidth(), comp.getHeight());
        if (comp instanceof JComponent jc) {
            if (jc.isOpaque()) {
                g.setColor(jc.getBackground());
                g.fillRect(x, y, bounds.width, bounds.height);
            }
            Border border = jc.getBorder();
            if (border != null) {
                border.paintBorder(jc, g, x, y, bounds.width, bounds.height);
            }
        }
        if (isLeaf(comp)) {
            addLeaf(comp, bounds, clip, shapes);
            return;
        }
        Rectangle childClip = clip.intersection(bounds);
        Container container = (Container) comp;
        for (int i = container.getComponentCount() - 1; i >= 0; i--) {
            collect(container.getComponent(i), x, y, childClip, g, shapes);
        }
    }

    /**
     * Composite widgets whose children are internals (renderer pane, arrow button) count as leaves too.
     */
    private static boolean isLeaf(Component c) {
        return !(c instanceof Container container) || container.getComponentCount() == 0
                || c instanceof JComboBox || c instanceof JSpinner
                || c instanceof JList || c instanceof JTable || c instanceof JTree;
    }

    private void addLeaf(Component comp, Rectangle bounds, Rectangle clip, Path2D shapes) {
        if (comp instanceof JLabel label) {
            addLabel(label, bounds, shapes);
        } else if (comp instanceof JTextArea area) {
            addLines(area, bounds, clip, shapes);
        } else if (!(comp instanceof JPanel || comp instanceof Box.Filler)) {
            addRounded(bounds.intersection(clip), UIScale.scale(8f), shapes);
        }
    }

    /**
     * A bar per text line, the last one shorter, like a paragraph.
     */
    private void addLines(JTextArea area, Rectangle bounds, Rectangle clip, Path2D shapes) {
        FontMetrics fm = area.getFontMetrics(area.getFont());
        Insets in = area.getInsets();
        int lineHeight = fm.getHeight();
        int bar = Math.round(lineHeight * 0.55f);
        int width = bounds.width - in.left - in.right;
        int lines = (bounds.height - in.top - in.bottom) / lineHeight;
        for (int i = 0; i < lines; i++) {
            int y = bounds.y + in.top + i * lineHeight + (lineHeight - bar) / 2;
            int w = i == lines - 1 ? Math.round(width * 0.6f) : width;
            addRounded(new Rectangle(bounds.x + in.left, y, w, bar).intersection(clip), bar, shapes);
        }
    }

    /**
     * Icon block plus a text-width bar, laid out the same way the label paints them.
     */
    private void addLabel(JLabel label, Rectangle bounds, Path2D shapes) {
        Insets in = label.getInsets();
        Rectangle view = new Rectangle(in.left, in.top, bounds.width - in.left - in.right, bounds.height - in.top - in.bottom);
        if (Boolean.TRUE.equals(label.getClientProperty(CIRCLE))) {
            // avatar: circle filling the label, with or without an icon
            int size = Math.min(view.width, view.height);
            addRounded(new Rectangle(bounds.x + view.x + (view.width - size) / 2,
                    bounds.y + view.y + (view.height - size) / 2, size, size), size, shapes);
            return;
        }
        FontMetrics fm = label.getFontMetrics(label.getFont());
        Rectangle icon = new Rectangle();
        Rectangle text = new Rectangle();
        String clipped = SwingUtilities.layoutCompoundLabel(label, fm, label.getText(), label.getIcon(),
                label.getVerticalAlignment(), label.getHorizontalAlignment(),
                label.getVerticalTextPosition(), label.getHorizontalTextPosition(),
                view, icon, text, label.getIconTextGap());
        if (label.getIcon() != null) {
            icon.translate(bounds.x, bounds.y);
            addRounded(icon, Math.min(icon.width, icon.height) * 0.3f, shapes);
        }
        if (clipped != null && !clipped.isEmpty()) {
            int barHeight = Math.round(fm.getHeight() * 0.55f);
            addRounded(new Rectangle(bounds.x + text.x, bounds.y + text.y + (text.height - barHeight) / 2,
                    text.width, barHeight), barHeight, shapes);
        }
    }

    private static void addRounded(Rectangle r, float arc, Path2D shapes) {
        if (r.isEmpty()) {
            return;
        }
        float a = Math.min(arc, Math.min(r.width, r.height));
        shapes.append(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, a, a), false);
    }

    /**
     * Skeleton color with a soft highlight band sweeping left to right.
     */
    private static Paint shimmer(int width) {
        Color fg = UIManager.getColor("Label.foreground");
        if (fg == null) {
            fg = Color.GRAY;
        }
        Color base = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 30);
        Color light = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 70);
        float t = (System.nanoTime() / 1_000_000f % PERIOD_MS) / (float) PERIOD_MS;
        float band = Math.max(width * 0.3f, UIScale.scale(80f));
        float x = -band + t * (width + 2 * band);
        return new LinearGradientPaint(x - band, 0, x + band, band * 0.3f,
                new float[]{0f, 0.5f, 1f}, new Color[]{base, light, base});
    }
}
