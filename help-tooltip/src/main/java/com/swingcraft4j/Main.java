package com.swingcraft4j;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.formdev.flatlaf.util.FontUtils;
import com.swingcraft4j.helptooltip.HelpTooltip;
import com.swingcraft4j.helptooltip.HelpTooltipManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Demo entry point: help tooltips on toolbars along every window edge, with shortcuts, descriptions, links, locations and a tooltip manager.
 */
public class Main extends JFrame {

    private static final String[][] ACTIONS = {
            {"New File", "ctrl N"},
            {"Open...", "ctrl O"},
            {"Save All", "ctrl S"},
            {"Find in Files", "ctrl shift F"},
            {"Reformat Code", "ctrl alt L"},
    };

    public Main() {
        super("Help Tooltip");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        init();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
        // start with nothing focused, so no focus ring shows on the first button
        getRootPane().requestFocusInWindow();
    }

    private void init() {
        JPanel panel = new JPanel(new MigLayout("insets 16,gap 10 10,wrap 2", "[fill][fill,grow]", "[fill][fill,grow]"));
        panel.add(content());
        panel.add(help());
        panel.add(manager());
        panel.add(settings());

        // IDE-like bars on every window edge, each opening its tooltips toward the window's center
        add(topBar(), BorderLayout.NORTH);
        add(leftBar(), BorderLayout.WEST);
        add(rightBar(), BorderLayout.EAST);
        add(bottomBar(), BorderLayout.SOUTH);
        add(panel);
    }

    private JPanel topBar() {
        // like IntelliJ's main toolbar: widgets on the left, run and IDE actions on the right
        JPanel bar = new Bar(new Insets(0, 0, 1, 0), "insets 4 6,gap 2", "[][][]push[][][][][][][]", "");

        bar.add(toolButton(null, "menu", new HelpTooltip()
                .setPlainTextTitle("Main Menu")
                .setShortcut("Alt+\\"), menu("File", "Edit", "View", "Navigate", "Code", "Help")));
        bar.add(dropDown(toolButton("swingcraft4j-example", null, new HelpTooltip()
                .setPlainTextTitle("swingcraft4j-example")
                .setDescription("~/projects/swingcraft4j-example"), menu("Open...", "Recent Projects", "Close Project"))));
        bar.add(dropDown(toolButton("main", "branch", new HelpTooltip()
                .setPlainTextTitle("Git Branch: main")
                .setDescription("Switch branches, update the project, commit and push."), menu("Update Project...", "Commit...", "Push...", "New Branch..."))));

        bar.add(dropDown(toolButton("Main", null, new HelpTooltip()
                .setPlainTextTitle("Run/Debug Configurations")
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)), menu("Main", "All Tests", "Edit Configurations..."))));
        bar.add(toolButton(null, "run", new HelpTooltip()
                .setPlainTextTitle("Run 'Main'")
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)), null));
        bar.add(toolButton(null, "debug", new HelpTooltip()
                .setPlainTextTitle("Debug 'Main'")
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_F9, InputEvent.SHIFT_DOWN_MASK)), null));
        bar.add(toolButton(null, "more", new HelpTooltip()
                .setPlainTextTitle("More Actions"), menu("Run with Coverage", "Profile", "Stop")));
        bar.add(toolButton(null, "search", new HelpTooltip()
                .setPlainTextTitle("Search Everywhere")
                .setShortcut("Double Shift"), null), "gapleft 12");
        bar.add(themeButton());
        bar.add(toolButton(null, "settings", new HelpTooltip()
                .setPlainTextTitle("IDE and Project Settings")
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK)), menu("Settings...", "Plugins...", "Keymap")));
        return bar;
    }

    /**
     * Light/dark switch whose title, a supplier, names the theme it switches to.
     */
    private static JButton themeButton() {
        JButton button = toolButton(null, themeIcon(), new HelpTooltip()
                .setTitle(() -> FlatLaf.isLafDark() ? "Switch to Light Theme" : "Switch to Dark Theme"), null);
        button.addActionListener(e -> {
            FlatAnimatedLafChange.showSnapshot();
            if (FlatLaf.isLafDark()) {
                FlatMacLightLaf.setup();
            } else {
                FlatMacDarkLaf.setup();
            }
            FlatLaf.updateUI();
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
            button.setIcon(icon(themeIcon()));
        });
        return button;
    }

    private static String themeIcon() {
        return FlatLaf.isLafDark() ? "sun" : "moon";
    }

    private JPanel leftBar() {
        // tool window stripe on the left edge, tooltips to the right
        JPanel bar = new Bar(new Insets(0, 0, 0, 1), "wrap,insets 6 5,gap 0 4", "", "");
        HelpTooltip.Alignment right = HelpTooltip.Alignment.RIGHT;
        JToggleButton project = stripButton("folder", right, new HelpTooltip()
                .setPlainTextTitle("Project")
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.ALT_DOWN_MASK)));
        project.setSelected(true);
        bar.add(project);
        bar.add(stripButton("commit", right, new HelpTooltip()
                .setPlainTextTitle("Commit")
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.ALT_DOWN_MASK))));
        bar.add(stripButton("structure", right, new HelpTooltip()
                .setPlainTextTitle("Structure")
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_7, InputEvent.ALT_DOWN_MASK))));
        bar.add(stripButton("bookmark", right, new HelpTooltip()
                .setPlainTextTitle("Bookmarks")
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.ALT_DOWN_MASK))));
        return bar;
    }

    private JPanel rightBar() {
        // tool window stripe on the right edge, tooltips to the left
        JPanel bar = new Bar(new Insets(0, 1, 0, 0), "wrap,insets 6 5,gap 0 4", "", "");
        HelpTooltip.Alignment left = HelpTooltip.Alignment.LEFT;
        bar.add(stripButton("notification", left, new HelpTooltip()
                .setPlainTextTitle("Notifications")
                .setDescription("No new notifications.")));
        bar.add(stripButton("database", left, new HelpTooltip()
                .setPlainTextTitle("Database")));
        bar.add(stripButton("maven", left, new HelpTooltip()
                .setPlainTextTitle("Maven")
                .setDescription("Reload the project, run goals and browse its dependencies.")));
        return bar;
    }

    private JPanel bottomBar() {
        // status bar on the bottom edge, tooltips above
        JPanel bar = new Bar(new Insets(1, 0, 0, 0), "insets 3 6,gap 2", "[][][]push[][][][]", "");
        HelpTooltip.Alignment top = HelpTooltip.Alignment.TOP;
        bar.add(stripButton("terminal", top, new HelpTooltip()
                .setPlainTextTitle("Terminal")
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.ALT_DOWN_MASK))));
        bar.add(stripButton("problems", top, new HelpTooltip()
                .setPlainTextTitle("Problems")
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_6, InputEvent.ALT_DOWN_MASK))));
        bar.add(stripButton("services", top, new HelpTooltip()
                .setPlainTextTitle("Services")
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_8, InputEvent.ALT_DOWN_MASK))));
        bar.add(toolButton("42:17", null, top, new HelpTooltip()
                .setPlainTextTitle("Go to Line:Column")
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK)), null));
        bar.add(toolButton("LF", null, top, new HelpTooltip()
                .setPlainTextTitle("Line Separator: \\n"), menu("CRLF - Windows (\\r\\n)", "LF - Unix and macOS (\\n)", "CR - Classic Mac OS (\\r)")));
        bar.add(toolButton("UTF-8", null, top, new HelpTooltip()
                .setPlainTextTitle("File Encoding: UTF-8"), menu("UTF-8", "UTF-16", "ISO-8859-1")));
        bar.add(toolButton("4 spaces", null, top, new HelpTooltip()
                .setPlainTextTitle("Indent: 4 spaces")
                .setDescription("Taken from the project's code style settings."), null));
        return bar;
    }

    /**
     * Tool window button in a stripe, with its tooltip on the given side.
     */
    private static JToggleButton stripButton(String icon, HelpTooltip.Alignment alignment, HelpTooltip tooltip) {
        JToggleButton button = new JToggleButton(icon(icon));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.setFocusable(false);
        tooltip.setLocation(alignment).setInitialDelay(100).installOn(button);
        return button;
    }

    private static JButton toolButton(String text, String icon, HelpTooltip tooltip, JPopupMenu popup) {
        return toolButton(text, icon, HelpTooltip.Alignment.BOTTOM, tooltip, popup);
    }

    /**
     * Toolbar button with its tooltip on the given side; a popup, if any, opens on click and hides the tooltip while open.
     */
    private static JButton toolButton(String text, String icon, HelpTooltip.Alignment alignment, HelpTooltip tooltip, JPopupMenu popup) {
        JButton button = new JButton(text, icon != null ? icon(icon) : null);
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.setFocusable(false);
        tooltip.setLocation(alignment).setInitialDelay(100).installOn(button);
        if (popup != null) {
            // a status bar popup opens upward, like its tooltip
            button.addActionListener(e -> popup.show(button, 0, alignment == HelpTooltip.Alignment.TOP
                    ? -popup.getPreferredSize().height : button.getHeight()));
            HelpTooltip.setMasterPopup(button, popup);
        }
        return button;
    }

    /**
     * Turns a text-only toolbar button into a widget with a trailing chevron.
     */
    private static JButton dropDown(JButton button) {
        if (button.getIcon() == null) {
            button.setIcon(icon("chevron"));
            button.setHorizontalTextPosition(SwingConstants.LEADING);
        }
        return button;
    }

    /**
     * Popup menu that follows theme changes; FlatLaf.updateUI() only reaches popups while they're open.
     */
    private static JPopupMenu menu(String... items) {
        JPopupMenu menu = new JPopupMenu();
        for (String item : items) {
            menu.add(item);
        }
        UIManager.addPropertyChangeListener(e -> {
            if ("lookAndFeel".equals(e.getPropertyName())) {
                SwingUtilities.updateComponentTreeUI(menu);
            }
        });
        return menu;
    }

    private static Icon icon(String name) {
        return new FlatSVGIcon("com/swingcraft4j/icons/" + name + ".svg");
    }

    private JPanel content() {
        // one kind of content per button; the edge bars show the side locations, these use the default cursor one
        JPanel card = card("Content", "wrap 3,gap 8", "[fill,sg][fill,sg][fill,sg]", "");
        card.add(caption("Each opens under the cursor, the default location"), "span 3,gapbottom 4");
        card.add(sample("Title", new HelpTooltip()
                .setPlainTextTitle("Project Structure")));
        card.add(sample("Shortcut", new HelpTooltip()
                .setPlainTextTitle("Find in Files")
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))));
        card.add(sample("Shortcut only", new HelpTooltip()
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))));
        card.add(sample("HTML title", new HelpTooltip()
                .setTitle("Rename <b>Main.java</b>")
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_F6, InputEvent.SHIFT_DOWN_MASK))));
        card.add(sample("Description", new HelpTooltip()
                .setPlainTextTitle("Debug 'Main'")
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_F9, InputEvent.SHIFT_DOWN_MASK))
                .setDescription("Runs the application with the debugger attached, stopping at breakpoints.")));
        card.add(sample("Paragraphs", new HelpTooltip()
                .setPlainTextTitle("Push")
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))
                .setDescription("Sends the local commits to the <b>remote</b> branch.<p>Pull first if the remote has commits you don't have yet.")));
        card.add(sample("Long title", new HelpTooltip()
                .setPlainTextTitle("Analyze data flow to here, including every assignment that can reach this expression")
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_F7, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK))
                .setDescription("Wrapped at the max width, then trimmed to the widest line.")));
        card.add(sample("Link", new HelpTooltip()
                .setPlainTextTitle("Commit")
                .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK))
                .setDescription("Records the staged changes in the local repository.")
                .setLink("Commit settings", () -> System.out.println("Open commit settings"))));
        card.add(sample("Browser link", new HelpTooltip()
                .setPlainTextTitle("Documentation")
                .setDescription("Opens the FlatLaf documentation in your browser.")
                .setBrowserLink("flatlaf.com", URI.create("https://www.formdev.com/flatlaf/"))));
        return card;
    }

    private static JButton sample(String text, HelpTooltip tooltip) {
        JButton button = new JButton(text);
        tooltip.installOn(button);
        return button;
    }

    private JPanel help() {
        JPanel card = card("Show & hide", "wrap 2,gap 10 12", "[][grow]", "");

        // a help button's tooltip never hides on timeout
        JButton helpButton = new JButton();
        helpButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_HELP);
        new HelpTooltip()
                .setPlainTextTitle("Proxy settings")
                .setDescription("Traffic goes through the proxy unless the host matches an exception.<p>Separate exceptions with commas.")
                .setBrowserLink("Learn more", URI.create("https://www.formdev.com/flatlaf/"))
                .setLocation(HelpTooltip.Alignment.HELP_BUTTON)
                .installOn(helpButton);
        card.add(helpButton);
        card.add(caption("Stays open until the mouse leaves"));

        // the popup menu takes over, so the tooltip doesn't cover it
        JButton more = new JButton("More ▾");
        JPopupMenu menu = menu("Rename...", "Duplicate", "Delete");
        more.addActionListener(e -> menu.show(more, 0, more.getHeight()));
        new HelpTooltip()
                .setPlainTextTitle("More actions")
                .setShortcut("Alt+Enter")
                .installOn(more);
        HelpTooltip.setMasterPopup(more, menu);
        card.add(more);
        card.add(caption("Hidden while its menu is open"));

        // closes on its own well before the default 10 s
        JButton quick = new JButton("Quick");
        new HelpTooltip()
                .setPlainTextTitle("Quick tooltip")
                .setShortcut("Ctrl+Q")
                .setDismissDelay(2000)
                .installOn(quick);
        card.add(quick);
        card.add(caption("Closes on its own after 2 s"));

        // waits longer than the default 500 ms before showing
        JButton slow = new JButton("Slow");
        new HelpTooltip()
                .setPlainTextTitle("Slow tooltip")
                .setShortcut("Ctrl+W")
                .setInitialDelay(1500)
                .installOn(slow);
        card.add(slow);
        card.add(caption("Shows after 1.5 s"));

        // gives the mouse time to wander off and come back
        card.add(sample("Lingering", new HelpTooltip()
                .setPlainTextTitle("Lingering tooltip")
                .setHideDelay(1000)));
        card.add(caption("Hides 1 s after the mouse leaves"));

        // no auto close; Escape hides it while it stays installed
        JButton pinned = sample("Pinned", new HelpTooltip()
                .setPlainTextTitle("Pinned tooltip")
                .setDescription("Never closes on its own. Press Esc to hide it.")
                .setNeverHideOnTimeout(true));
        pinned.registerKeyboardAction(e -> HelpTooltip.hide(pinned),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        card.add(pinned);
        card.add(caption("Never closes on its own, Esc hides it"));

        // the supplier runs on every show, so the time is always current
        card.add(sample("Live title", new HelpTooltip()
                .setTitle(() -> "Now: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
        card.add(caption("Title recomputed on every show"));

        // the condition is checked when the tooltip is about to show
        JButton focused = sample("Focused only", new HelpTooltip()
                .setPlainTextTitle("Shown while the window is focused"));
        HelpTooltip.setMasterPopupOpenCondition(focused, this::isFocused);
        card.add(focused);
        card.add(caption("Only while this window is focused"));
        return card;
    }

    private JPanel manager() {
        JPanel card = card("Tooltip manager", "wrap,gap 0 6", "[fill,grow]", "[][fill,grow]");

        // tooltip text per row, shown by the manager with the row's shortcut next to it
        JList<String[]> list = new JList<>(ACTIONS) {
            @Override
            public String getToolTipText(MouseEvent e) {
                String[] action = actionAt(this, e.getPoint());
                return action != null ? action[0] : null;
            }
        };
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused) {
                return super.getListCellRendererComponent(list, ((String[]) value)[0], index, selected, focused);
            }
        });
        list.setVisibleRowCount(ACTIONS.length);
        // the manager asks for the shortcut while handling the mouse event, so the mouse is over the row
        list.putClientProperty(HelpTooltipManager.SHORTCUT_PROPERTY, (Supplier<String>) () -> {
            Point p = list.getMousePosition();
            String[] action = p != null ? actionAt(list, p) : null;
            return action != null ? HelpTooltip.getShortcutText(KeyStroke.getKeyStroke(action[1])) : null;
        });
        new HelpTooltipManager().register(list);

        card.add(caption("Hover a row: its tooltip text plus a shortcut"));
        card.add(new JScrollPane(list));
        return card;
    }

    private JPanel settings() {
        JPanel card = card("Settings", "wrap,gap 0 8", "[fill,grow]", "");

        HelpTooltip tooltip = new HelpTooltip()
                .setPlainTextTitle("Sample action")
                .setShortcut("Ctrl+Alt+S");
        JButton sample = new JButton("Sample");
        tooltip.installOn(sample);

        // disabling keeps the tooltip installed, just silent
        JCheckBox disabled = new JCheckBox("Disable the tooltip");
        disabled.addActionListener(e -> {
            if (disabled.isSelected()) {
                HelpTooltip.disableTooltip(sample);
            } else {
                HelpTooltip.enableTooltip(sample);
            }
        });

        // disposing removes its listeners; installing it again brings it back
        JCheckBox uninstalled = new JCheckBox("Uninstall the tooltip");
        uninstalled.addActionListener(e -> {
            if (uninstalled.isSelected()) {
                HelpTooltip.dispose(sample);
            } else {
                tooltip.installOn(sample);
            }
        });

        card.add(caption("Both apply to the button below"));
        card.add(disabled);
        card.add(uninstalled);
        card.add(sample, "grow 0,gaptop 4");
        return card;
    }

    private static String[] actionAt(JList<String[]> list, Point p) {
        int index = list.locationToIndex(p);
        return index >= 0 && list.getCellBounds(index, index).contains(p) ? list.getModel().getElementAt(index) : null;
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
     * Panel with a line on the given sides in the theme's border color.
     */
    private static class Bar extends JPanel {

        private final Insets sides;

        Bar(Insets sides, String layout, String columns, String rows) {
            super(new MigLayout(layout, columns, rows));
            this.sides = sides;
            updateUI();
        }

        @Override
        public void updateUI() {
            super.updateUI();
            // null while the super constructor calls this
            if (sides != null) {
                setBorder(BorderFactory.createMatteBorder(sides.top, sides.left, sides.bottom, sides.right,
                        UIManager.getColor("Component.borderColor")));
            }
        }
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
