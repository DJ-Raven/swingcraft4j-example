package com.swingcraft4j;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.util.FontUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Demo entry point: an animal search whose results and profile show skeletons while loading.
 */
public class Main extends JFrame {

    public Main() {
        super("Skeleton");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        init();
    }

    private void init() {
        JPanel panel = new JPanel(new MigLayout("fill", "[fill]", "[fill]"));

        ProfileDetail profileDetail = new ProfileDetail();
        SearchPanel searchPanel = new SearchPanel();
        searchPanel.setOnSelect(profileDetail::load);

        JSplitPane splitPane = new JSplitPane();
        splitPane.setLeftComponent(searchPanel);
        splitPane.setRightComponent(profileDetail);

        splitPane.setDividerLocation(270);

        splitPane.putClientProperty(FlatClientProperties.STYLE, "style:plain;");

        panel.add(splitPane);
        add(panel);
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
