package com.swingcraft4j;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.util.FontUtils;
import com.swingcraft4j.badgenotification.Badge;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Demo entry point: buttons and a text field wrapped with {@link Badge} layers.
 */
public class Main extends JFrame {

    private int count;

    public Main() {
        super("Badge Notification");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        init();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    private void init() {

        JButton bell = new JButton(new FlatSVGIcon("com/swingcraft4j/badgenotification/icons/bell.svg", 1.3f));
        JButton mail = new JButton(new FlatSVGIcon("com/swingcraft4j/badgenotification/icons/mail.svg", 1.3f));
        JButton messages = new JButton("Messages");
        JTextField search = new JTextField(15);
        JComboBox<String> folder = new JComboBox<>(new String[]{"Inbox", "Sent", "Drafts"});

        String iconStyle = "buttonType:toolBarButton;margin:8,8,8,8;";
        bell.putClientProperty(FlatClientProperties.STYLE, iconStyle);
        mail.putClientProperty(FlatClientProperties.STYLE, iconStyle);
        search.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search");

        // badges wrap each component's own border
        Badge bellBadge = new Badge();
        Badge mailBadge = new Badge();
        Badge messagesBadge = new Badge();
        Badge searchBadge = new Badge();

        // the combo box badge shows the selected folder's unread count
        Badge folderBadge = new Badge();
        int[] unread = {0, 0, 2};
        folder.addActionListener(e -> folderBadge.setCount(unread[folder.getSelectedIndex()]));


        // icon buttons have no visible outline, so nudge their badges in towards the icon
        bellBadge.setStyle("background:$Component.accentColor;height:14;font:bold -3;offsetX:3;offsetY:3;");
        mailBadge.setStyle("background:$Actions.Blue;offsetX:6;offsetY:6;");
        searchBadge.setStyle("background:$Actions.Green;");
        folderBadge.setStyle("background:$Component.accentColor;height:14;font:bold -3;offsetX:3;offsetY:3;");

        JButton notify = new JButton("New notification");
        JButton dot = new JButton("Toggle dot");
        JButton clear = new JButton("Clear");

        notify.addActionListener(e -> {
            count++;
            bellBadge.alert(count);
            messagesBadge.alert("News " + count);
            mailBadge.alert();
            unread[0] = count;
            if (folder.getSelectedIndex() == 0) {
                folderBadge.alert(count);
            }
        });
        dot.addActionListener(e -> searchBadge.setBadgeVisible(!searchBadge.isBadgeVisible()));
        clear.addActionListener(e -> {
            count = 0;
            bellBadge.setCount(0);
            messagesBadge.setCount(0);
            mailBadge.setBadgeVisible(false);
            searchBadge.setBadgeVisible(false);
            unread[0] = 0;
            folderBadge.setCount(unread[folder.getSelectedIndex()]);
        });

        // add the wrapping layers, not the components themselves
        JPanel buttons = card("gap 12", "", "[center]");
        buttons.add(bellBadge.wrap(bell));
        buttons.add(mailBadge.wrap(mail));
        buttons.add(messagesBadge.wrap(messages), "gapleft 8");

        JPanel inputs = card("wrap 2,gap 12 10", "[right][fill,grow]", "");
        inputs.add(new JLabel("Search"));
        inputs.add(searchBadge.wrap(search));
        inputs.add(new JLabel("Folder"));
        inputs.add(folderBadge.wrap(folder), "growx 0");

        // move every badge to the chosen corner
        JComboBox<Badge.Position> position = new JComboBox<>(Badge.Position.values());
        position.addActionListener(e -> {
            Badge.Position p = (Badge.Position) position.getSelectedItem();
            for (Badge badge : new Badge[]{bellBadge, mailBadge, messagesBadge, searchBadge, folderBadge}) {
                badge.setPosition(p);
            }
        });

        JPanel controls = card("gap 8", "[][][]push[][]", "");
        controls.add(notify);
        controls.add(dot);
        controls.add(clear);
        controls.add(new JLabel("Position"), "gapleft 24");
        controls.add(position);

        JPanel panel = new JPanel(new MigLayout("wrap,insets 24,gap 16", "[fill,grow]"));
        panel.add(section("Buttons", buttons));
        panel.add(section("Inputs", inputs));
        panel.add(section("Controls", controls));
        add(panel);
    }

    /**
     * Rounded panel holding one group of examples.
     */
    private static JPanel card(String layout, String columns, String rows) {
        // the insets leave room for badges that overhang the card's outer components
        JPanel card = new JPanel(new MigLayout("insets 20," + layout, columns, rows));
        card.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:16;" +
                "[dark]background:lighten(@background,4%);" +
                "[light]background:darken(@background,3%);");
        return card;
    }

    /**
     * Small caption above a card.
     */
    private static JPanel section(String title, JPanel card) {
        JLabel caption = new JLabel(title);
        caption.putClientProperty(FlatClientProperties.STYLE, "font:bold -1;foreground:$Label.disabledForeground;");

        JPanel section = new JPanel(new MigLayout("wrap,insets 0,gap 0 6", "[fill,grow]"));
        section.add(caption, "gapleft 4");
        section.add(card);
        return section;
    }

    /**
     * Installs the demo's font/theme, then shows the window on the Swing event thread.
     */
    public static void main(String[] args) {
        FlatRobotoFont.install();
        FlatLaf.registerCustomDefaultsSource("com.swingcraft4j.badgenotification.themes");
        UIManager.put("defaultFont", FontUtils.getCompositeFont(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
        FlatMacDarkLaf.setup();

        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
