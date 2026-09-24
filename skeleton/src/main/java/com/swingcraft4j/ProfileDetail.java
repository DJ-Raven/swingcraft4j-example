package com.swingcraft4j;

import com.formdev.flatlaf.FlatClientProperties;
import com.swingcraft4j.animal.Animal;
import com.swingcraft4j.animal.AnimalApi;
import com.swingcraft4j.animal.AnimalSummary;
import com.swingcraft4j.profile.AvatarIcon;
import com.swingcraft4j.skeleton.Skeleton;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;

/**
 * Animal profile: photo, names, classification and description; shows a skeleton while the details load.
 */
public class ProfileDetail extends JPanel {

    private static final String EMPTY = "empty";
    private static final String DETAIL = "detail";
    private static final int AVATAR = 120;
    // placeholder text only sizes the skeleton bars
    private static final String PLACEHOLDER_GROUP = "Mammal · Species";
    private static final String PLACEHOLDER_STATUS = "Least concern · 12,345 observations";
    private static final String PLACEHOLDER_DESCRIPTION = "The animal's description is loading. It usually runs for a few "
            + "lines of text taken from the Wikipedia summary, covering where the species lives, what it looks like "
            + "and how it behaves in the wild.";

    private final CardLayout cards = new CardLayout();
    private final Skeleton skeleton = new Skeleton();
    private final AvatarIcon avatarIcon = new AvatarIcon(AVATAR);
    private final JLabel avatar = new JLabel();
    private final JLabel name = new JLabel();
    private final JLabel scientific = new JLabel();
    private final JLabel group = new JLabel();
    private final JLabel status = new JLabel();
    private final JTextArea description = new JTextArea();
    private SwingWorker<Animal, Void> worker;

    public ProfileDetail() {
        init();
    }

    private void init() {
        setLayout(cards);
        putClientProperty(FlatClientProperties.STYLE, "border:5,5,5,5,$Component.borderColor,1,10;");

        JLabel empty = new JLabel("Select an animal to see its profile", SwingConstants.CENTER);
        empty.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;");

        avatar.putClientProperty(Skeleton.CIRCLE, true);
        name.putClientProperty(FlatClientProperties.STYLE, "font:bold +8;");
        scientific.putClientProperty(FlatClientProperties.STYLE, "font:italic +2;foreground:$Label.disabledForeground;");
        status.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;");
        description.setEditable(false);
        description.setFocusable(false);
        description.setOpaque(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);

        JPanel content = new JPanel(new MigLayout("wrap,insets 20,gapx 20,fillx", "[][grow,fill]", "[center]"));
        content.add(avatar, "width " + AVATAR + "!,height " + AVATAR + "!");
        content.add(name, "split 4,flowy");
        content.add(scientific);
        content.add(group, "gaptop 10");
        content.add(status);
        content.add(description, "span 2,growx,gaptop 20,wmin 0");

        add(empty, EMPTY);
        add(new JLayer<>(content, skeleton), DETAIL);
    }

    /**
     * Shows the animal's names at once and fetches the rest in the background under the skeleton.
     */
    public void load(AnimalSummary summary) {
        if (worker != null) {
            worker.cancel(true);
        }
        name.setText(summary.commonName());
        scientific.setText(summary.scientificName());
        group.setText(PLACEHOLDER_GROUP);
        status.setText(PLACEHOLDER_STATUS);
        description.setText(PLACEHOLDER_DESCRIPTION);
        avatar.setIcon(null);
        cards.show(this, DETAIL);
        skeleton.setLoading(true);

        worker = new SwingWorker<>() {
            @Override
            protected Animal doInBackground() throws Exception {
                return AnimalApi.fetch(summary);
            }

            @Override
            protected void done() {
                // another animal was picked meanwhile
                if (worker != this) {
                    return;
                }
                worker = null;
                skeleton.setLoading(false);
                try {
                    setAnimal(get());
                } catch (InterruptedException | ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    group.setText(null);
                    status.setText(null);
                    description.setText("Couldn't load details: " + cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void setAnimal(Animal animal) {
        name.setText(animal.commonName());
        scientific.setText(animal.scientificName());
        group.setText(animal.group() + " · " + animal.rank());
        status.setText(String.format("%s · %,d observations", animal.conservation(), animal.observations()));
        description.setText(animal.description());
        avatarIcon.setImage(animal.photo());
        avatar.setIcon(avatarIcon);
        avatar.repaint();
    }
}
