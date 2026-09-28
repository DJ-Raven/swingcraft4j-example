package com.swingcraft4j.helptooltip;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;

/**
 * Shows a component's regular tooltip text as a {@link HelpTooltip}, with an optional shortcut.
 */
public final class HelpTooltipManager extends HelpTooltip {

    /**
     * Client property holding the shortcut to show after the tooltip text, a {@code Supplier<String>} or a {@code String}.
     */
    public static final String SHORTCUT_PROPERTY = "help-tooltip-shortcut";

    private final MouseAdapter ownerListener = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            showTooltip((JComponent) e.getComponent(), e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            showTooltip((JComponent) e.getComponent(), e);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            mouseListener.mouseExited(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            hideTooltip();
        }
    };

    public HelpTooltipManager() {
        createMouseListeners();
    }

    /**
     * Shows the component's tooltip text through this manager instead of Swing's {@link ToolTipManager}.
     */
    public void register(JComponent component) {
        ToolTipManager.sharedInstance().unregisterComponent(component);
        component.addMouseListener(ownerListener);
        component.addMouseMotionListener(ownerListener);
    }

    /**
     * Stops showing the component's tooltip text here and gives it back to Swing's {@link ToolTipManager}.
     */
    public void unregister(JComponent component) {
        component.removeMouseListener(ownerListener);
        component.removeMouseMotionListener(ownerListener);
        ToolTipManager.sharedInstance().registerComponent(component);
        hideTooltip();
    }

    /**
     * Takes the title from {@code getToolTipText(event)} and the shortcut from {@link #SHORTCUT_PROPERTY}, then schedules the tooltip.
     */
    public void showTooltip(JComponent component, MouseEvent event) {
        setTitle(component.getToolTipText(event));
        Object shortcut = component.getClientProperty(SHORTCUT_PROPERTY);
        setShortcut(shortcut instanceof Supplier<?> s ? (String) s.get() : shortcut instanceof String str ? str : null);

        if (event.getID() == MouseEvent.MOUSE_ENTERED) {
            mouseListener.mouseEntered(event);
        } else {
            mouseListener.mouseMoved(event);
        }
    }

    public void hideTooltip() {
        hidePopup(true);
    }
}
