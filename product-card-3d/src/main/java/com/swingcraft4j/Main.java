package com.swingcraft4j;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.util.FontUtils;
import com.swingcraft4j.productcard.ProductCard;
import net.miginfocom.swing.MigLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Demo entry point: a row of 3D product cards on a dark background.
 */
public class Main extends JFrame {

    public Main() {
        super("Product Card 3D");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel panel = new JPanel(new MigLayout("fill,wrap 4", "push[][][][]push", "push[][]push"));
        panel.setBackground(new Color(0x16131f));

        JLabel selected = new JLabel("Click a card to pick a product");
        selected.putClientProperty(FlatClientProperties.STYLE, "font:+2;foreground:#ffffff99");

        ProductCard[] cards = {
                new ProductCard("01", "SNEAKERS", "Blaze Orange", "$39.9", new Color(0x1e88e5), image("shoe-orange.png")),
                new ProductCard("02", "SNEAKERS", "Aqua Knit", "$49.9", new Color(0x6a3cf2), image("shoe-blue.png")),
                new ProductCard("03", "SNEAKERS", "Crimson Run", "$59.9", new Color(0x0f9583), image("shoe-red.png")),
                new ProductCard("04", "SNEAKERS", "Storm Gray", "$69.9", new Color(0xf0a01e), image("shoe-blue-gray.png"))
        };
        for (ProductCard card : cards) {
            card.addActionListener(e -> selected.setText("Selected: " + card.getProductName() + "  " + card.getPrice()));
            panel.add(card);
        }
        panel.add(selected, "span,center");

        setContentPane(panel);
        setSize(1540, 660);
        setLocationRelativeTo(null);
    }

    private static Image image(String name) {
        try {
            return ImageIO.read(ProductCard.class.getResource(name));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void main(String[] args) {
        FlatRobotoFont.install();
        UIManager.put("defaultFont", FontUtils.getCompositeFont(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
        FlatMacDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
