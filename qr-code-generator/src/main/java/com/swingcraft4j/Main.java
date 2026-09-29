package com.swingcraft4j;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.FontUtils;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.formdev.flatlaf.util.UIScale;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.swingcraft4j.qrcode.QrCode;
import com.swingcraft4j.qrcode.QrCodeView;
import com.swingcraft4j.qrcode.QrMatrix;
import com.swingcraft4j.qrcode.shape.BodyStyle;
import com.swingcraft4j.qrcode.shape.EyeBallStyle;
import com.swingcraft4j.qrcode.shape.EyeFrameStyle;
import net.miginfocom.swing.MigLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Demo entry point: a large preview, a sidebar of controls for it, and a gallery of presets.
 */
public class Main extends JFrame {

    private static final String DEFAULT_TEXT = "https://github.com/DJ-Raven/swingcraft4j-example";
    // short, so the small preset codes have few modules and their shapes are easy to see
    private static final String PRESET_TEXT = "swingcraft4j";

    private record Palette(String name, Color body, Color eye, Color gradient) {
    }

    private static final Palette[] PALETTES = {
            new Palette("Ink", Color.decode("#111827"), Color.decode("#111827"), Color.decode("#4b5563")),
            new Palette("Ocean", Color.decode("#0369a1"), Color.decode("#0c4a6e"), Color.decode("#0891b2")),
            new Palette("Grape", Color.decode("#6d28d9"), Color.decode("#4c1d95"), Color.decode("#be185d")),
            new Palette("Forest", Color.decode("#047857"), Color.decode("#064e3b"), Color.decode("#4d7c0f")),
            new Palette("Sunset", Color.decode("#c2410c"), Color.decode("#9a3412"), Color.decode("#be185d")),
            new Palette("Rose", Color.decode("#be123c"), Color.decode("#881337"), Color.decode("#c2410c")),
            new Palette("Indigo", Color.decode("#4338ca"), Color.decode("#312e81"), Color.decode("#7c3aed")),
            new Palette("Teal", Color.decode("#0f766e"), Color.decode("#134e4a"), Color.decode("#0369a1")),
            new Palette("Amber", Color.decode("#b45309"), Color.decode("#78350f"), Color.decode("#c2410c")),
            new Palette("Berry", Color.decode("#a21caf"), Color.decode("#701a75"), Color.decode("#db2777")),
    };

    private static final String[] LOGO_NAMES = {"None", "Heart", "Star", "Bolt", "Chat"};
    private static final String[] LOGO_FILES = {null, "heart.svg", "star.svg", "bolt.svg", "chat.svg"};

    private static final String[] ERROR_NAMES = {"L (7%)", "M (15%)", "Q (25%)", "H (30%)"};
    private static final ErrorCorrectionLevel[] ERROR_LEVELS = {
            ErrorCorrectionLevel.L, ErrorCorrectionLevel.M, ErrorCorrectionLevel.Q, ErrorCorrectionLevel.H};

    private record Preset(String name, BodyStyle body, EyeFrameStyle frame, EyeBallStyle ball, int palette,
                          boolean gradient, int logo) {
    }

    private static final int PRESET_COLUMNS = 6;
    private static final Preset[] PRESETS = {
            new Preset("Classic", BodyStyle.SQUARE, EyeFrameStyle.SQUARE, EyeBallStyle.SQUARE, 0, false, 0),
            new Preset("Fluid", BodyStyle.FLUID, EyeFrameStyle.ROUNDED, EyeBallStyle.ROUNDED, 1, true, 4),
            new Preset("Dots", BodyStyle.DOTS, EyeFrameStyle.CIRCLE, EyeBallStyle.CIRCLE, 2, true, 2),
            new Preset("Rounded", BodyStyle.ROUNDED, EyeFrameStyle.ROUNDED, EyeBallStyle.ROUNDED, 3, false, 0),
            new Preset("Leaf", BodyStyle.LEAF, EyeFrameStyle.LEAF, EyeBallStyle.LEAF, 5, true, 1),
            new Preset("Lines", BodyStyle.HORIZONTAL, EyeFrameStyle.ROUNDED, EyeBallStyle.CIRCLE, 4, true, 3),
            new Preset("Columns", BodyStyle.VERTICAL, EyeFrameStyle.SQUARE, EyeBallStyle.ROUNDED, 7, false, 0),
            new Preset("Diamond", BodyStyle.DIAMOND, EyeFrameStyle.CIRCLE, EyeBallStyle.DIAMOND, 0, true, 2),
            new Preset("Beads", BodyStyle.CONNECTED, EyeFrameStyle.ROUNDED, EyeBallStyle.CIRCLE, 6, true, 1),
            new Preset("Honeycomb", BodyStyle.HEXAGON, EyeFrameStyle.OCTAGON, EyeBallStyle.OCTAGON, 8, false, 3),
            new Preset("Lattice", BodyStyle.CROSS, EyeFrameStyle.SQUARE, EyeBallStyle.SQUARE, 9, true, 0),
            new Preset("Stars", BodyStyle.STAR, EyeFrameStyle.ROUNDED, EyeBallStyle.STAR, 2, true, 4),
    };

    private static final String CAPTION_STYLE = "foreground:$Label.disabledForeground;";
    private static final String SELECTED_CAPTION_STYLE = "foreground:$Component.accentColor;font:bold;";

    private final QrCodeView preview = new QrCodeView(DEFAULT_TEXT);
    private final List<JLabel> presetCaptions = new ArrayList<>();
    private final List<JToggleButton> swatchButtons = new ArrayList<>();
    private final List<JToggleButton> logoButtons = new ArrayList<>();
    private final JTextField content = new JTextField(DEFAULT_TEXT);
    private final JComboBox<String> errorCorrection = new JComboBox<>(ERROR_NAMES);
    private final JComboBox<String> body = new JComboBox<>(names(BodyStyle.values()));
    private final JComboBox<String> frame = new JComboBox<>(names(EyeFrameStyle.values()));
    private final JComboBox<String> ball = new JComboBox<>(names(EyeBallStyle.values()));
    private final JCheckBox gradient = new JCheckBox("Gradient");
    private final JCheckBox coloredEyes = new JCheckBox("Darker eyes", true);
    private final JCheckBox transparent = new JCheckBox("Transparent background");
    private final JSlider logoSize = new JSlider(10, 30, 22);
    private final JLabel info = caption("");
    // logo color of the preview and the sidebar buttons, read by their color filter
    private Color logoColor = PALETTES[0].eye();
    private int palette;
    private int logo;
    // true while a preset sets the controls, so they don't apply one by one
    private boolean applying;

    public Main() {
        super("QR Code Generator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        init();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    private void init() {
        JPanel panel = new JPanel(new MigLayout("", "[fill,grow][420!,fill]", "[fill,grow][fill]"));
        panel.add(preview(), "cell 0 0");
        panel.add(customize(), "cell 1 0");
        panel.add(presets(), "cell 0 1 2 1");
        add(panel);
        applyPreset(PRESETS[1]);
    }

    /**
     * The large code everything in the sidebar changes, with its version and size below.
     */
    private JPanel preview() {
        JPanel card = card("Preview", "wrap", "[center,grow]", "[center,grow][]");
        preview.setMargin(2);
        preview.setBackgroundRadius(2);
        preview.setPreferredSize(UIScale.scale(new Dimension(340, 340)));
        preview.addPropertyChangeListener("matrix", e -> updateInfo());
        card.add(preview, "grow");
        card.add(info);
        updateInfo();
        return card;
    }

    private void updateInfo() {
        QrMatrix matrix = preview.getMatrix();
        if (matrix != null) {
            info.setText("Version " + matrix.getVersion() + " · " + matrix.getSize() + " × " + matrix.getSize() + " modules");
        } else {
            info.setText(content.getText().isEmpty() ? "Type some text to encode" : "Too long for a QR code at this error correction");
        }
    }

    /**
     * Sidebar of controls in one grid, so every label shares a line with its control.
     */
    private JPanel customize() {
        // rows: content, error, three shapes, color, three options, logo, logo size, theme, save at the bottom
        JPanel card = card("Customize", "wrap 3", "[right,pref!][grow][36!,right]", "[][][][][][][][][][][][]push[]");

        content.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Text or URL");
        content.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        content.getDocument().addDocumentListener(new DocumentAdapter(() -> preview.setText(content.getText())));
        card.add(new JLabel("Content"));
        card.add(content, "span 2,growx,wmin 0");

        errorCorrection.setToolTipText("How much of the code can be covered and still read; use H with a logo");
        card.add(new JLabel("Error level"));
        card.add(errorCorrection, "span 2,growx");

        card.add(new JLabel("Body"));
        card.add(body, "span 2,growx");
        card.add(new JLabel("Eye frame"));
        card.add(frame, "span 2,growx");
        card.add(new JLabel("Eye ball"));
        card.add(ball, "span 2,growx");

        card.add(new JLabel("Color"));
        addSwatches(card);

        card.add(new JLabel("Show"));
        card.add(gradient, "span 2");
        card.add(coloredEyes, "skip,span 2");
        // also exported, so a saved PNG is transparent too
        transparent.setToolTipText("Scanners need a light surface behind the code");
        card.add(transparent, "skip,span 2");

        card.add(new JLabel("Logo"));
        addLogos(card);

        JLabel size = new JLabel(logoSize.getValue() + "%");
        logoSize.addChangeListener(e -> size.setText(logoSize.getValue() + "%"));
        card.add(new JLabel("Logo size"));
        card.add(logoSize, "growx");
        card.add(size);

        card.add(new JLabel("Theme"));
        card.add(darkMode(), "span 2");

        JButton savePng = new JButton("Save PNG…");
        savePng.addActionListener(e -> save("png", "PNG image"));
        JButton saveSvg = new JButton("Save SVG…");
        saveSvg.addActionListener(e -> save("svg", "SVG image"));
        card.add(savePng, "skip,span 2,split 2,growx,sizegroupx save,gaptop 6");
        card.add(saveSvg, "growx,sizegroupx save,gaptop 6");

        for (JComboBox<?> combo : List.of(errorCorrection, body)) {
            combo.addActionListener(e -> apply());
        }
        // the dotted frame and the star ball don't scan together, so picking one moves the other off
        frame.addActionListener(e -> {
            if (!applying && !isEyePairSupported()) {
                selectSilently(ball, EyeBallStyle.CIRCLE.ordinal());
            }
            apply();
        });
        ball.addActionListener(e -> {
            if (!applying && !isEyePairSupported()) {
                selectSilently(frame, EyeFrameStyle.ROUNDED.ordinal());
            }
            apply();
        });
        gradient.addActionListener(e -> apply());
        coloredEyes.addActionListener(e -> apply());
        transparent.addActionListener(e -> apply());
        logoSize.addChangeListener(e -> apply());
        return card;
    }

    /**
     * Palette buttons split into one cell; each shows its body to gradient colors.
     */
    private void addSwatches(JPanel card) {
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < PALETTES.length; i++) {
            Palette p = PALETTES[i];
            JToggleButton button = new JToggleButton(new SwatchIcon(p.body(), p.gradient()));
            button.setToolTipText(p.name());
            // the icon paints its own round selection, hover and focus rings
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder());
            button.setRolloverEnabled(true);
            int index = i;
            button.addActionListener(e -> {
                palette = index;
                apply();
            });
            group.add(button);
            swatchButtons.add(button);
            card.add(button, i == 0 ? "span 2,split " + PALETTES.length : "gapx 0 0");
        }
    }

    /**
     * Logo buttons split into one cell, tinted with the palette like the preview's logo.
     */
    private void addLogos(JPanel card) {
        ButtonGroup group = new ButtonGroup();
        FlatSVGIcon.ColorFilter filter = new FlatSVGIcon.ColorFilter(c -> Color.BLACK.equals(c) ? logoColor : c);
        for (int i = 0; i < LOGO_NAMES.length; i++) {
            JToggleButton button = new JToggleButton();
            if (LOGO_FILES[i] == null) {
                button.setText(LOGO_NAMES[i]);
            } else {
                button.setIcon(logoIcon(i, 20, filter));
            }
            button.setToolTipText(LOGO_NAMES[i]);
            button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
            int index = i;
            button.addActionListener(e -> {
                logo = index;
                // a logo covers modules, so it needs the highest error correction
                if (index > 0) {
                    errorCorrection.setSelectedIndex(ERROR_LEVELS.length - 1);
                }
                apply();
            });
            group.add(button);
            logoButtons.add(button);
            card.add(button, i == 0 ? "span 2,split " + LOGO_NAMES.length : "gapx 0 0");
        }
    }

    /**
     * Every preset in rows, each code over its caption; clicking one sets the sidebar to it, and the matching one is highlighted.
     */
    private JPanel presets() {
        JPanel card = card("Presets (click to try)", "", "[center,grow]".repeat(PRESET_COLUMNS), "");
        for (int start = 0; start < PRESETS.length; start += PRESET_COLUMNS) {
            List<Preset> row = Arrays.asList(PRESETS).subList(start, Math.min(start + PRESET_COLUMNS, PRESETS.length));
            for (Preset preset : row) {
                card.add(presetView(preset), preset == row.getLast() ? "wrap" : "");
            }
            // then their captions, one row below
            for (Preset preset : row) {
                JLabel caption = caption(preset.name());
                presetCaptions.add(caption);
                card.add(caption, preset == row.getLast() ? "wrap" : "");
            }
        }
        return card;
    }

    /**
     * Small code styled like the preset, which applies it when clicked.
     */
    private QrCodeView presetView(Preset preset) {
        Palette p = PALETTES[preset.palette()];
        QrCodeView view = new QrCodeView(PRESET_TEXT);
        view.setErrorCorrection(ErrorCorrectionLevel.H);
        view.setBackgroundRadius(2);
        view.setBodyShape(preset.body());
        view.setEyeFrameShape(preset.frame());
        view.setEyeBallShape(preset.ball());
        view.setBodyColor(p.body());
        view.setGradientColor(preset.gradient() ? p.gradient() : null);
        view.setEyeFrameColor(p.eye());
        view.setEyeBallColor(p.eye());
        if (preset.logo() > 0) {
            view.setLogo(logoIcon(preset.logo(), 48, new FlatSVGIcon.ColorFilter(c -> Color.BLACK.equals(c) ? p.eye() : c)));
        }
        view.setPreferredSize(UIScale.scale(new Dimension(116, 116)));
        view.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        view.setToolTipText("Show " + preset.name() + " in the preview");
        view.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    applyPreset(preset);
                }
            }
        });
        return view;
    }

    /**
     * Sets the sidebar controls to the preset, then applies them once.
     */
    private void applyPreset(Preset preset) {
        applying = true;
        body.setSelectedIndex(preset.body().ordinal());
        frame.setSelectedIndex(preset.frame().ordinal());
        ball.setSelectedIndex(preset.ball().ordinal());
        gradient.setSelected(preset.gradient());
        coloredEyes.setSelected(true);
        palette = preset.palette();
        swatchButtons.get(palette).setSelected(true);
        logo = preset.logo();
        logoButtons.get(logo).setSelected(true);
        errorCorrection.setSelectedIndex(ERROR_LEVELS.length - 1);
        applying = false;
        apply();
    }

    /**
     * Applies every sidebar control to the preview.
     */
    private void apply() {
        if (applying) {
            return;
        }
        Palette p = PALETTES[palette];
        logoColor = p.eye();
        // one new code for every setting, so it never passes through an unsupported eye pair
        preview.setCode(preview.getCode().toBuilder()
                .errorCorrection(ERROR_LEVELS[errorCorrection.getSelectedIndex()])
                .bodyShape(BodyStyle.values()[body.getSelectedIndex()])
                .eyeFrameShape(EyeFrameStyle.values()[frame.getSelectedIndex()])
                .eyeBallShape(EyeBallStyle.values()[ball.getSelectedIndex()])
                .bodyColor(p.body())
                .gradientColor(gradient.isSelected() ? p.gradient() : null)
                .eyeFrameColor(coloredEyes.isSelected() ? p.eye() : null)
                .eyeBallColor(coloredEyes.isSelected() ? p.eye() : null)
                .background(transparent.isSelected() ? null : Color.WHITE)
                .logo(logo > 0 ? logoIcon(logo, 48, new FlatSVGIcon.ColorFilter(c -> Color.BLACK.equals(c) ? logoColor : c)) : null)
                .logoSize(logoSize.getValue() / 100f)
                .build());
        logoSize.setEnabled(logo > 0);
        logoButtons.forEach(Component::repaint);
        updatePresetSelection();
    }

    private boolean isEyePairSupported() {
        return QrCode.isSupported(EyeFrameStyle.values()[frame.getSelectedIndex()], EyeBallStyle.values()[ball.getSelectedIndex()]);
    }

    /**
     * Selects a combo box item without applying it right away.
     */
    private void selectSilently(JComboBox<?> combo, int index) {
        boolean old = applying;
        applying = true;
        combo.setSelectedIndex(index);
        applying = old;
    }

    /**
     * Highlights the preset the sidebar matches, if any.
     */
    private void updatePresetSelection() {
        for (int i = 0; i < PRESETS.length; i++) {
            Preset preset = PRESETS[i];
            boolean match = preset.body().ordinal() == body.getSelectedIndex()
                    && preset.frame().ordinal() == frame.getSelectedIndex()
                    && preset.ball().ordinal() == ball.getSelectedIndex()
                    && preset.palette() == palette
                    && preset.gradient() == gradient.isSelected()
                    && preset.logo() == logo
                    && coloredEyes.isSelected();
            presetCaptions.get(i).putClientProperty(FlatClientProperties.STYLE, match ? SELECTED_CAPTION_STYLE : CAPTION_STYLE);
        }
    }

    /**
     * Saves the preview as a 1024 × 1024 PNG or SVG.
     */
    private void save(String extension, String description) {
        QrCode code = preview.getCode();
        if (code.getMatrix() == null) {
            return;
        }
        // the operating system's own save dialog, not Swing's
        SystemFileChooser chooser = new SystemFileChooser();
        chooser.setFileFilter(new SystemFileChooser.FileNameExtensionFilter(description, extension));
        chooser.setSelectedFile(new File("qr-code." + extension));
        if (chooser.showSaveDialog(this) != SystemFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith("." + extension)) {
            file = new File(file.getParentFile(), file.getName() + "." + extension);
        }
        try {
            if (extension.equals("svg")) {
                Files.writeString(file.toPath(), code.toSvg(1024));
            } else {
                ImageIO.write(code.toImage(1024), "png", file);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Couldn't save the image: " + e.getMessage(), "Save " + description, JOptionPane.ERROR_MESSAGE);
        }
    }

    private static FlatSVGIcon logoIcon(int index, int size, FlatSVGIcon.ColorFilter filter) {
        FlatSVGIcon icon = new FlatSVGIcon("com/swingcraft4j/icons/" + LOGO_FILES[index], size, size);
        icon.setColorFilter(filter);
        return icon;
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
     * Title-case names of enum constants, for combo boxes.
     */
    private static String[] names(Enum<?>[] values) {
        return Arrays.stream(values)
                .map(v -> v.name().charAt(0) + v.name().substring(1).toLowerCase())
                .toArray(String[]::new);
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
     * Palette swatch: a circle shaded from the body to the gradient color, with round rings for selection and focus.
     */
    private record SwatchIcon(Color body, Color gradient) implements Icon {

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
                g2.setColor(body);
                g2.fill(ring(center, SWATCH / 2 + GAP + RING, RING));
            } else if (b.getModel().isRollover()) {
                g2.setColor(UIManager.getColor("Component.borderColor"));
                g2.fill(ring(center, SWATCH / 2 + GAP + RING, RING));
            }

            float start = center - SWATCH / 2;
            g2.setPaint(new GradientPaint(start, start, body, start + SWATCH, start + SWATCH, gradient));
            g2.fill(circle(center, SWATCH / 2));
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
        JPanel card = new JPanel(new MigLayout(layout, columns, rows));
        card.setBorder(BorderFactory.createTitledBorder(title));
        return card;
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
