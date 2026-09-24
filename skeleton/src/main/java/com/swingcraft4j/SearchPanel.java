package com.swingcraft4j;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.swingcraft4j.animal.AnimalApi;
import com.swingcraft4j.animal.AnimalSummary;
import com.swingcraft4j.skeleton.Skeleton;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Animal search: a text field above a result list that shows skeleton rows while a search runs.
 */
public class SearchPanel extends JPanel {

    private static final int DEBOUNCE_MS = 400;
    private static final int PLACEHOLDER_ROWS = 6;
    // placeholder text only sizes the skeleton bars
    private static final AnimalSummary PLACEHOLDER = new AnimalSummary(0, "Common animal name", "Genus species", null, null);

    private final Skeleton skeleton = new Skeleton();
    private final JTextField search = new JTextField();
    private final JPanel results = new ResultPanel();
    private final JScrollPane scroll = new JScrollPane(results);
    private final Timer debounce = new Timer(DEBOUNCE_MS, e -> search());
    private SwingWorker<List<AnimalSummary>, Void> worker;
    private Consumer<AnimalSummary> onSelect;
    private Item selected;

    public SearchPanel() {
        init();
    }

    /**
     * Called with the clicked result.
     */
    public void setOnSelect(Consumer<AnimalSummary> onSelect) {
        this.onSelect = onSelect;
    }

    private void init() {
        setLayout(new MigLayout("wrap,gap 0,fill", "[fill]", "[][grow,fill]"));
        putClientProperty(FlatClientProperties.STYLE, "border:5,5,5,5,$Component.borderColor,1,10;");

        search.putClientProperty(FlatClientProperties.STYLE, "margin:3,5,3,5;focusWidth:0;borderWidth:1;arc:5;");
        search.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search animals");
        search.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSearchIcon());
        search.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                debounce.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                debounce.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                debounce.restart();
            }
        });
        debounce.setRepeats(false);

        results.setLayout(new MigLayout("wrap,gap 0,fillx,insets 0", "[fill]"));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, "trackArc:999;width:5;thumbInsets:0,0,0,0;");
        // only the results sit in the skeleton layer, so the field stays usable while searching
        add(search, "gapbottom 5");
        add(new JLayer<>(scroll, skeleton));

        showMessage("Type to search animals");
    }

    /**
     * Runs the current query in the background, showing skeleton rows until the results arrive.
     */
    private void search() {
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
        String query = search.getText().trim();
        if (query.isEmpty()) {
            skeleton.setLoading(false);
            showMessage("Type to search animals");
            return;
        }
        showRows(Collections.nCopies(PLACEHOLDER_ROWS, PLACEHOLDER), false);
        skeleton.setLoading(true);

        worker = new SwingWorker<>() {
            @Override
            protected List<AnimalSummary> doInBackground() throws Exception {
                return AnimalApi.search(query);
            }

            @Override
            protected void done() {
                // a newer search (or a cleared field) replaced this one
                if (worker != this) {
                    return;
                }
                worker = null;
                skeleton.setLoading(false);
                try {
                    List<AnimalSummary> animals = get();
                    if (animals.isEmpty()) {
                        showMessage("No animals found");
                    } else {
                        showRows(animals, true);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    showMessage("Search failed: " + cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void showRows(List<AnimalSummary> animals, boolean selectable) {
        results.removeAll();
        selected = null;
        for (AnimalSummary animal : animals) {
            Item item = new Item(animal);
            if (selectable) {
                item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                item.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            select(item);
                        }
                    }
                });
            }
            results.add(item);
        }
        refresh();
    }

    private void showMessage(String message) {
        results.removeAll();
        selected = null;
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;");
        results.add(label, "gaptop 20");
        refresh();
    }

    private void select(Item item) {
        if (item == selected) {
            return;
        }
        if (selected != null) {
            selected.setSelected(false);
        }
        selected = item;
        item.setSelected(true);
        if (onSelect != null) {
            onSelect.accept(item.getAnimal());
        }
    }

    private void refresh() {
        results.revalidate();
        results.repaint();
        scroll.getVerticalScrollBar().setValue(0);
    }

    /**
     * Result column that follows the viewport width, so long names get clipped instead of scrolling sideways.
     */
    private static class ResultPanel extends JPanel implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
