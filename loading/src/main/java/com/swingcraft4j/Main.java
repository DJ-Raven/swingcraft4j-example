package com.swingcraft4j;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.util.FontUtils;
import com.formdev.flatlaf.util.UIScale;
import com.swingcraft4j.loading.LoadingIcon;
import com.swingcraft4j.loading.LoadingPainter;
import com.swingcraft4j.loading.painter.arc.RingPainter;
import com.swingcraft4j.loading.painter.arc.SpinnerPainter;
import com.swingcraft4j.loading.painter.dots.DotsPainter;
import com.swingcraft4j.loading.painter.shape.BallPainter;
import com.swingcraft4j.loading.painter.shape.BarsPainter;
import com.swingcraft4j.loading.painter.shape.InfinityPainter;
import com.swingcraft4j.loading.painter.text.TextShimmerWavePainter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo entry point: every loading type and size, colors, loaders inside components, and a playground.
 */
public class Main extends JFrame {

    private static final String[] COLOR_NAMES = {"Default", "Accent", "Blue", "Green", "Yellow", "Red", "Purple"};
    private static final String[] COLORS = {null, "$Component.accentColor", "$Actions.Blue", "$Actions.Green",
            "$Actions.Yellow", "$Actions.Red", "#a855f7"};

    private static final String ALL_GROUPS = "all";
    private static final String DEFAULT_TEXT = "Loading...";

    // types shown in the grid and colors sections; newer types are only in the playground for now
    private static final LoadingPainter[] SHOWCASE = {new SpinnerPainter(), new DotsPainter(), new RingPainter(),
            new BallPainter(), new BarsPainter(), new InfinityPainter()};

    public Main() {
        super("Loading");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        init();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    private void init() {
        // each section is a titled card; types span the colors and components rows
        JPanel panel = new JPanel(new MigLayout("insets 16,gap 10 10", "[fill][fill,grow]", "[fill][fill,grow][fill,grow]"));
        panel.add(types(), "cell 0 0 1 2");
        panel.add(colors(), "cell 1 0");
        panel.add(components(), "cell 1 1");
        panel.add(playground(), "cell 0 2 2 1");
        add(panel);
    }

    private JPanel types() {
        JPanel card = card("Types & sizes", "wrap 6,gap 12 10", "[left]8[center][center][center][center][center]", "[center]");
        card.add(new JLabel());
        for (LoadingIcon.Size size : LoadingIcon.Size.values()) {
            card.add(caption(size.name().toLowerCase()));
        }
        for (LoadingPainter type : SHOWCASE) {
            card.add(caption(name(type)));
            for (LoadingIcon.Size size : LoadingIcon.Size.values()) {
                card.add(new JLabel(new LoadingIcon(type, size)));
            }
        }
        return card;
    }

    private JPanel colors() {
        JPanel card = card("Colors", "wrap 3,gap 12 10", "[sg c,center,grow][sg c,center,grow][sg c,center,grow]", "");
        for (int i = 0; i < SHOWCASE.length; i++) {
            LoadingIcon icon = new LoadingIcon(SHOWCASE[i], LoadingIcon.Size.LG);
            icon.setStyle("color:" + COLORS[i + 1]);
            // color name under the loader
            JLabel label = caption(COLOR_NAMES[i + 1]);
            label.setIcon(icon);
            label.setHorizontalTextPosition(SwingConstants.CENTER);
            label.setVerticalTextPosition(SwingConstants.BOTTOM);
            label.setIconTextGap(8);
            card.add(label);
        }
        return card;
    }

    private JPanel components() {
        // a button that shows a spinner while its (simulated) work runs
        LoadingIcon saveIcon = new LoadingIcon(new SpinnerPainter(), LoadingIcon.Size.XS);
        JButton save = new JButton("Save");
        save.addActionListener(e -> {
            save.setEnabled(false);
            save.setIcon(saveIcon);
            save.setText("Saving");
            Timer done = new Timer(2500, ev -> {
                save.setIcon(null);
                save.setText("Save");
                save.setEnabled(true);
            });
            done.setRepeats(false);
            done.start();
        });

        JButton loading = new JButton("Loading", new LoadingIcon(new DotsPainter(), LoadingIcon.Size.XS));

        // a search field whose trailing loader shows while the user types
        JLabel searching = new JLabel(new LoadingIcon(new RingPainter(), LoadingIcon.Size.XS));
        searching.setVisible(false);
        JTextField search = new JTextField(16);
        search.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Type to search");
        search.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, searching);
        Timer idle = new Timer(900, e -> searching.setVisible(false));
        idle.setRepeats(false);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                typing();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                typing();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }

            private void typing() {
                searching.setVisible(true);
                idle.restart();
            }
        });

        JLabel fetching = new JLabel("Fetching data", new LoadingIcon(new BarsPainter(), LoadingIcon.Size.XS), SwingConstants.LEADING);
        fetching.setIconTextGap(8);

        // text loader with custom text
        LoadingIcon thinkingIcon = new LoadingIcon(new TextShimmerWavePainter("Generating response..."));
        thinkingIcon.setSize(26);
        JLabel thinking = new JLabel(thinkingIcon, SwingConstants.LEADING);

        JPanel card = card("Components", "wrap 3,gap 8 10", "[sg btn,fill][sg btn,fill][grow]", "");
        card.add(save, "wmin 110");
        card.add(loading, "wrap");
        card.add(search, "span 3,growx");
        card.add(fetching, "span 3,gapleft 2");
        card.add(thinking, "span 3");
        return card;
    }

    private JPanel playground() {
        LoadingIcon preview = new LoadingIcon(new SpinnerPainter());
        preview.setSize(48);
        JLabel previewLabel = new JLabel(preview, SwingConstants.CENTER);

        // every painter class found under the painter package, grouped by sub-package, plus the demo's custom one
        Map<String, List<LoadingPainter>> groups = new LinkedHashMap<>(PainterCatalog.load());
        groups.put("custom", List.of(new OrbitPainter()));

        JComboBox<String> group = new JComboBox<>();
        group.addItem(ALL_GROUPS);
        groups.keySet().forEach(group::addItem);
        group.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String text = (String) value;
                text = text == null ? null : Character.toUpperCase(text.charAt(0)) + text.substring(1);
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });

        DefaultListModel<LoadingPainter> typeModel = new DefaultListModel<>();
        JList<LoadingPainter> type = new JList<>(typeModel);
        type.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, name((LoadingPainter) value), index, isSelected, cellHasFocus);
                // unselected rows sit on the panel; FlatLaf paints the rounded selection of opaque (selected) rows
                setOpaque(isSelected);
                return this;
            }
        });
        // each color row shows a swatch of the color it applies
        JList<String> color = new JList<>(COLOR_NAMES);
        color.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setIcon(new SwatchIcon(resolveColor(COLORS[index])));
                setIconTextGap(8);
                setOpaque(isSelected);
                return this;
            }
        });

        for (JList<?> list : new JList<?>[]{type, color}) {
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setVisibleRowCount(6);
            list.setSelectedIndex(0);
            // the list sits on its panel; the selection keeps an accent tint
            // even when the list isn't focused, so both lists always show what's picked
            list.setOpaque(false);
            list.putClientProperty(FlatClientProperties.STYLE, "" +
                    "selectionArc:10;" +
                    "selectionInsets:2,6,2,6;" +
                    "cellMargins:5,10,5,10;" +
                    "selectionBackground:fade($Component.accentColor,30%);" +
                    "selectionForeground:$List.foreground;" +
                    "selectionInactiveBackground:fade($Component.accentColor,20%);" +
                    "selectionInactiveForeground:$List.foreground;");
        }

        JSlider size = new JSlider(12, 80, 48);
        JLabel sizeValue = caption("48 px");
        JSlider speed = new JSlider(25, 300, 100);
        JLabel speedValue = caption("100%");

        // the preview sits on an inset stage, with the current settings underneath
        JLabel info = caption("");
        JPanel stage = new JPanel(new MigLayout("insets 10,wrap", "[center,grow]", "[grow,center][]"));
        stage.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:14;" +
                "[dark]background:darken(@background,2%);" +
                "[light]background:lighten(@background,5%);");
        stage.add(previewLabel, "wmin 100,h 100!");
        stage.add(info);

        // speed scales the selected painter's own cycle duration
        Runnable apply = () -> {
            preview.setDuration(preview.getPainter().getDuration() * 100 / speed.getValue());
            sizeValue.setText(size.getValue() + " px");
            speedValue.setText(speed.getValue() + "%");
            info.setText(name(preview.getPainter()) + "  ·  " + size.getValue() + " px  ·  " + preview.getDuration() + " ms");
        };

        // the text type gets an extra field for its text; editing it swaps in a painter with the new text
        JLabel textCaption = caption("Text");
        JTextField text = new JTextField(DEFAULT_TEXT);
        Runnable applyPainter = () -> {
            LoadingPainter selected = type.getSelectedValue();
            boolean isText = selected instanceof TextShimmerWavePainter;
            textCaption.setVisible(isText);
            text.setVisible(isText);
            preview.setPainter(isText ? new TextShimmerWavePainter(text.getText()) : selected);
            apply.run();
        };
        text.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyPainter.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyPainter.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });

        type.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && type.getSelectedValue() != null) {
                applyPainter.run();
            }
        });

        // the group filter refills the list, keeping the selected painter if it's still listed
        group.addActionListener(e -> {
            LoadingPainter current = type.getSelectedValue();
            String selected = (String) group.getSelectedItem();
            typeModel.clear();
            groups.forEach((name, painters) -> {
                if (ALL_GROUPS.equals(selected) || name.equals(selected)) {
                    typeModel.addAll(painters);
                }
            });
            int index = current != null ? typeModel.indexOf(current) : -1;
            type.setSelectedIndex(Math.max(index, 0));
            type.ensureIndexIsVisible(type.getSelectedIndex());
        });
        Runnable selectSpinner = () -> {
            group.setSelectedItem(ALL_GROUPS);
            for (int i = 0; i < typeModel.size(); i++) {
                if (typeModel.get(i) instanceof SpinnerPainter) {
                    type.setSelectedIndex(i);
                    type.ensureIndexIsVisible(i);
                }
            }
        };
        group.setSelectedItem(ALL_GROUPS);
        selectSpinner.run();
        color.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || color.getSelectedIndex() < 0) {
                return;
            }
            String value = COLORS[color.getSelectedIndex()];
            if (value == null) {
                preview.setColor(null);
            } else {
                preview.setStyle("color:" + value);
            }
        });
        size.addChangeListener(e -> {
            preview.setSize(size.getValue());
            apply.run();
        });
        speed.addChangeListener(e -> apply.run());
        apply.run();

        JButton reset = new JButton("Reset");
        reset.addActionListener(e -> {
            selectSpinner.run();
            color.setSelectedIndex(0);
            size.setValue(48);
            speed.setValue(100);
            text.setText(DEFAULT_TEXT);
        });


        // size, speed, (for the text type) the text and reset, under one titled border
        JPanel options = new JPanel(new MigLayout("insets 4,wrap,hidemode 3,gap 0 4", "[fill,grow]", "[][]10[][]10[][][grow,bottom]"));
        options.setBorder(BorderFactory.createTitledBorder("Options"));
        options.add(caption("Size"), "split 2");
        options.add(sizeValue, "gapleft push");
        options.add(size);
        options.add(caption("Speed"), "split 2");
        options.add(speedValue, "gapleft push");
        options.add(speed);
        options.add(textCaption);
        options.add(text);
        options.add(reset, "cell 0 6,growx 0,alignx right");

        // stage | type list | color list | options
        JPanel card = card("Playground", "gap 12 10", "[340!,fill]6[180!,fill][170!,fill]12[fill,grow]", "[fill,grow]");
        card.add(stage);
        card.add(listPanel("Type", group, type));
        card.add(listPanel("Color", null, color));
        card.add(options);
        return card;
    }

    /**
     * Custom painter example: eight dots around a circle with a fading trail.
     */
    private static class OrbitPainter implements LoadingPainter {

        @Override
        public void paint(Graphics2D g, float size, float fraction) {
            Color color = g.getColor();
            float r = size / 10f;
            float radius = size / 2 - r;
            for (int i = 0; i < 8; i++) {
                double angle = 2 * Math.PI * i / 8 - Math.PI / 2;
                float x = size / 2 + radius * (float) Math.cos(angle);
                float y = size / 2 + radius * (float) Math.sin(angle);
                // the dot the head just passed is brightest
                float behind = Math.floorMod((int) (fraction * 8) - i, 8);
                g.setColor(LoadingPainter.alpha(color, 1 - behind / 8f));
                g.fill(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2));
            }
        }

        @Override
        public int getDuration() {
            return 900;
        }
    }

    /**
     * Display name from the class name, e.g. {@code DualRingPainter} becomes "Dual ring"; painters outside the library are marked custom.
     */
    private static String name(LoadingPainter painter) {
        String words = painter.getClass().getSimpleName().replaceAll("Painter$", "").replaceAll("(?<=[a-z])(?=[A-Z])", " ");
        String name = words.charAt(0) + words.substring(1).toLowerCase();
        return painter.getClass().getPackageName().startsWith("com.swingcraft4j.loading.painter") ? name : name + " (custom)";
    }

    private static JLabel caption(String text) {
        JLabel label = new JLabel(text);
        label.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;");
        return label;
    }

    /**
     * Small round color swatch for the color list; {@code null} paints the list's foreground.
     */
    private record SwatchIcon(Color color) implements Icon {

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color != null ? color : c.getForeground());
            g2.fill(new Ellipse2D.Float(x, y, getIconWidth(), getIconHeight()));
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return UIScale.scale(10);
        }

        @Override
        public int getIconHeight() {
            return UIScale.scale(10);
        }
    }

    /**
     * Resolves a style color value: {@code $Key} from UIManager or a {@code #rrggbb} literal.
     */
    private static Color resolveColor(String value) {
        if (value == null) {
            return null;
        }
        return value.startsWith("$") ? UIManager.getColor(value.substring(1)) : Color.decode(value);
    }

    /**
     * List under a titled border, with an optional header (e.g. a filter) above it; the scroll pane has no border of its own.
     */
    private static JPanel listPanel(String title, JComponent header, JList<?> list) {
        JScrollPane scroll = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, "trackArc:999;width:5;thumbInsets:0,0,0,0;");

        JPanel panel = new JPanel(new MigLayout("insets 4,wrap,gap 0 6", "[fill,grow]", "[][fill,grow]"));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        if (header != null) {
            panel.add(header);
        }
        panel.add(scroll, "growy,pushy");
        return panel;
    }

    /**
     * Panel holding one group of examples, framed by a titled border.
     */
    private static JPanel card(String title, String layout, String columns, String rows) {
        JPanel card = new JPanel(new MigLayout("insets 10," + layout, columns, rows));
        card.setBorder(BorderFactory.createTitledBorder(title));
        return card;
    }

    /**
     * Installs the demo's font/theme, then shows the window on the Swing event thread.
     */
    public static void main(String[] args) {
        FlatRobotoFont.install();
        UIManager.put("defaultFont", FontUtils.getCompositeFont(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
        FlatMacDarkLaf.setup();

        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
