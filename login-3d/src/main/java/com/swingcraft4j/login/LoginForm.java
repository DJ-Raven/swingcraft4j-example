package com.swingcraft4j.login;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import com.swingcraft4j.login.background.Palette;
import net.miginfocom.swing.MigLayout;

/**
 * A macOS style sign in card, translucent over the 3D background.
 */
public class LoginForm extends JPanel {

    private static final int ARC = 26;

    private final Settings settings;
    private final LockBadge badge = new LockBadge();
    private final JLabel lbTitle = new JLabel("Welcome back");
    private final JLabel lbSubtitle = new JLabel("Sign in to continue to your account");
    private final JTextField txtUsername = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JLabel lbMessage = new JLabel();
    private final JCheckBox chkRemember = new JCheckBox("Remember me");
    private final JButton btnForgot = linkButton("Forgot password?");
    private final JButton btnSignIn = new JButton("Sign in");
    private final JLabel lbNoAccount = new JLabel("Don't have an account?");
    private final JButton btnSignUp = linkButton("Sign up");

    private final GlyphIcon userIcon = new GlyphIcon(GlyphIcon.Kind.USER, 16, Color.GRAY);
    private final GlyphIcon lockIcon = new GlyphIcon(GlyphIcon.Kind.LOCK, 16, Color.GRAY);
    private final GlyphIcon spinner = new GlyphIcon(GlyphIcon.Kind.SPINNER, 16, Color.WHITE);
    private final GlyphIcon checkIcon = new GlyphIcon(GlyphIcon.Kind.CHECK, 16, Color.WHITE);
    private final Timer spinTimer = new Timer(16, e -> {
        spinner.step();
        btnSignIn.repaint();
    });
    private Timer shakeTimer;

    private Palette palette;
    private boolean busy;
    private Consumer<String> onSignedIn;

    public LoginForm(Settings settings, Palette palette) {
        this.settings = settings;
        setOpaque(false);
        setLayout(new MigLayout("wrap, fillx, insets 30 42 30 42", "[fill, 300!]"));

        txtUsername.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Username");
        txtUsername.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, userIcon);
        txtPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Password");
        txtPassword.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, lockIcon);
        clearMessageOnTyping(txtUsername);
        clearMessageOnTyping(txtPassword);
        // Enter in the username goes on to the password, Enter in the password signs in
        txtUsername.addActionListener(e -> {
            if (txtPassword.getPassword().length == 0) {
                txtPassword.requestFocusInWindow();
            } else {
                signIn();
            }
        });
        txtPassword.addActionListener(e -> signIn());

        chkRemember.setOpaque(false);
        chkRemember.setSelected(settings.remember);
        txtUsername.setText(settings.username);

        btnSignIn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSignIn.setIconTextGap(8);
        btnSignIn.addActionListener(e -> signIn());

        add(badge, "align center, growx 0, w 76!, h 76!");
        add(lbTitle, "align center, growx 0, gapy 8 0");
        add(lbSubtitle, "align center, growx 0, gapy 2 22");
        add(txtUsername, "h 40!");
        add(txtPassword, "h 40!, gapy 10 0");
        // keeps its place when empty, so a message does not move the card
        add(lbMessage, "h 20!, gapy 4 0");
        add(chkRemember, "split 2, growx 0");
        add(btnForgot, "growx 0, gapbefore push");
        add(btnSignIn, "h 42!, gapy 16 14");
        add(lbNoAccount, "split 2, growx 0, gapbefore push");
        add(btnSignUp, "growx 0, gapafter push");

        setPalette(palette);
    }

    /**
     * Gives the card, the fields and the buttons the colors of the palette.
     */
    public final void setPalette(Palette palette) {
        this.palette = palette;
        String accent = Palette.hex(palette.accent);
        String hover = Palette.hex(Palette.mix(palette.accent, Color.WHITE, 0.15));
        String pressed = Palette.hex(Palette.mix(palette.accent, Color.BLACK, 0.15));
        String foreground = Palette.hex(palette.accentForeground);
        String text = Palette.hex(palette.text);
        String secondary = Palette.hex(palette.secondaryText);

        lbTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +14; foreground:" + text);
        lbSubtitle.putClientProperty(FlatClientProperties.STYLE, "foreground:" + secondary);
        lbNoAccount.putClientProperty(FlatClientProperties.STYLE, "foreground:" + secondary);
        lbMessage.putClientProperty(FlatClientProperties.STYLE, "font:-1; foreground:" + Palette.hex(palette.error));

        String fieldStyle = ""
                + "arc:10;"
                + "margin:4,8,4,10;"
                + "background:" + Palette.hex(palette.fieldBackground) + ";"
                + "foreground:" + text + ";"
                + "placeholderForeground:" + secondary + ";"
                + "borderColor:" + Palette.hex(palette.fieldBorder) + ";"
                + "focusedBorderColor:" + accent + ";"
                + "focusColor:" + Palette.hex(Palette.alpha(palette.accent, 90)) + ";"
                + "focusWidth:3;";
        txtUsername.putClientProperty(FlatClientProperties.STYLE, fieldStyle);
        txtPassword.putClientProperty(FlatClientProperties.STYLE, fieldStyle + "showRevealButton:true;");
        userIcon.setColor(palette.secondaryText);
        lockIcon.setColor(palette.secondaryText);

        chkRemember.putClientProperty(FlatClientProperties.STYLE, ""
                + "foreground:" + text + ";"
                + "icon.selectedBackground:" + accent + ";"
                + "icon.selectedBorderColor:" + accent + ";"
                + "icon.checkmarkColor:" + foreground + ";"
                + "icon.focusedBorderColor:" + accent);

        btnSignIn.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:bold +1;"
                + "foreground:" + foreground + ";"
                + "background:" + accent + ";"
                + "hoverBackground:" + hover + ";"
                + "pressedBackground:" + pressed + ";"
                // the same colors when it is the default button of the window
                + "default.foreground:" + foreground + ";"
                + "default.background:" + accent + ";"
                + "default.hoverBackground:" + hover + ";"
                + "default.pressedBackground:" + pressed + ";"
                + "arc:10;"
                + "borderWidth:0;"
                + "focusWidth:0;"
                + "innerFocusWidth:0");
        spinner.setColor(palette.accentForeground);
        checkIcon.setColor(palette.accentForeground);

        String link = Palette.hex(palette.dark
                ? Palette.mix(palette.accent, Color.WHITE, 0.25)
                : Palette.mix(palette.accent, Color.BLACK, 0.15));
        btnForgot.putClientProperty(FlatClientProperties.STYLE, linkStyle(link));
        btnSignUp.putClientProperty(FlatClientProperties.STYLE, linkStyle(link));
        repaint();
    }

    private static JButton linkButton(String text) {
        JButton button = new JButton(text);
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static String linkStyle(String color) {
        return "foreground:" + color + ";"
                + "hoverBackground:null;"
                + "pressedBackground:null;"
                + "margin:0,2,0,2";
    }

    /**
     * The sign in button of the window.
     */
    public JButton getSignInButton() {
        return btnSignIn;
    }

    /**
     * Puts the focus in the password when the username is remembered, else in the username.
     */
    public void focusFirstField() {
        (txtUsername.getText().isEmpty() ? txtUsername : txtPassword).requestFocusInWindow();
    }

    private void signIn() {
        if (busy) {
            return;
        }
        String username = txtUsername.getText().trim();
        char[] password = txtPassword.getPassword();
        JComponent missing = null;
        if (password.length == 0) {
            missing = txtPassword;
        }
        if (username.isEmpty()) {
            missing = txtUsername;
        }
        if (missing != null) {
            Arrays.fill(password, '\0');
            missing.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
            showMessage(missing == txtUsername ? "Enter your username." : "Enter your password.");
            missing.requestFocusInWindow();
            shake();
            return;
        }

        setBusy(true);
        // a pause like a call to the server; do the real check off the event thread
        Timer wait = new Timer(1200, e -> {
            boolean ok = authenticate(username, password);
            Arrays.fill(password, '\0');
            setBusy(false);
            if (ok) {
                signedIn(username);
            } else {
                txtPassword.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
                showMessage("Incorrect username or password.");
                txtPassword.selectAll();
                txtPassword.requestFocusInWindow();
                shake();
            }
        });
        wait.setRepeats(false);
        wait.start();
    }

    /**
     * Checks the username and the password.
     */
    protected boolean authenticate(String username, char[] password) {
        return password.length >= 4;
    }

    /**
     * Sets what runs with the username once the user is signed in, to open the application.
     */
    public void setOnSignedIn(Consumer<String> onSignedIn) {
        this.onSignedIn = onSignedIn;
    }

    /**
     * Greets the user on the button for a moment, then tells {@link #setOnSignedIn}.
     */
    protected void signedIn(String username) {
        settings.remember = chkRemember.isSelected();
        settings.username = username;
        settings.save();

        // stays busy, so a second click does not sign in twice
        busy = true;
        btnSignIn.setIcon(checkIcon);
        btnSignIn.setText("Welcome, " + username);
        Timer done = new Timer(900, e -> {
            reset();
            if (onSignedIn != null) {
                onSignedIn.accept(username);
            }
        });
        done.setRepeats(false);
        done.start();
    }

    /**
     * Gets the form ready for the next sign in: no password or message, the username only when remembered.
     */
    public void reset() {
        busy = false;
        btnSignIn.setIcon(null);
        btnSignIn.setText("Sign in");
        txtUsername.setText(settings.remember ? settings.username : "");
        txtPassword.setText("");
        txtUsername.putClientProperty(FlatClientProperties.OUTLINE, null);
        txtPassword.putClientProperty(FlatClientProperties.OUTLINE, null);
        lbMessage.setText("");
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        txtUsername.setEnabled(!busy);
        txtPassword.setEnabled(!busy);
        chkRemember.setEnabled(!busy);
        btnSignIn.setIcon(busy ? spinner : null);
        btnSignIn.setText(busy ? "Signing in..." : "Sign in");
        if (busy) {
            spinTimer.start();
        } else {
            spinTimer.stop();
        }
    }

    private void showMessage(String message) {
        lbMessage.setText(message);
    }

    /**
     * Shakes the card from side to side, as macOS does on a wrong password.
     */
    private void shake() {
        if (shakeTimer != null && shakeTimer.isRunning()) {
            return;
        }
        Point base = getLocation();
        long start = System.currentTimeMillis();
        shakeTimer = new Timer(15, null);
        shakeTimer.addActionListener(e -> {
            double t = (System.currentTimeMillis() - start) / 480.0;
            if (t >= 1) {
                setLocation(base);
                shakeTimer.stop();
                return;
            }
            setLocation(base.x + (int) Math.round(Math.sin(t * Math.PI * 6) * 14 * (1 - t)), base.y);
        });
        shakeTimer.start();
    }

    private void clearMessageOnTyping(JTextComponent field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                clear();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                clear();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }

            private void clear() {
                field.putClientProperty(FlatClientProperties.OUTLINE, null);
                lbMessage.setText("");
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(palette.card);
            g2.fill(new RoundRectangle2D.Double(0, 0, w, h, ARC, ARC));
            g2.setColor(palette.cardBorder);
            g2.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, ARC, ARC));
        } finally {
            g2.dispose();
        }
    }

    /**
     * A round badge in the accent with a lock, like the picture of the macOS login, with a soft light behind it.
     */
    private class LockBadge extends JComponent {

        private final GlyphIcon lock = new GlyphIcon(GlyphIcon.Kind.LOCK, 28, Color.WHITE);

        LockBadge() {
            setPreferredSize(new Dimension(76, 76));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float cx = getWidth() / 2f, cy = getHeight() / 2f;
                g2.setPaint(new RadialGradientPaint(cx, cy, 38, new float[]{0.6f, 1f},
                        new Color[]{Palette.alpha(palette.accent, 90), Palette.alpha(palette.accent, 0)}));
                g2.fill(new Ellipse2D.Float(cx - 38, cy - 38, 76, 76));
                g2.setPaint(new GradientPaint(cx, cy - 29, Palette.mix(palette.accent, Color.WHITE, 0.3),
                        cx, cy + 29, palette.accent));
                g2.fill(new Ellipse2D.Float(cx - 29, cy - 29, 58, 58));
                g2.setColor(new Color(255, 255, 255, 50));
                g2.draw(new Ellipse2D.Float(cx - 28.5f, cy - 28.5f, 57, 57));
                lock.setColor(palette.accentForeground);
                lock.paintIcon(this, g2, Math.round(cx - 14), Math.round(cy - 15));
            } finally {
                g2.dispose();
            }
        }
    }
}
