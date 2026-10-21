package com.swingcraft4j.login;

import com.formdev.flatlaf.FlatClientProperties;
import com.swingcraft4j.login.background.Palette;
import net.miginfocom.swing.MigLayout;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

/**
 * The form shown once signed in, on a still background: the account, the session and a sign out button.
 */
public class HomePanel extends JPanel {

    private final Palette palette;

    public HomePanel(String username, boolean remembered, Palette palette, Runnable onSignOut) {
        this.palette = palette;
        Color background = palette.dark ? new Color(0x1C1C1E) : new Color(0xF5F5F7);
        setBackground(background);
        setLayout(new MigLayout("wrap,fill,insets 0", "[fill]", "[][][grow,fill]"));
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        JPanel header = new JPanel(new MigLayout("fillx,insets 0 18 0 0", "[][grow][][]", "[center]"));
        header.setOpaque(false);
        JLabel lbName = new JLabel(username);
        lbName.putClientProperty(FlatClientProperties.STYLE, "font:bold +1; foreground:" + Palette.hex(palette.text));
        JLabel lbSince = new JLabel("Signed in at " + time);
        lbSince.putClientProperty(FlatClientProperties.STYLE, "font:-1; foreground:" + Palette.hex(palette.secondaryText));
        JButton btnSignOut = new JButton("Sign out");
        btnSignOut.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:4,14,4,14");
        btnSignOut.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSignOut.addActionListener(e -> onSignOut.run());
        // keeps the room of the window buttons, as the content runs under the title bar
        JPanel buttonsRoom = new JPanel();
        buttonsRoom.setOpaque(false);
        buttonsRoom.putClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT_BUTTONS_PLACEHOLDER, "win");

        header.add(new Avatar(username), "w 38!, h 38!, gapy 10 10");
        header.add(lbName, "split 2, flowy, gapy 0");
        header.add(lbSince, "gapy 0");
        header.add(btnSignOut, "gapright 12");
        header.add(buttonsRoom, "aligny top");
        add(header);
        add(new JSeparator());

        JPanel body = new JPanel(new MigLayout("wrap 3,insets 40 48 40 48", "[fill,sg card,grow]", ""));
        body.setOpaque(false);
        JLabel lbHello = new JLabel("Welcome, " + username);
        lbHello.putClientProperty(FlatClientProperties.STYLE, "font:bold +16; foreground:" + Palette.hex(palette.text));
        JLabel lbInfo = new JLabel("You are signed in. The 3D background is paused while you are here, so it takes no CPU.");
        lbInfo.putClientProperty(FlatClientProperties.STYLE, "foreground:" + Palette.hex(palette.secondaryText));
        body.add(lbHello, "span 3");
        body.add(lbInfo, "span 3, gapbottom 24");
        body.add(new InfoCard("Account", username, remembered ? "Remembered on this computer" : "Not remembered"));
        body.add(new InfoCard("Session", "Since " + time, "Sign out to end it"));
        body.add(new InfoCard("3D background", "Paused", "Starts again when you sign out"));
        add(body);
    }

    /**
     * A round picture in the accent with the first letter of the name.
     */
    private class Avatar extends JComponent {

        private final String letter;

        Avatar(String username) {
            letter = username.substring(0, 1).toUpperCase();
            setPreferredSize(new Dimension(38, 38));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight());
                g2.setPaint(new GradientPaint(0, 0, Palette.mix(palette.accent, Color.WHITE, 0.3), 0, size, palette.accent));
                g2.fill(new Ellipse2D.Double(0, 0, size, size));
                g2.setColor(palette.accentForeground);
                g2.setFont(getFont().deriveFont(Font.BOLD, size * 0.42f));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(letter, (size - fm.stringWidth(letter)) / 2f, (size - fm.getHeight()) / 2f + fm.getAscent());
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * A rounded card with a small title, a value and a line of detail.
     */
    private class InfoCard extends JPanel {

        InfoCard(String title, String value, String detail) {
            super(new MigLayout("wrap,insets 18 20 18 20", "[fill]"));
            setOpaque(false);
            JLabel lbTitle = new JLabel(title);
            lbTitle.putClientProperty(FlatClientProperties.STYLE, "font:-1; foreground:" + Palette.hex(palette.secondaryText));
            JLabel lbValue = new JLabel(value);
            lbValue.putClientProperty(FlatClientProperties.STYLE, "font:bold +6; foreground:" + Palette.hex(palette.text));
            JLabel lbDetail = new JLabel(detail);
            lbDetail.putClientProperty(FlatClientProperties.STYLE, "foreground:" + Palette.hex(palette.secondaryText));
            add(lbTitle);
            add(lbValue);
            add(lbDetail);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(palette.dark ? new Color(0x2A2A2D) : Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, w, h, 16, 16));
                g2.setColor(palette.fieldBorder);
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, 16, 16));
            } finally {
                g2.dispose();
            }
        }
    }
}
