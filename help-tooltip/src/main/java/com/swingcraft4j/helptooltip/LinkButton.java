package com.swingcraft4j.helptooltip;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.Map;

/**
 * Link-styled button for the tooltip, underlined on hover, with an optional external-link arrow.
 */
class LinkButton extends JButton {

    LinkButton(String text, boolean external, Runnable action) {
        super(text);
        setBorder(BorderFactory.createEmptyBorder());
        setContentAreaFilled(false);
        setFocusable(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setHorizontalTextPosition(SwingConstants.LEFT);
        if (external) {
            FlatSVGIcon icon = new FlatSVGIcon("com/swingcraft4j/helptooltip/icons/external.svg");
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> getForeground()));
            setIcon(icon);
            setIconTextGap(UIScale.scale(2));
        }
        getModel().addChangeListener(e -> setUnderline(getModel().isRollover()));
        addActionListener(e -> action.run());
    }

    private void setUnderline(boolean underline) {
        Font font = getFont();
        boolean current = TextAttribute.UNDERLINE_ON.equals(font.getAttributes().get(TextAttribute.UNDERLINE));
        if (current != underline) {
            setFont(font.deriveFont(Map.of(TextAttribute.UNDERLINE, underline ? TextAttribute.UNDERLINE_ON : -1)));
        }
    }
}
