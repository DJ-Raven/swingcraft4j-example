package com.swingcraft4j.treehoveraction;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.swingcraft4j.treehoveraction.model.Item;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellEditor;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.Objects;

/**
 * Inline rename editor for a tree node: a text field plus commit/cancel icon buttons, laid out
 * to match the cell renderer so editing doesn't visibly shift the row.
 */
class HoverCellEditor extends AbstractCellEditor implements TreeCellEditor {

    private final TreeCell treeCell;
    private Item item;

    public HoverCellEditor() {
        treeCell = new TreeCell();
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        if (node.getUserObject() instanceof Item e) {
            item = e;
        } else {
            item = null;
        }

        treeCell.init(value);
        return treeCell;
    }

    @Override
    public boolean isCellEditable(EventObject event) {
        // blocks click-to-rename; editing still starts via startEditingAtPath (event == null) or F2
        return !(event instanceof MouseEvent);
    }

    @Override
    public Object getCellEditorValue() {
        String value = treeCell.getValue();
        if (item == null) {
            return value;
        }
        // getCellEditorValue() is called by BasicTreeUI on both commit and cancel,
        // so it must not mutate the node's existing item - only a commit should apply the change.
        return new Item(item.getType(), value);
    }

    /**
     * The editor's own row component: a text field plus commit/cancel icon buttons.
     */
    public class TreeCell extends JPanel {

        public TreeCell() {
            setLayout(new MigLayout("fillx,gap 3", "[fill,50][grow 0][grow 0]"));
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
            });
            field = new JTextField();
            cmdOk = new JButton(new FlatSVGIcon("com/swingcraft4j/treehoveraction/icons/ok.svg", 0.7f));
            cmdCancel = new JButton(new FlatSVGIcon("com/swingcraft4j/treehoveraction/icons/cancel.svg", 0.7f));

            field.putClientProperty(FlatClientProperties.STYLE, "" +
                    "borderWidth:1;" +
                    "focusWidth:0;");
            cmdOk.putClientProperty(FlatClientProperties.STYLE, "" +
                    "arc:5;" +
                    "borderWidth:0;" +
                    "focusWidth:0;" +
                    "innerFocusWidth:0;" +
                    "[dark]background:shade($Button.background,30%);" +
                    "[light]background:tint($Button.background,30%);");
            cmdCancel.putClientProperty(FlatClientProperties.STYLE, "" +
                    "arc:5;" +
                    "borderWidth:0;" +
                    "focusWidth:0;" +
                    "innerFocusWidth:0;" +
                    "[dark]background:shade($Button.background,30%);" +
                    "[light]background:tint($Button.background,30%);");

            field.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        stopCellEditing();
                    }
                }
            });
            cmdOk.addActionListener(e -> stopCellEditing());
            cmdCancel.addActionListener(e -> cancelCellEditing());

            cmdOk.setFocusable(false);
            cmdCancel.setFocusable(false);

            add(field);
            add(cmdOk);
            add(cmdCancel);
        }

        private void init(Object value) {
            field.setText(Objects.toString(value, ""));
        }

        private String getValue() {
            return field.getText();
        }

        private JTextField field;
        private JButton cmdOk;
        private JButton cmdCancel;
    }
}
