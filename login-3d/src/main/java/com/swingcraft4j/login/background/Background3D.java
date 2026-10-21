package com.swingcraft4j.login.background;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


/**
 * An animated 3D background: one of the scenes over a gradient with stars flying toward the screen.
 */
public class Background3D extends JPanel {

    private static final int STARS = 260;
    private static final double AUTO_SPIN = 0.0035;
    private static final int FADE_FRAMES = 36;
    private static final int FPS = 60;

    private final List<Scene> scenes = Arrays.<Scene>asList(
            new GlobeScene(), new WaveScene(), new KnotScene(), new GalaxyScene(), new CubeScene(),
            new HelixScene(), new TunnelScene(), new AtomScene(), new MorphScene(), new CrystalScene());
    private Scene scene;
    // the scene fading out after a switch, and how far the fade is from 0 to 1
    private Scene previous;
    private double fade = 1;

    private Palette palette;
    // the colors shown, eased toward the ones of the palette so a switch blends
    private final float[] top;
    private final float[] bottom;
    private final float[] far;
    private final float[] near;
    private final float[] accent;

    // x and y in -1..1, z the distance from 0 (at the screen) to 1 (far away)
    private final double[][] stars = new double[STARS][3];
    // x, y and age in frames of the ripples sent by a click
    private final List<double[]> ripples = new ArrayList<>();
    private final Random random = new Random();
    private final View view = new View();

    private double velocityX, velocityY = AUTO_SPIN;
    private Point dragLast;
    // the frames come from a thread of their own: a Swing timer runs at about 32
    // frames a second on Windows, a sleep of the thread keeps the 60
    private volatile Thread animator;

    public Background3D(Palette palette, int sceneIndex) {
        this.palette = palette;
        scene = scenes.get(Math.max(0, Math.min(sceneIndex, scenes.size() - 1)));
        top = rgb(palette.top);
        bottom = rgb(palette.bottom);
        far = rgb(palette.far);
        near = rgb(palette.near);
        accent = rgb(palette.accent);
        setOpaque(true);
        setBackground(palette.top);
        view.setColors(palette.far, palette.near, palette.accent, palette.dark);
        for (double[] star : stars) {
            resetStar(star, random.nextDouble());
        }
        MouseAdapter drag = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragLast = e.getPoint();
                ripples.add(new double[]{e.getX(), e.getY(), 0});
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                velocityY = (e.getX() - dragLast.x) * 0.004;
                velocityX = (e.getY() - dragLast.y) * 0.004;
                dragLast = e.getPoint();
            }
        };
        addMouseListener(drag);
        addMouseMotionListener(drag);
    }

    /**
     * The names of the scenes, in the order of {@link #setScene}.
     */
    public List<String> getSceneNames() {
        List<String> names = new ArrayList<>();
        for (Scene s : scenes) {
            names.add(s.getName());
        }
        return names;
    }

    /**
     * Fades into the scene at the index of {@link #getSceneNames}.
     */
    public void setScene(int index) {
        Scene next = scenes.get(index);
        if (next != scene) {
            previous = scene;
            scene = next;
            fade = 0;
        }
    }

    /**
     * Blends into the colors of the palette.
     */
    public void setPalette(Palette palette) {
        this.palette = palette;
    }

    private static float[] rgb(Color c) {
        return c.getRGBColorComponents(null);
    }

    private static void ease(float[] current, Color target) {
        float[] t = rgb(target);
        for (int i = 0; i < 3; i++) {
            current[i] += (t[i] - current[i]) * 0.08f;
        }
    }

    private static Color color(float[] rgb) {
        return new Color(rgb[0], rgb[1], rgb[2]);
    }

    private void resetStar(double[] star, double z) {
        star[0] = random.nextDouble() * 2 - 1;
        star[1] = random.nextDouble() * 2 - 1;
        star[2] = z;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        animator = new Thread(this::animate, "Background3D");
        animator.setDaemon(true);
        animator.start();
    }

    @Override
    public void removeNotify() {
        Thread thread = animator;
        animator = null;
        if (thread != null) {
            thread.interrupt();
        }
        super.removeNotify();
    }

    /**
     * Moves the background forward 60 times a second, each frame on the event thread, until the panel is removed.
     */
    private void animate() {
        long frameNanos = 1_000_000_000L / FPS;
        long next = System.nanoTime();
        try {
            while (animator == Thread.currentThread()) {
                // wait for the frame, so the frames never pile up on a slow machine
                SwingUtilities.invokeAndWait(this::tick);
                next += frameNanos;
                long sleep = (next - System.nanoTime()) / 1_000_000;
                if (sleep > 0) {
                    Thread.sleep(sleep);
                } else {
                    // behind: start again from now rather than rushing to catch up
                    next = System.nanoTime();
                }
            }
        } catch (InterruptedException e) {
            // removed from the window
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    /**
     * Moves the background one frame forward.
     */
    private void tick() {
        Point mouse = null;
        if (isShowing()) {
            PointerInfo info = MouseInfo.getPointerInfo();
            if (info != null) {
                Point p = info.getLocation();
                SwingUtilities.convertPointFromScreen(p, this);
                if (contains(p)) {
                    mouse = p;
                }
            }
        }
        double targetX = 0, targetY = 0;
        if (mouse != null && getWidth() > 0 && getHeight() > 0) {
            targetX = (double) mouse.x / getWidth() - 0.5;
            targetY = (double) mouse.y / getHeight() - 0.5;
        }
        view.width = getWidth();
        view.height = getHeight();
        view.frame++;
        view.mouse = mouse;
        view.mouseX += (targetX - view.mouseX) * 0.06;
        view.mouseY += (targetY - view.mouseY) * 0.06;
        view.tiltY = view.mouseX * 0.9;
        view.tiltX = -view.mouseY * 0.7;
        view.spinX += velocityX;
        view.spinY += velocityY;
        // a drag fades back to the slow turn of its own
        velocityX *= 0.96;
        velocityY += (AUTO_SPIN - velocityY) * 0.02;

        ease(top, palette.top);
        ease(bottom, palette.bottom);
        ease(far, palette.far);
        ease(near, palette.near);
        ease(accent, palette.accent);
        view.setColors(color(far), color(near), color(accent), palette.dark);

        for (double[] star : stars) {
            star[2] -= 0.0022;
            if (star[2] < 0.04) {
                resetStar(star, 1);
            }
        }
        for (Iterator<double[]> it = ripples.iterator(); it.hasNext();) {
            double[] ripple = it.next();
            if (++ripple[2] > 60) {
                it.remove();
            }
        }
        scene.tick(view);
        if (previous != null) {
            previous.tick(view);
            fade += 1.0 / FADE_FRAMES;
            if (fade >= 1) {
                fade = 1;
                previous = null;
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int w = getWidth(), h = getHeight();
            // the first paint can come before the first frame
            view.width = w;
            view.height = h;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setPaint(new GradientPaint(0, 0, color(top), 0, h, color(bottom)));
            g2.fillRect(0, 0, w, h);

            paintStars(g2, w, h);
            if (previous != null) {
                paintScene(g2, previous, 1 - fade);
            }
            paintScene(g2, scene, fade);
            paintMouse(g2);
        } finally {
            g2.dispose();
        }
    }

    private void paintScene(Graphics2D g2, Scene s, double alpha) {
        Graphics2D sg = (Graphics2D) g2.create();
        try {
            if (alpha < 1) {
                sg.setComposite(AlphaComposite.SrcOver.derive((float) alpha));
            }
            s.paint(sg, view);
        } finally {
            sg.dispose();
        }
    }

    private void paintStars(Graphics2D g2, int w, int h) {
        double cx = w / 2.0, cy = h / 2.0;
        // light stars on the dark, specks of the accent on the light
        Color base = view.dark ? Palette.mix(view.near, Color.WHITE, 0.7) : view.near;
        double strength = view.dark ? 230 : 120;
        Ellipse2D.Double dot = new Ellipse2D.Double();
        for (double[] star : stars) {
            double z = star[2];
            // the nearer stars move more with the mouse, which gives the depth
            double sx = cx + star[0] / z * cx * 0.6 - view.mouseX * 120 * (1.05 - z);
            double sy = cy + star[1] / z * cy * 0.6 - view.mouseY * 120 * (1.05 - z);
            if (sx < 0 || sy < 0 || sx > w || sy > h) {
                continue;
            }
            double nearness = 1 - z;
            double size = 0.6 + nearness * 2.6;
            g2.setColor(Scene.alpha(base, (int) (nearness * nearness * strength)));
            dot.setFrame(sx - size / 2, sy - size / 2, size, size);
            g2.fill(dot);
        }
    }

    private void paintMouse(Graphics2D g2) {
        if (view.mouse != null) {
            Scene.paintGlow(g2, view.mouse.x, view.mouse.y, 200, view.near, 40);
        }
        g2.setStroke(new BasicStroke(2f));
        for (double[] ripple : ripples) {
            double t = ripple[2] / 60;
            double r = 10 + t * 140;
            g2.setColor(Scene.alpha(view.light, (int) ((1 - t) * 200)));
            g2.draw(new Ellipse2D.Double(ripple[0] - r, ripple[1] - r, r * 2, r * 2));
        }
    }
}
