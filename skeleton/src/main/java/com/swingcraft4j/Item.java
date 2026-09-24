package com.swingcraft4j;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import com.swingcraft4j.animal.AnimalSummary;
import com.swingcraft4j.net.Http;
import com.swingcraft4j.profile.AvatarIcon;
import com.swingcraft4j.skeleton.Skeleton;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Search result row: circular thumbnail beside the common and scientific names.
 */
public class Item extends JPanel {

    private static final int THUMB = 40;
    private static final ExecutorService THUMB_LOADER = Executors.newFixedThreadPool(4, r -> {
        Thread thread = new Thread(r, "thumb-loader");
        thread.setDaemon(true);
        return thread;
    });

    private final JLabel avatar = new JLabel();
    private final JLabel name = new JLabel();
    private final JLabel scientific = new JLabel();
    private AnimalSummary animal;
    private boolean hover;
    private boolean selected;

    public Item(AnimalSummary animal) {
        setLayout(new MigLayout("gap 5", "[]10[grow]", "[center]"));
        // transparent so the rounded hover/selection highlight shows through
        setOpaque(false);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
        avatar.putClientProperty(Skeleton.CIRCLE, true);
        name.putClientProperty(FlatClientProperties.STYLE, "font:bold;");
        scientific.putClientProperty(FlatClientProperties.STYLE, "font:italic;foreground:$Label.disabledForeground;");
        add(avatar, "width " + THUMB + "!,height " + THUMB + "!");
        add(name, "split 2,flowy");
        add(scientific);
        setAnimal(animal);
    }

    /**
     * Shows the animal's names and loads its thumbnail in the background; the avatar stays empty until it arrives.
     */
    public void setAnimal(AnimalSummary animal) {
        this.animal = animal;
        name.setText(animal != null ? animal.commonName() : null);
        scientific.setText(animal != null ? animal.scientificName() : null);
        avatar.setIcon(null);
        if (animal == null || animal.thumbUrl() == null) {
            return;
        }
        THUMB_LOADER.execute(() -> {
            try {
                BufferedImage image = Http.image(animal.thumbUrl());
                SwingUtilities.invokeLater(() -> {
                    // ignore a late image if the row was switched to another animal meanwhile
                    if (this.animal == animal) {
                        AvatarIcon icon = new AvatarIcon(THUMB);
                        icon.setImage(image);
                        avatar.setIcon(icon);
                    }
                });
            } catch (Exception ignored) {
                // thumbnail is best-effort
            }
        });
    }

    public AnimalSummary getAnimal() {
        return animal;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!selected && !hover) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fg = getForeground();
            g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), selected ? 30 : 15));
            float arc = UIScale.scale(10f);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));
        } finally {
            g2.dispose();
        }
    }
}
