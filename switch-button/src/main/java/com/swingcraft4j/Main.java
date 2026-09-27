package com.swingcraft4j;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.formdev.flatlaf.util.FontUtils;
import com.swingcraft4j.switchbutton.SwitchIcon;
import com.swingcraft4j.switchbutton.SwitchType;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Demo entry point: every switch type, colors, sizes, and switches in a settings list.
 */
public class Main extends JFrame {

    private static final String[] COLOR_NAMES = {"Accent", "Green", "Yellow", "Red", "Purple"};
    private static final String[] COLORS = {"$Component.accentColor", "$Actions.Green", "$Actions.Yellow", "$Actions.Red", "#a855f7"};

    public Main() {
        super("Switch Button");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        init();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    private void init() {
        JPanel panel = new JPanel(new MigLayout("insets 16,gap 10 10", "[fill][fill,grow]", "[fill][fill][fill][fill,grow]"));
        panel.add(types(), "cell 0 0 2 1");
        panel.add(colors(), "cell 0 1");
        panel.add(sizes(), "cell 0 2");
        panel.add(focus(), "cell 0 3");
        panel.add(settings(), "cell 1 1 1 3");
        add(panel);
    }

    private JPanel types() {
        SwitchType[] types = SwitchType.values();
        JPanel card = card("Types", "wrap " + (types.length + 1) + ",gap 18 8", "[left]8" + "[center]".repeat(types.length), "");
        card.add(new JLabel());
        for (SwitchType type : types) {
            card.add(caption(name(type)));
        }
        for (boolean selected : new boolean[]{false, true}) {
            card.add(caption(selected ? "On" : "Off"));
            for (SwitchType type : types) {
                card.add(switchBox(null, new SwitchIcon(type), selected));
            }
        }
        return card;
    }

    private JPanel colors() {
        JPanel card = card("Colors", "wrap " + COLORS.length + ",gap 18 4", "[center]".repeat(COLORS.length), "");
        for (SwitchType type : new SwitchType[]{SwitchType.CLASSIC, SwitchType.MATERIAL}) {
            for (String color : COLORS) {
                SwitchIcon icon = new SwitchIcon(type);
                icon.setStyle("onColor:" + color);
                card.add(switchBox(null, icon, true));
            }
        }
        for (String name : COLOR_NAMES) {
            card.add(caption(name));
        }
        return card;
    }

    private JPanel sizes() {
        int[][] sizes = {{28, 16}, {SwitchIcon.DEFAULT_WIDTH, SwitchIcon.DEFAULT_HEIGHT}, {48, 26}};
        String[] names = {"Small", "Default", "Large"};
        JPanel card = card("Sizes", "wrap 3,gap 18 4", "[center][center][center]", "[bottom][bottom][]");
        for (SwitchType type : new SwitchType[]{SwitchType.INSET, SwitchType.LINE_TINTED}) {
            for (int[] size : sizes) {
                card.add(switchBox(null, new SwitchIcon(type, size[0], size[1]), true));
            }
        }
        for (String name : names) {
            card.add(caption(name));
        }
        return card;
    }

    private JPanel focus() {
        JPanel card = card("Focus (press Tab)", "wrap 3,gap 18 4", "[center][center][center]", "");

        // ring shown while focused
        JCheckBox ring = switchBox(null, new SwitchIcon(SwitchType.CLASSIC), true);

        // still reachable with Tab and Space, just without the ring
        SwitchIcon noRingIcon = new SwitchIcon(SwitchType.CLASSIC);
        noRingIcon.setFocusPainted(false);
        JCheckBox noRing = switchBox(null, noRingIcon, true);

        // skipped by Tab, so it never shows a ring
        JCheckBox notFocusable = switchBox(null, new SwitchIcon(SwitchType.CLASSIC), true);
        notFocusable.setFocusable(false);

        card.add(ring);
        card.add(noRing);
        card.add(notFocusable);
        card.add(caption("Default"));
        card.add(caption("No ring"));
        card.add(caption("Not focusable"));
        return card;
    }

    private JPanel settings() {
        JPanel card = card("Settings", "wrap 2,gap 12 10", "[grow,fill][right]", "");
        addSetting(card, "Wi-Fi", "Connected to Home", SwitchType.CLASSIC, true, true);
        addSetting(card, "Bluetooth", "Visible to nearby devices", SwitchType.SOFT, false, true);
        addSetting(card, "Airplane mode", "Turns off all radios", SwitchType.MATERIAL, false, true);
        addSetting(card, "Notifications", "Banners and sounds", SwitchType.OVERHANG, true, true);
        addSetting(card, "Location", "Managed by your organization", SwitchType.TONAL, true, false);

        // the dark mode switch swaps the theme with FlatLaf's animated change
        JCheckBox dark = addSetting(card, "Dark mode", "Switch the demo's theme", SwitchType.CLASSIC, FlatLaf.isLafDark(), true);
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
        return card;
    }

    /**
     * Settings row: title and description on the left, a switch on the right.
     */
    private static JCheckBox addSetting(JPanel card, String title, String description, SwitchType type, boolean selected, boolean enabled) {
        JLabel titleLabel = new JLabel(title);
        titleLabel.setEnabled(enabled);
        JPanel text = new JPanel(new MigLayout("insets 0,wrap,gap 0 2", "[]", ""));
        text.add(titleLabel);
        text.add(caption(description));
        JCheckBox box = switchBox(null, new SwitchIcon(type), selected);
        box.setEnabled(enabled);
        card.add(text);
        card.add(box);
        return box;
    }

    /**
     * Check box drawn as a switch.
     */
    private static JCheckBox switchBox(String text, SwitchIcon icon, boolean selected) {
        JCheckBox box = new JCheckBox(text, icon, selected);
        box.setRolloverEnabled(true);
        return box;
    }

    /**
     * Display name from the enum constant, e.g. {@code LINE_TINTED} becomes "Line tinted".
     */
    private static String name(SwitchType type) {
        String words = type.name().replace('_', ' ');
        return words.charAt(0) + words.substring(1).toLowerCase();
    }

    private static JLabel caption(String text) {
        JLabel label = new JLabel(text);
        label.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;");
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
