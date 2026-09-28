package com.swingcraft4j;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.FontUtils;
import com.formdev.flatlaf.util.UIScale;
import com.swingcraft4j.liquidprogress.LiquidProgress;
import com.swingcraft4j.liquidprogress.painter.CirclePainter;
import com.swingcraft4j.liquidprogress.painter.GaugePainter;
import com.swingcraft4j.liquidprogress.painter.LiquidPainter;
import com.swingcraft4j.liquidprogress.painter.ShapePainter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Demo entry point: a large preview, a sidebar of controls for it, and a gallery of every style.
 */
public class Main extends JFrame {

    private static final String[] SWATCH_NAMES = {"Purple", "Lime", "Ocean", "Sunset", "Rose", "Mint", "Indigo", "Amber"};
    // front and back wave colors of each swatch
    private static final String[][] SWATCHES = {
            {"#800080", "#ffc0cb"},
            {"#55dd10", "#cddd10"},
            {"#16e1ff", "#4f8fc6"},
            {"#ff6b35", "#ffc15e"},
            {"#e11d48", "#fda4af"},
            {"#10b981", "#a7f3d0"},
            {"#4f46e5", "#a5b4fc"},
            {"#f59e0b", "#fde68a"},
    };

    private static final String[] STYLE_NAMES = {"Circle", "Gauge", "Box", "Battery", "Flask", "Test tube", "Heart", "Droplet"};
    private static final List<Supplier<LiquidPainter>> STYLES = List.of(
            CirclePainter::new, GaugePainter::new, ShapePainter::box, ShapePainter::battery,
            ShapePainter::flask, ShapePainter::testTube, ShapePainter::heart, ShapePainter::droplet);
    // swatch and value of each meter in the styles gallery
    private static final int[] GALLERY_SWATCHES = {0, 2, 6, 1, 5, 7, 4, 2};
    private static final int[] GALLERY_VALUES = {60, 45, 70, 80, 55, 65, 50, 40};
    private static final int[] PRESETS = {0, 25, 50, 75, 100};
    private static final String CAPTION_STYLE = "foreground:$Label.disabledForeground;";
    private static final String SELECTED_CAPTION_STYLE = "foreground:$Component.accentColor;font:bold;";

    private final List<LiquidProgress> meters = new ArrayList<>();
    private final List<JLabel> styleCaptions = new ArrayList<>();
    private final JComboBox<String> style = new JComboBox<>(STYLE_NAMES);
    private final JSlider slider = new JSlider(0, 100, 60);
    private LiquidProgress preview;
    private boolean effects = true;

    public Main() {
        super("Liquid Progress Indicator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        init();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    private void init() {
        JPanel panel = new JPanel(new MigLayout("insets 16,gap 12 12", "[fill,grow][fill]", "[fill,grow][fill]"));
        panel.add(preview(), "cell 0 0");
        panel.add(styles(), "cell 0 1 2 1");
        // the sidebar last, so its options apply to every meter above
        panel.add(customize(), "cell 1 0");
        add(panel);
        selectStyle(0);
    }

    /**
     * The large meter everything in the sidebar changes.
     */
    private JPanel preview() {
        JPanel card = card("Preview", "", "[center,grow]", "[center,grow]");
        preview = meter(slider.getValue(), SWATCHES[0]);
        preview.setPreferredSize(UIScale.scale(new Dimension(300, 300)));
        card.add(preview, "grow");
        return card;
    }

    /**
     * Sidebar of controls in one grid, so every label shares a line with its control.
     */
    private JPanel customize() {
        // rows: value, presets, style, color, text, four options, theme; wider gaps between sections
        JPanel card = card("Customize", "wrap 3", "[right,pref!]12[grow]6[36!,right]", "[]2[]14[]14[]14[]14[]2[]2[]2[]14[]");

        JLabel value = new JLabel(slider.getValue() + "%");
        slider.addChangeListener(e -> {
            preview.setValue(slider.getValue());
            value.setText(slider.getValue() + "%");
        });
        card.add(new JLabel("Value"));
        card.add(slider, "growx");
        card.add(value);
        addPresets(card);

        style.addActionListener(e -> selectStyle(style.getSelectedIndex()));
        card.add(new JLabel("Style"));
        card.add(style, "span 2,growx");

        card.add(new JLabel("Color"));
        addSwatches(card);

        card.add(new JLabel("Format"));
        card.add(textFormat(), "span 2,growx");

        card.add(new JLabel("Show"));
        card.add(option("Bubbles", true, (m, b) -> m.setBubblesPainted(b)), "span 2");
        card.add(option("Text", true, (m, b) -> m.setTextPainted(b)), "skip,span 2");
        card.add(option("Percent sign", true, (m, b) -> m.setPercentSignPainted(b)), "skip,span 2");
        card.add(option("Shadow / highlight", true, (m, b) -> {
            effects = b;
            applyEffects(m.getPainter(), b);
        }), "skip,span 2");

        card.add(new JLabel("Theme"));
        card.add(darkMode(), "span 2");
        return card;
    }

    /**
     * Field for the preview's text; {@code {value}} stands for the percent, and an empty field shows the default percentage.
     */
    private JTextField textFormat() {
        JTextField field = new JTextField();
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "e.g. Loading {value}%");
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        field.setToolTipText("{value} is replaced by the percent; leave empty for the default");
        field.getDocument().addDocumentListener(new DocumentAdapter(() -> {
            String format = field.getText();
            preview.setTextFormatter(format.isEmpty() ? null : v -> format.replace("{value}", String.valueOf(v)));
        }));
        return field;
    }

    /**
     * Runs an action on every change to a document.
     */
    private record DocumentAdapter(Runnable action) implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            action.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            action.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            action.run();
        }
    }

    /**
     * Quick values under the slider, to watch the liquid rise and fall; split into one cell, not a nested panel.
     */
    private void addPresets(JPanel card) {
        ButtonGroup group = new ButtonGroup();
        List<JToggleButton> buttons = new ArrayList<>();
        for (int preset : PRESETS) {
            if (!buttons.isEmpty()) {
                card.add(new JSeparator(SwingConstants.VERTICAL), "growy,gapx 2 2");
            }
            JToggleButton button = new JToggleButton(preset + "%", preset == slider.getValue());
            // flat until hovered or selected, like a tool bar button
            button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
            button.addActionListener(e -> slider.setValue(preset));
            group.add(button);
            card.add(button, buttons.isEmpty() ? "skip,span 2,split " + (PRESETS.length * 2 - 1) : "gapx 0 0");
            buttons.add(button);
        }
        // the button of the current value stays selected, none between presets
        slider.addChangeListener(e -> {
            group.clearSelection();
            for (int i = 0; i < PRESETS.length; i++) {
                if (PRESETS[i] == slider.getValue()) {
                    buttons.get(i).setSelected(true);
                }
            }
        });
    }

    /**
     * Every style side by side; clicking one shows it in the preview, and the selected one is highlighted.
     */
    private JPanel styles() {
        JPanel card = card("Styles (click to try)", "wrap " + STYLES.size() + ",gap 10 4", "[center,grow]".repeat(STYLES.size()), "");
        for (int i = 0; i < STYLES.size(); i++) {
            LiquidProgress meter = meter(GALLERY_VALUES[i], SWATCHES[GALLERY_SWATCHES[i]]);
            meter.setPainter(STYLES.get(i).get());
            meter.setPreferredSize(UIScale.scale(new Dimension(96, 96)));
            meter.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            meter.setToolTipText("Show " + STYLE_NAMES[i] + " in the preview");
            int index = i;
            meter.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        style.setSelectedIndex(index);
                    }
                }
            });
            card.add(meter);
        }
        for (String name : STYLE_NAMES) {
            JLabel caption = caption(name);
            styleCaptions.add(caption);
            card.add(caption);
        }
        return card;
    }

    private void selectStyle(int index) {
        LiquidPainter painter = STYLES.get(index).get();
        preview.setPainter(painter);
        applyEffects(painter, effects);
        for (int i = 0; i < styleCaptions.size(); i++) {
            styleCaptions.get(i).putClientProperty(FlatClientProperties.STYLE, i == index ? SELECTED_CAPTION_STYLE : CAPTION_STYLE);
        }
    }

    /**
     * Turns the style's decoration on or off: the circle's drop shadow or a shape's glass highlight.
     */
    private static void applyEffects(LiquidPainter painter, boolean on) {
        if (painter instanceof CirclePainter circle) {
            circle.setShadowPainted(on);
        } else if (painter instanceof ShapePainter shape) {
            shape.setHighlightPainted(on);
        }
    }

    /**
     * Color buttons split into one cell; each sets the preview's front and back wave colors.
     */
    private void addSwatches(JPanel card) {
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < SWATCHES.length; i++) {
            Color front = Color.decode(SWATCHES[i][0]);
            Color back = Color.decode(SWATCHES[i][1]);
            JToggleButton button = new JToggleButton(new SwatchIcon(front, back), i == 0);
            button.setToolTipText(SWATCH_NAMES[i]);
            // the icon paints its own round selection, hover and focus rings
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder());
            button.setRolloverEnabled(true);
            button.addActionListener(e -> {
                preview.getFrontWave().setColor(front);
                preview.getBackWave().setColor(back);
            });
            group.add(button);
            card.add(button, i == 0 ? "span 2,split " + SWATCHES.length : "gapx 0 0");
        }
    }

    private static JCheckBox darkMode() {
        JCheckBox dark = new JCheckBox("Dark mode", FlatLaf.isLafDark());
        dark.addActionListener(e -> {
            FlatAnimatedLafChange.showSnapshot();
            if (dark.isSelected()) {
                FlatMacDarkLaf.setup();
            } else {
                FlatMacLightLaf.setup();
            }
            FlatLaf.updateUI();
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        });
        return dark;
    }

    /**
     * Check box that applies its state to every meter.
     */
    private JCheckBox option(String text, boolean selected, Setter setter) {
        JCheckBox box = new JCheckBox(text, selected);
        // meters start in the check box's state
        meters.forEach(m -> setter.apply(m, selected));
        box.addActionListener(e -> meters.forEach(m -> setter.apply(m, box.isSelected())));
        return box;
    }

    /**
     * Meter with the front and back wave colors of a swatch.
     */
    private LiquidProgress meter(float value, String[] swatch) {
        LiquidProgress meter = new LiquidProgress(value);
        meter.getFrontWave().setColor(Color.decode(swatch[0]));
        meter.getBackWave().setColor(Color.decode(swatch[1]));
        meters.add(meter);
        return meter;
    }

    /**
     * Swatch of two colors: a circle of the back color, half filled with the front color like the liquid, with round rings for selection and focus.
     */
    private record SwatchIcon(Color front, Color back) implements Icon {

        private static final float SWATCH = 20;
        private static final float GAP = 2;
        private static final float RING = 2;
        private static final float FOCUS = 2;
        private static final int SIZE = (int) (SWATCH + (GAP + RING + FOCUS) * 2);

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            FlatUIUtils.setRenderingHints(g2);
            g2.translate(x, y);
            g2.scale(UIScale.getUserScaleFactor(), UIScale.getUserScaleFactor());
            float center = SIZE / 2f;

            // focus ring outside, then selection or hover ring, then the swatch
            if (c.hasFocus()) {
                g2.setColor(UIManager.getColor("Component.focusColor"));
                g2.fill(ring(center, SWATCH / 2 + GAP + RING + FOCUS, FOCUS));
            }
            AbstractButton b = (AbstractButton) c;
            if (b.isSelected()) {
                g2.setColor(front);
                g2.fill(ring(center, SWATCH / 2 + GAP + RING, RING));
            } else if (b.getModel().isRollover()) {
                g2.setColor(UIManager.getColor("Component.borderColor"));
                g2.fill(ring(center, SWATCH / 2 + GAP + RING, RING));
            }

            Ellipse2D swatch = circle(center, SWATCH / 2);
            g2.setColor(back);
            g2.fill(swatch);
            Area half = new Area(swatch);
            half.intersect(new Area(new Rectangle2D.Float(0, center, SIZE, SIZE)));
            g2.setColor(front);
            g2.fill(half);
            g2.dispose();
        }

        /**
         * Round ring whose outer radius is {@code outer}.
         */
        private static Area ring(float center, float outer, float width) {
            Area ring = new Area(circle(center, outer));
            ring.subtract(new Area(circle(center, outer - width)));
            return ring;
        }

        private static Ellipse2D circle(float center, float radius) {
            return new Ellipse2D.Float(center - radius, center - radius, radius * 2, radius * 2);
        }

        @Override
        public int getIconWidth() {
            return UIScale.scale(SIZE);
        }

        @Override
        public int getIconHeight() {
            return UIScale.scale(SIZE);
        }
    }

    private static JLabel caption(String text) {
        JLabel label = new JLabel(text);
        label.putClientProperty(FlatClientProperties.STYLE, CAPTION_STYLE);
        return label;
    }

    /**
     * Panel holding one group of examples, framed by a titled border.
     */
    private static JPanel card(String title, String layout, String columns, String rows) {
        JPanel card = new JPanel(new MigLayout("insets 10," + layout, columns, rows));
        card.setBorder(BorderFactory.createTitledBorder(title));
        return card;
    }

    private interface Setter {
        void apply(LiquidProgress meter, boolean value);
    }

    /**
     * Installs the demo's font/theme, then shows the window on the Swing event thread.
     */
    public static void main(String[] args) {
        FlatRobotoFont.install();
        UIManager.put("defaultFont", FontUtils.getCompositeFont(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
        FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", "#3d6bff"));
        FlatMacLightLaf.setup();

        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
