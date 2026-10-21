package com.swingcraft4j;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.formdev.flatlaf.util.FontUtils;
import com.swingcraft4j.login.HomePanel;
import com.swingcraft4j.login.LoginForm;
import com.swingcraft4j.login.Settings;
import com.swingcraft4j.login.StyleSwitcher;
import com.swingcraft4j.login.background.Background3D;
import com.swingcraft4j.login.background.Palette;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Demo entry point: the sign in card on the 3D background, with bars to switch the scene and the theme.
 */
public class Main extends JFrame {

    private final Settings settings;
    private final Background3D background;
    private final LoginForm form;
    private Palette palette;

    public Main(Settings settings) {
        super("Login 3D");
        this.settings = settings;
        palette = Palette.of(settings.accent, settings.dark);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        // the background goes under the title bar, which keeps only its buttons
        getRootPane().putClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT, true);
        getRootPane().putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_TITLE, false);
        getRootPane().putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_ICON, false);

        background = new Background3D(palette, settings.scene);
        // dragging the top of the background moves the window
        background.putClientProperty(FlatClientProperties.COMPONENT_TITLE_BAR_CAPTION, true);
        background.setLayout(new MigLayout("fill,insets 0 0 20 0", "[center]", "[grow,center][]"));
        form = new LoginForm(settings, palette);
        form.setOnSignedIn(this::showHome);
        StyleSwitcher switcher = new StyleSwitcher(background.getSceneNames(), settings.scene, palette,
                this::sceneChanged, this::paletteChanged);
        background.add(form, "wrap");
        background.add(switcher);

        setContentPane(background);
        getRootPane().setDefaultButton(form.getSignInButton());
        setSize(1100, 800);
        setMinimumSize(new Dimension(720, 760));
        setLocationRelativeTo(null);
    }

    /**
     * Swaps the login for the home form; taking the background out of the window stops its animation.
     */
    private void showHome(String username) {
        setContentPane(new HomePanel(username, settings.remember, palette, this::signOut));
        getRootPane().setDefaultButton(null);
        revalidate();
        repaint();
    }

    /**
     * Brings the login back; the background starts animating again once it is in the window.
     */
    private void signOut() {
        setContentPane(background);
        getRootPane().setDefaultButton(form.getSignInButton());
        revalidate();
        repaint();
        form.focusFirstField();
    }

    private void sceneChanged(int index) {
        background.setScene(index);
        settings.scene = index;
        settings.save();
    }

    private void paletteChanged(Palette palette) {
        this.palette = palette;
        boolean appearanceChanged = palette.dark != settings.dark;
        if (appearanceChanged) {
            FlatAnimatedLafChange.showSnapshot();
        }
        setLookAndFeel(palette);
        FlatLaf.updateUI();
        if (appearanceChanged) {
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        }
        background.setPalette(palette);
        form.setPalette(palette);
        settings.accent = palette.accentKind;
        settings.dark = palette.dark;
        settings.save();
    }

    /**
     * Sets the FlatLaf Mac light or dark theme in the accent of the palette.
     */
    private static void setLookAndFeel(Palette palette) {
        FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", Palette.hex(palette.accent)));
        if (palette.dark) {
            FlatMacDarkLaf.setup();
        } else {
            FlatMacLightLaf.setup();
        }
    }

    public static void main(String[] args) {
        Settings settings = Settings.load();
        FlatRobotoFont.install();
        UIManager.put("defaultFont", FontUtils.getCompositeFont(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
        UIManager.put("Component.arc", 10);
        UIManager.put("CheckBox.arc", 5);
        // a light shade on the eye button of the password while it shows the password
        UIManager.put("[style]ToggleButton.revealButton", ""
                + "toolbar.hoverBackground:#80808020;"
                + "toolbar.pressedBackground:#80808038;"
                + "toolbar.selectedBackground:#80808028");
        setLookAndFeel(Palette.of(settings.accent, settings.dark));

        SwingUtilities.invokeLater(() -> {
            Main frame = new Main(settings);
            frame.setVisible(true);
            frame.form.focusFirstField();
        });
    }
}
