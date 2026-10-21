package com.swingcraft4j.login;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import com.swingcraft4j.login.background.Palette;
import com.swingcraft4j.login.background.Palette.Accent;
import net.miginfocom.swing.MigLayout;

/**
 * Two floating bars to switch the 3D scene, the appearance and the accent color.
 */
public class StyleSwitcher extends JPanel {

    private final Consumer<Palette> onPalette;
    private Accent accent;
    private boolean dark;
    private Palette palette;

    public StyleSwitcher(List<String> sceneNames, int scene, Palette palette,
            IntConsumer onScene, Consumer<Palette> onPalette) {
        this.onPalette = onPalette;
        this.palette = palette;
        accent = palette.accentKind;
        dark = palette.dark;
        setOpaque(false);
        setLayout(new MigLayout("wrap, insets 0, gap 0 8", "[center]"));
        Bar sceneBar = new Bar("insets 5, gap 2, aligny center");
        Bar themeBar = new Bar("insets 5 6 5 12, gap 2, aligny center");
        add(sceneBar);
        add(themeBar);

        ButtonGroup sceneGroup = new ButtonGroup();
        for (int i = 0; i < sceneNames.size(); i++) {
            int index = i;
            PillButton button = new PillButton(sceneNames.get(i));
            button.setSelected(i == scene);
            button.addActionListener(e -> onScene.accept(index));
            sceneGroup.add(button);
            sceneBar.add(button, "h 32!");
        }

        themeBar.add(new AppearanceButton(), "w 32!, h 32!");
        themeBar.add(new Divider(), "w 1!, h 20!, gapx 4 8");

        ButtonGroup accentGroup = new ButtonGroup();
        for (Accent a : Accent.values()) {
            AccentDot dot = new AccentDot(a);
            dot.setSelected(a == accent);
            dot.addActionListener(e -> {
                accent = a;
                changed();
            });
            accentGroup.add(dot);
            themeBar.add(dot, "w 24!, h 26!");
        }
    }

    private void changed() {
        palette = Palette.of(accent, dark);
        repaint();
        onPalette.accept(palette);
    }

    private static void antialias(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /**
     * A floating pill holding a row of buttons.
     */
    private class Bar extends JPanel {

        Bar(String layout) {
            super(new MigLayout(layout));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                antialias(g2);
                int w = getWidth(), h = getHeight();
                g2.setColor(palette.card);
                g2.fill(new RoundRectangle2D.Double(0, 0, w, h, h, h));
                g2.setColor(palette.cardBorder);
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, h, h));
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * A scene button: a pill in the accent when selected.
     */
    private class PillButton extends JToggleButton {

        PillButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusable(false);
            setRolloverEnabled(true);
            setBorder(new EmptyBorder(0, 16, 0, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                antialias(g2);
                int w = getWidth(), h = getHeight();
                boolean rollover = getModel().isRollover();
                if (isSelected()) {
                    g2.setColor(palette.accent);
                    g2.fill(new RoundRectangle2D.Double(0, 0, w, h, h, h));
                } else if (rollover) {
                    g2.setColor(Palette.alpha(palette.text, 22));
                    g2.fill(new RoundRectangle2D.Double(0, 0, w, h, h, h));
                }
                g2.setColor(isSelected() ? palette.accentForeground : rollover ? palette.text : palette.secondaryText);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Switches between the light and the dark appearance.
     */
    private class AppearanceButton extends JButton {

        private final GlyphIcon sun = new GlyphIcon(GlyphIcon.Kind.SUN, 16, Color.WHITE);
        private final GlyphIcon moon = new GlyphIcon(GlyphIcon.Kind.MOON, 16, Color.WHITE);

        AppearanceButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusable(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(dark ? "Light appearance" : "Dark appearance");
            addActionListener(e -> {
                dark = !dark;
                setToolTipText(dark ? "Light appearance" : "Dark appearance");
                changed();
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                antialias(g2);
                int w = getWidth(), h = getHeight();
                if (getModel().isRollover()) {
                    g2.setColor(Palette.alpha(palette.text, 22));
                    g2.fill(new Ellipse2D.Double(0, 0, w, h));
                }
                GlyphIcon icon = dark ? sun : moon;
                icon.setColor(getModel().isRollover() ? palette.text : palette.secondaryText);
                icon.paintIcon(this, g2, (w - 16) / 2, (h - 16) / 2);
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * An accent button: a dot of the accent with a white dot in the middle when selected, as in the macOS settings.
     */
    private class AccentDot extends JToggleButton {

        private final Accent dotAccent;

        AccentDot(Accent dotAccent) {
            this.dotAccent = dotAccent;
            setToolTipText(dotAccent.title);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusable(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                antialias(g2);
                double cx = getWidth() / 2.0, cy = getHeight() / 2.0;
                Color color = dotAccent.color(dark);
                if (getModel().isRollover() && !isSelected()) {
                    g2.setStroke(new BasicStroke(2f));
                    g2.setColor(Palette.alpha(color, 120));
                    g2.draw(new Ellipse2D.Double(cx - 9.5, cy - 9.5, 19, 19));
                }
                g2.setColor(color);
                g2.fill(new Ellipse2D.Double(cx - 7.5, cy - 7.5, 15, 15));
                g2.setColor(Palette.alpha(Color.BLACK, 30));
                g2.draw(new Ellipse2D.Double(cx - 7.5, cy - 7.5, 15, 15));
                if (isSelected()) {
                    g2.setColor(Color.WHITE);
                    g2.fill(new Ellipse2D.Double(cx - 2.75, cy - 2.75, 5.5, 5.5));
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * A thin line between the groups.
     */
    private class Divider extends JComponent {

        Divider() {
            setPreferredSize(new Dimension(1, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(Palette.alpha(palette.secondaryText, 70));
                g2.draw(new Line2D.Double(0, 0, 0, getHeight()));
            } finally {
                g2.dispose();
            }
        }
    }
}
