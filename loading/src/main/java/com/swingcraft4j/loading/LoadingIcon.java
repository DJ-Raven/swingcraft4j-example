package com.swingcraft4j.loading;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.ui.FlatStylingSupport;
import com.formdev.flatlaf.ui.FlatStylingSupport.Styleable;
import com.formdev.flatlaf.util.UIScale;
import com.swingcraft4j.loading.painter.arc.SpinnerPainter;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.util.*;

/**
 * Animated loading indicator icon; animates while painted and stops on its own once no longer shown.
 */
public class LoadingIcon implements Icon, FlatLaf.DisabledIconProvider {

    /**
     * Preset sizes from 16 to 32 px.
     */
    public enum Size {
        XS(16), SM(20), MD(24), LG(28), XL(32);

        private final int size;

        Size(int size) {
            this.size = size;
        }

        public int getSize() {
            return size;
        }
    }

    private static final int FRAME_DELAY = 16;
    private static final long STALE_NANOS = 1_000_000_000L;

    // one timer for all icons, so every loader repaints in the same frame
    private static final Set<LoadingIcon> ACTIVE = new LinkedHashSet<>();
    private static final Timer TIMER = new Timer(FRAME_DELAY, e -> tickAll());

    @Styleable
    protected int size = -1;
    @Styleable
    protected Color color;
    @Styleable
    protected int duration = -1;

    private LoadingPainter painter;
    private final Map<Component, Target> targets = new WeakHashMap<>();
    private Icon disabledIcon;

    public LoadingIcon() {
        this(new SpinnerPainter());
    }

    public LoadingIcon(LoadingPainter painter) {
        setPainter(painter);
    }

    public LoadingIcon(LoadingPainter painter, Size size) {
        this(painter);
        this.size = size.getSize();
    }

    /**
     * Applies a FlatLaf style string, e.g. {@code "color:$Actions.Blue;size:32;duration:800"}.
     */
    public void setStyle(String style) {
        FlatStylingSupport.parseAndApply(null, style,
                (key, v) -> FlatStylingSupport.applyToAnnotatedObject(this, key, v));
        update(true);
    }

    public LoadingPainter getPainter() {
        return painter;
    }

    /**
     * Animation to paint: a built-in painter (e.g. {@code new SpinnerPainter()}) or any custom {@link LoadingPainter}.
     */
    public void setPainter(LoadingPainter painter) {
        this.painter = painter != null ? painter : new SpinnerPainter();
        update(true);
    }

    /**
     * Unscaled height in pixels (and width, for square painters); {@link Size#MD} (24) if not set.
     */
    public int getSize() {
        return size > 0 ? size : Size.MD.getSize();
    }

    public void setSize(int size) {
        this.size = size;
        update(true);
    }

    public void setSize(Size size) {
        setSize(size.getSize());
    }

    /**
     * Loader color; {@code null} uses {@code Loading.color} from UIManager, else the component's foreground.
     */
    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
        update(false);
    }

    /**
     * Duration of one cycle in milliseconds; falls back to the painter's own duration.
     */
    public int getDuration() {
        return duration > 0 ? duration : painter.getDuration();
    }

    public void setDuration(int duration) {
        this.duration = duration;
        update(false);
    }

    @Override
    public int getIconWidth() {
        return (int) Math.ceil(painter.getWidth(UIScale.scale((float) getSize())));
    }

    @Override
    public int getIconHeight() {
        return UIScale.scale(getSize());
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Color paintColor = color;
        if (paintColor == null) {
            paintColor = UIManager.getColor("Loading.color");
        }
        if (paintColor == null) {
            paintColor = c != null ? c.getForeground() : UIManager.getColor("Label.foreground");
        }
        paint(c, g, x, y, paintColor);
    }

    /**
     * Same animation painted in {@code Label.disabledForeground}, used by disabled labels and buttons.
     */
    @Override
    public Icon getDisabledIcon() {
        if (disabledIcon == null) {
            disabledIcon = new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    paint(c, g, x, y, UIManager.getColor("Label.disabledForeground"));
                }

                @Override
                public int getIconWidth() {
                    return LoadingIcon.this.getIconWidth();
                }

                @Override
                public int getIconHeight() {
                    return LoadingIcon.this.getIconHeight();
                }
            };
        }
        return disabledIcon;
    }

    private void paint(Component c, Graphics g, int x, int y, Color paintColor) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // pure strokes keep sub-pixel positions, so motion doesn't snap in whole-pixel steps
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.translate(x, y);
            g2.setColor(paintColor);
            painter.paint(g2, UIScale.scale((float) getSize()), fraction());
        } finally {
            g2.dispose();
        }
        track(c, new Rectangle(x, y, getIconWidth(), getIconHeight()));
    }

    /**
     * Position in the current cycle, derived from the clock so every copy of the animation stays in sync.
     */
    private float fraction() {
        long cycle = getDuration() * 1_000_000L;
        return Math.floorMod(System.nanoTime(), cycle) / (float) cycle;
    }

    /**
     * Remembers where the icon was painted so the timer can repaint just that area.
     */
    private void track(Component c, Rectangle bounds) {
        if (c == null) {
            return;
        }
        Target target = targets.computeIfAbsent(c, k -> new Target());
        target.bounds = target.bounds == null || target.fresh ? bounds : target.bounds.union(bounds);
        target.fresh = false;
        target.lastPaint = System.nanoTime();
        ACTIVE.add(this);
        if (!TIMER.isRunning()) {
            TIMER.start();
        }
    }

    private static void tickAll() {
        long now = System.nanoTime();
        ACTIVE.removeIf(icon -> !icon.tick(now));
        if (ACTIVE.isEmpty()) {
            TIMER.stop();
        }
    }

    /**
     * Repaints every component still showing the icon; returns false once none is left.
     */
    private boolean tick(long now) {
        Iterator<Map.Entry<Component, Target>> it = targets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Component, Target> entry = it.next();
            Component c = entry.getKey();
            Target target = entry.getValue();
            if (c == null || !c.isShowing() || now - target.lastPaint > STALE_NANOS) {
                it.remove();
            } else {
                target.fresh = true;
                c.repaint(target.bounds.x, target.bounds.y, target.bounds.width, target.bounds.height);
            }
        }
        return !targets.isEmpty();
    }

    private void update(boolean layout) {
        for (Component c : targets.keySet()) {
            if (layout) {
                c.revalidate();
            }
            c.repaint();
        }
    }

    private static class Target {
        Rectangle bounds;
        boolean fresh;
        long lastPaint;
    }
}
