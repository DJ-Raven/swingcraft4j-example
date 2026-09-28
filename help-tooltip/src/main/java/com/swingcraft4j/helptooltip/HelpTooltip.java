package com.swingcraft4j.helptooltip;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatEmptyBorder;
import com.formdev.flatlaf.ui.FlatLineBorder;
import com.formdev.flatlaf.util.ColorFunctions;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Rich help tooltip (title, shortcut, description and link) in the style of IntelliJ's {@code HelpTooltip}.
 */
public class HelpTooltip {

    private static final String TOOLTIP_PROPERTY = "JComponent.helpTooltip";
    private static final String TOOLTIP_DISABLED_PROPERTY = "JComponent.helpTooltipDisabled";
    private static final String PARAGRAPH_SPLITTER = "<p/?>";

    // delays in ms, the same defaults as IntelliJ's registry
    private static final int INITIAL_DELAY = 500;
    private static final int RESHOW_DELAY = 500;
    private static final int EXIT_HIDE_DELAY = 150;
    private static final int REGULAR_DISMISS_DELAY = 10_000;
    private static final int FULL_DISMISS_DELAY = 30_000;

    private Supplier<String> title;
    private String shortcut;
    private String description;
    private LinkButton link;
    private boolean neverHide;
    private Alignment alignment = Alignment.CURSOR;
    private BooleanSupplier masterPopupOpenCondition;

    private Popup popup;
    private Component popupOwner;
    private Timer timer;
    private boolean isOverPopup;
    private boolean isMultiline;
    private MouseListener overTipListener;
    private int initialDelay = -1;
    private int hideDelay = -1;
    private int dismissDelay = -1;
    private String toolTipText;
    private boolean initialShowScheduled;

    protected MouseAdapter mouseListener = new MouseAdapter() {
    };

    /**
     * Location of the tooltip relative to the owner component.
     */
    public enum Alignment {
        RIGHT {
            @Override
            public Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation) {
                return new Point(owner.getWidth() + UIScale.scale(5) - xOffset(), UIScale.scale(1) + yOffset());
            }
        },
        LEFT {
            @Override
            public Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation) {
                return new Point(-popupSize.width - UIScale.scale(5) + xOffset(), UIScale.scale(1) + yOffset());
            }
        },
        TOP {
            @Override
            public Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation) {
                return new Point(UIScale.scale(1) + xOffset(), -UIScale.scale(5) - popupSize.height + yOffset());
            }
        },
        BOTTOM {
            @Override
            public Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation) {
                return new Point(UIScale.scale(1) + xOffset(), UIScale.scale(5) + owner.getHeight() - yOffset());
            }
        },
        HELP_BUTTON {
            @Override
            public Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation) {
                Insets i = ((JComponent) owner).getInsets();
                return new Point(xOffset() - UIScale.scale(40), i.top + yOffset() - UIScale.scale(6) - popupSize.height);
            }
        },
        CURSOR {
            @Override
            public Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation) {
                Point location = mouseLocation.getLocation();
                location.y += uiInt("HelpTooltip.mouseCursorOffset", 20);

                SwingUtilities.convertPointToScreen(location, owner);
                Rectangle r = new Rectangle(location, popupSize);
                moveToFit(r, screenBounds(owner));
                location = r.getLocation();
                SwingUtilities.convertPointFromScreen(location, owner);
                r.setLocation(location);

                // flip above the cursor when fitting on screen pushed it under the cursor
                if (r.contains(mouseLocation)) {
                    location.y = mouseLocation.y - r.height - UIScale.scale(5);
                }
                return location;
            }
        };

        public abstract Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation);
    }

    /**
     * Sets the title; it may contain HTML, without the enclosing {@code <html>} tags.
     */
    public HelpTooltip setTitle(String title) {
        this.title = title != null ? () -> title : null;
        return this;
    }

    /**
     * Sets a title supplier, asked again every time the tooltip shows; it may return HTML.
     */
    public HelpTooltip setTitle(Supplier<String> title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the title as plain text, escaping any HTML in it.
     */
    public HelpTooltip setPlainTextTitle(String title) {
        this.title = title != null ? () -> escape(title) : null;
        return this;
    }

    /**
     * Sets the shortcut text shown next to the title, e.g. {@code "Ctrl+Shift+N"}.
     */
    public HelpTooltip setShortcut(String shortcut) {
        this.shortcut = shortcut;
        return this;
    }

    public HelpTooltip setShortcut(KeyStroke shortcut) {
        this.shortcut = shortcut != null ? getShortcutText(shortcut) : null;
        return this;
    }

    /**
     * Sets the description; it may contain HTML, and {@code <p>} starts a new paragraph.
     */
    public HelpTooltip setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Adds a link below the description that hides the tooltip and runs the action.
     */
    public HelpTooltip setLink(String linkText, Runnable linkAction) {
        return setLink(linkText, linkAction, false);
    }

    /**
     * Adds a link below the description; {@code external} paints an arrow after the text.
     */
    public HelpTooltip setLink(String linkText, Runnable linkAction, boolean external) {
        link = new LinkButton(linkText, external, () -> {
            hidePopup(true);
            linkAction.run();
        });
        return this;
    }

    /**
     * Adds an external link that opens the URI in the browser.
     */
    public HelpTooltip setBrowserLink(String linkText, URI uri) {
        return setLink(linkText, () -> {
            try {
                Desktop.getDesktop().browse(uri);
            } catch (Exception ignored) {
            }
        }, true);
    }

    /**
     * Delay in ms between the mouse entering the owner and the tooltip showing.
     */
    public HelpTooltip setInitialDelay(int delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("Negative delay is not allowed");
        }
        initialDelay = delay;
        return this;
    }

    /**
     * Delay in ms between the mouse leaving the owner and the tooltip hiding.
     */
    public HelpTooltip setHideDelay(int delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("Negative delay is not allowed");
        }
        hideDelay = delay;
        return this;
    }

    /**
     * How long in ms the tooltip stays open before closing on its own, for one-line and multiline tooltips alike.
     */
    public HelpTooltip setDismissDelay(int delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("Negative delay is not allowed");
        }
        dismissDelay = delay;
        return this;
    }

    /**
     * Keeps the tooltip open instead of closing after 10 s (one line) or 30 s (multiline).
     */
    public HelpTooltip setNeverHideOnTimeout(boolean neverHide) {
        this.neverHide = neverHide;
        return this;
    }

    public HelpTooltip setLocation(Alignment alignment) {
        this.alignment = alignment;
        return this;
    }

    /**
     * Installs the tooltip on the component, replacing one installed before.
     */
    public void installOn(JComponent component) {
        HelpTooltip installed = getTooltipFor(component);
        if (installed == this) {
            return;
        }
        if (installed != null) {
            installed.hideAndDispose(component);
        }
        neverHide = neverHide || isHelpButton(component);
        createMouseListeners();
        component.putClientProperty(TOOLTIP_PROPERTY, this);
        component.addMouseListener(mouseListener);
        component.addMouseMotionListener(mouseListener);
    }

    /**
     * Whether a mouse exit may hide the tooltip even when the mouse moved onto it; only a link makes it worth reaching.
     */
    protected boolean shouldForceHiding() {
        return link == null;
    }

    protected void createMouseListeners() {
        mouseListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // back on the owner before the hide delay ran out: keep the shown tooltip
                if (popup != null && popupOwner == e.getComponent()) {
                    cancelTimer();
                    if (!neverHide) {
                        scheduleHide(true, dismissDelay());
                    }
                    return;
                }
                closePopup();
                initialShowScheduled = true;
                scheduleShow(e, initialDelay != -1 ? initialDelay : INITIAL_DELAY);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                scheduleHide(shouldForceHiding(), hideDelay != -1 ? hideDelay : EXIT_HIDE_DELAY);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (!initialShowScheduled) {
                    scheduleShow(e, RESHOW_DELAY);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                hidePopup(true);
            }
        };
    }

    /**
     * Builds the tooltip content from the current title, shortcut, description and link.
     */
    protected JPanel createTipPanel() {
        isMultiline = false;

        // one left-aligned column
        JPanel tipPanel = new JPanel();
        tipPanel.setLayout(new BoxLayout(tipPanel, BoxLayout.Y_AXIS));
        tipPanel.setBackground(UIManager.getColor("ToolTip.background"));

        String currentTitle = title != null ? title.get() : null;
        boolean hasTitle = !isEmpty(currentTitle);
        boolean hasDescription = !isEmpty(description);

        if (hasTitle) {
            addRow(tipPanel, new Header(currentTitle, hasDescription));
        }

        if (hasDescription) {
            String[] paragraphs = description.split(PARAGRAPH_SPLITTER);
            isMultiline = paragraphs.length > 1;
            for (String p : paragraphs) {
                if (!p.isEmpty()) {
                    addRow(tipPanel, new Paragraph(p, hasTitle));
                }
            }
        }

        if (!hasTitle && !isEmpty(shortcut)) {
            JLabel shortcutLabel = new JLabel(shortcut);
            shortcutLabel.setFont(deriveDescriptionFont(shortcutLabel.getFont(), false));
            shortcutLabel.setForeground(shortcutForeground());
            addRow(tipPanel, shortcutLabel);
        }

        if (link != null) {
            // the link lives outside any component tree between shows, so pick up theme changes here
            link.updateUI();
            link.setForeground(uiColor("ToolTip.linkForeground", UIManager.getColor("Component.linkColor")));
            link.setFont(deriveDescriptionFont(UIManager.getFont("Label.font"), hasTitle));
            addRow(tipPanel, link);
        }

        isMultiline = isMultiline || hasDescription && (hasTitle || link != null);
        tipPanel.setBorder(tipBorder(isMultiline));
        putPopupBorderStyle(tipPanel);
        // heavy weight, so FlatLaf gives it a drop shadow and rounded corners and it can extend past the window
        tipPanel.putClientProperty(FlatClientProperties.POPUP_FORCE_HEAVY_WEIGHT, true);
        return tipPanel;
    }

    /**
     * Adds a left-aligned row, after the theme's vertical gap when it isn't the first.
     */
    private static void addRow(JPanel tipPanel, JComponent row) {
        if (tipPanel.getComponentCount() > 0) {
            tipPanel.add(Box.createVerticalStrut(UIScale.scale(uiInt("HelpTooltip.verticalGap", 4))));
        }
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        tipPanel.add(row);
    }

    private void hideAndDispose(JComponent owner) {
        hidePopup(true);
        owner.removeMouseListener(mouseListener);
        owner.removeMouseMotionListener(mouseListener);
        masterPopupOpenCondition = null;
        owner.putClientProperty(TOOLTIP_PROPERTY, null);
    }

    private void scheduleShow(MouseEvent e, int delay) {
        cancelTimer();
        if (isTooltipDisabled(e.getComponent())) {
            return;
        }

        schedule(delay, () -> {
            initialShowScheduled = false;
            if (masterPopupOpenCondition != null && !masterPopupOpenCondition.getAsBoolean()) {
                return;
            }

            Component owner = e.getComponent();
            if (!owner.isShowing()) {
                return;
            }
            String text = owner instanceof JComponent c ? c.getToolTipText(e) : null;
            if (popup != null) {
                // keep the shown tooltip while the owner's tooltip text stays the same
                if (isEmpty(text) && isEmpty(toolTipText) || Objects.equals(text, toolTipText)) {
                    // the mouse move that got here cancelled the dismiss timer, so restart it
                    if (!neverHide) {
                        scheduleHide(true, dismissDelay());
                    }
                    return;
                }
                closePopup();
            }

            toolTipText = text;
            JPanel tipPanel = createTipPanel();
            overTipListener = createIsOverTipMouseListener(tipPanel);
            tipPanel.addMouseListener(overTipListener);
            if (link != null) {
                link.addMouseListener(overTipListener);
            }

            Dimension size = tipPanel.getPreferredSize();
            Point location = alignment.getPointFor(owner, size, e.getPoint());
            SwingUtilities.convertPointToScreen(location, owner);
            Rectangle r = new Rectangle(location, size);
            moveToFit(r, screenBounds(owner));

            popup = PopupFactory.getSharedInstance().getPopup(owner, tipPanel, r.x, r.y);
            popupOwner = owner;
            popup.show();

            if (!neverHide) {
                scheduleHide(true, dismissDelay());
            }
        });
    }

    /**
     * Tracks whether the mouse is over the tooltip, so it stays open while the user reaches for the link.
     */
    protected MouseListener createIsOverTipMouseListener(JComponent tipPanel) {
        return new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isOverPopup = true;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // moving between the panel and the link inside it isn't leaving the tooltip
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), tipPanel);
                if (!tipPanel.contains(p)) {
                    isOverPopup = false;
                    hidePopup(false);
                }
            }
        };
    }

    /**
     * The set dismiss delay, else the theme's one for one-line or multiline tooltips.
     */
    private int dismissDelay() {
        if (dismissDelay != -1) {
            return dismissDelay;
        }
        return isMultiline
                ? uiInt("HelpTooltip.fullDismissDelay", FULL_DISMISS_DELAY)
                : uiInt("HelpTooltip.regularDismissDelay", REGULAR_DISMISS_DELAY);
    }

    private void scheduleHide(boolean force, int delay) {
        schedule(delay, () -> hidePopup(force));
    }

    /**
     * Hides the tooltip; unless {@code force}, it stays open while the mouse is over it.
     */
    protected void hidePopup(boolean force) {
        initialShowScheduled = false;
        cancelTimer();
        if (popup != null && (!isOverPopup || force)) {
            closePopup();
        }
    }

    private void closePopup() {
        if (popup != null) {
            popup.hide();
        }
        if (link != null && overTipListener != null) {
            link.removeMouseListener(overTipListener);
        }
        popup = null;
        popupOwner = null;
        overTipListener = null;
        toolTipText = null;
        isOverPopup = false;
    }

    private void schedule(int delay, Runnable runnable) {
        cancelTimer();
        timer = new Timer(delay, e -> runnable.run());
        timer.setRepeats(false);
        timer.start();
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    private String htmlTitle(String currentTitle) {
        return BasicHTML.isHTMLString(currentTitle) ? currentTitle : "<html><div>" + currentTitle + getShortcutAsHtml(shortcut) + "</div></html>";
    }

    /**
     * Label for HTML text that, once wrapped, shrinks to its widest line instead of the full wrap width.
     */
    private static class BoundWidthLabel extends JLabel {

        void setSizeForWidth(float width) {
            if (width > maxWidth() && getClientProperty(BasicHTML.propertyKey) instanceof View v) {
                width = 0;
                for (View row : getRows(v)) {
                    width = Math.max(width, row.getPreferredSpan(View.X_AXIS));
                }
                v.setSize(width, v.getPreferredSpan(View.Y_AXIS));
            }
        }

        private static List<View> getRows(View root) {
            List<View> rows = new ArrayList<>();
            visit(root, rows);
            return rows;
        }

        private static void visit(View v, List<View> result) {
            String name = v.getClass().getCanonicalName();
            if (name != null && name.contains("ParagraphView.Row")) {
                result.add(v);
            }
            for (int i = 0; i < v.getViewCount(); i++) {
                visit(v.getView(i), result);
            }
        }
    }

    /**
     * Title with the shortcut after it, wrapped at the max width when there's a description too.
     */
    private class Header extends BoundWidthLabel {

        /**
         * Takes the title already fetched for this show, so a supplier runs once per show.
         */
        Header(String currentTitle, boolean obeyWidth) {
            setFont(deriveHeaderFont(getFont()));
            setForeground(UIManager.getColor("ToolTip.foreground"));

            int maxWidth = maxWidth();
            if (obeyWidth || currentTitle.length() > maxWidth) {
                String shortcutHtml = getShortcutAsHtml(shortcut);
                View v = BasicHTML.createHTMLView(this, "<html>" + currentTitle + shortcutHtml + "</html>");
                float width = v.getPreferredSpan(View.X_AXIS);
                isMultiline = isMultiline || width > maxWidth;
                setText("<html>" + div(width > maxWidth) + currentTitle + shortcutHtml + "</div></html>");
                setSizeForWidth(width);
            } else {
                setText(htmlTitle(currentTitle));
            }
        }
    }

    /**
     * One description paragraph, wrapped at the max width.
     */
    private class Paragraph extends BoundWidthLabel {

        Paragraph(String text, boolean hasTitle) {
            setForeground(hasTitle ? infoForeground() : UIManager.getColor("ToolTip.foreground"));
            setFont(deriveDescriptionFont(getFont(), hasTitle));

            View v = BasicHTML.createHTMLView(this, "<html>" + text + "</html>");
            float width = v.getPreferredSpan(View.X_AXIS);
            isMultiline = isMultiline || width > maxWidth();
            setText("<html>" + div(width > maxWidth()) + text + "</div></html>");
            setSizeForWidth(width);
        }
    }

    public static HelpTooltip getTooltipFor(JComponent owner) {
        return owner.getClientProperty(TOOLTIP_PROPERTY) instanceof HelpTooltip t ? t : null;
    }

    /**
     * Hides the tooltip installed on the component and removes its listeners.
     */
    public static void dispose(Component owner) {
        if (owner instanceof JComponent c && getTooltipFor(c) instanceof HelpTooltip t) {
            t.hideAndDispose(c);
        }
    }

    /**
     * Hides the tooltip installed on the component, keeping it installed.
     */
    public static void hide(Component owner) {
        if (owner instanceof JComponent c && getTooltipFor(c) instanceof HelpTooltip t) {
            t.hidePopup(true);
        }
    }

    /**
     * Keeps the tooltip from showing while the owner's popup menu is open.
     */
    public static void setMasterPopup(Component owner, JPopupMenu master) {
        WeakReference<JPopupMenu> ref = new WeakReference<>(master);
        setMasterPopupOpenCondition(owner, () -> {
            JPopupMenu menu = ref.get();
            return menu == null || !menu.isVisible();
        });
    }

    /**
     * The tooltip shows only while the condition returns {@code true}.
     */
    public static void setMasterPopupOpenCondition(Component owner, BooleanSupplier condition) {
        if (owner instanceof JComponent c && getTooltipFor(c) instanceof HelpTooltip t) {
            t.masterPopupOpenCondition = condition;
        }
    }

    public static void disableTooltip(Component source) {
        if (source instanceof JComponent c) {
            c.putClientProperty(TOOLTIP_DISABLED_PROPERTY, true);
        }
    }

    public static void enableTooltip(Component source) {
        if (source instanceof JComponent c) {
            c.putClientProperty(TOOLTIP_DISABLED_PROPERTY, null);
        }
    }

    private static boolean isTooltipDisabled(Component component) {
        return component instanceof JComponent c && Boolean.TRUE.equals(c.getClientProperty(TOOLTIP_DISABLED_PROPERTY));
    }

    /**
     * FlatLaf help button ({@code JButton.buttonType = help}), whose tooltip never hides on timeout.
     */
    private static boolean isHelpButton(Component c) {
        return c instanceof AbstractButton b
                && FlatClientProperties.BUTTON_TYPE_HELP.equals(b.getClientProperty(FlatClientProperties.BUTTON_TYPE));
    }

    /**
     * Shortcut as HTML to put after the title, in the shortcut color.
     */
    public static String getShortcutAsHtml(String shortcut) {
        if (isEmpty(shortcut)) {
            return "";
        }
        Color c = shortcutForeground();
        return String.format("&nbsp;&nbsp;<font color=\"#%06x\">%s</font>", c.getRGB() & 0xffffff, escape(shortcut));
    }

    /**
     * Key stroke as text, e.g. {@code Ctrl+Shift+N}.
     */
    public static String getShortcutText(KeyStroke keyStroke) {
        String modifiers = KeyEvent.getModifiersExText(keyStroke.getModifiers());
        String key = KeyEvent.getKeyText(keyStroke.getKeyCode());
        return modifiers.isEmpty() ? key : modifiers + "+" + key;
    }

    private static Border tipBorder(boolean multiline) {
        Insets insets = multiline
                ? uiInsets("HelpTooltip.defaultTextBorderInsets", new Insets(8, 10, 10, 16))
                : uiInsets("HelpTooltip.smallTextBorderInsets", new Insets(4, 8, 5, 8));
        // follow the theme's ToolTip.border: light themes draw a line, dark themes don't
        return UIManager.getBorder("ToolTip.border") instanceof FlatLineBorder b
                ? new FlatLineBorder(insets, b.getLineColor(), b.getLineThickness(), b.getArc())
                : new FlatEmptyBorder(insets);
    }

    /**
     * Rounded border like a JToolTip's; FlatLaf only reads the ToolTip.* keys for a JToolTip, so pass them on.
     */
    private static void putPopupBorderStyle(JComponent tipPanel) {
        if (UIManager.get("ToolTip.roundedBorderWidth") instanceof Number width) {
            tipPanel.putClientProperty(FlatClientProperties.POPUP_ROUNDED_BORDER_WIDTH, width.floatValue());
        }
        if (UIManager.get("ToolTip.borderCornerRadius") instanceof Integer radius) {
            tipPanel.putClientProperty(FlatClientProperties.POPUP_BORDER_CORNER_RADIUS, radius);
        }
    }

    private static Color infoForeground() {
        return uiColor("ToolTip.infoForeground", ColorFunctions.mix(
                UIManager.getColor("ToolTip.foreground"), UIManager.getColor("ToolTip.background"), 0.7f));
    }

    private static Color shortcutForeground() {
        return uiColor("ToolTip.shortcutForeground", ColorFunctions.mix(
                UIManager.getColor("ToolTip.foreground"), UIManager.getColor("ToolTip.background"), 0.55f));
    }

    private static Font deriveHeaderFont(Font font) {
        return font.deriveFont(font.getSize2D() + uiInt("HelpTooltip.fontSizeDelta", 0));
    }

    private static Font deriveDescriptionFont(Font font, boolean hasTitle) {
        return hasTitle ? font.deriveFont(font.getSize2D() + uiInt("HelpTooltip.descriptionSizeDelta", 0)) : deriveHeaderFont(font);
    }

    private static String div(boolean wrap) {
        return wrap ? "<div width=\"" + maxWidth() + "\">" : "<div>";
    }

    private static int maxWidth() {
        return UIScale.scale(uiInt("HelpTooltip.maxWidth", 250));
    }

    private static int xOffset() {
        return UIScale.scale(uiInt("HelpTooltip.xOffset", 0));
    }

    private static int yOffset() {
        return UIScale.scale(uiInt("HelpTooltip.yOffset", 0));
    }

    private static int uiInt(String key, int defaultValue) {
        return UIManager.get(key) instanceof Integer i ? i : defaultValue;
    }

    private static Color uiColor(String key, Color defaultValue) {
        Color color = UIManager.getColor(key);
        return color != null ? color : defaultValue;
    }

    private static Insets uiInsets(String key, Insets defaultValue) {
        Insets insets = UIManager.getInsets(key);
        return insets != null ? insets : defaultValue;
    }

    /**
     * Usable screen area (without the taskbar) of the screen showing the component.
     */
    private static Rectangle screenBounds(Component c) {
        GraphicsConfiguration gc = c.getGraphicsConfiguration();
        Rectangle bounds = gc.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        return new Rectangle(bounds.x + insets.left, bounds.y + insets.top,
                bounds.width - insets.left - insets.right, bounds.height - insets.top - insets.bottom);
    }

    private static void moveToFit(Rectangle r, Rectangle bounds) {
        r.x = Math.max(bounds.x, Math.min(r.x, bounds.x + bounds.width - r.width));
        r.y = Math.max(bounds.y, Math.min(r.y, bounds.y + bounds.height - r.height));
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
