package com.swingcraft4j.treehoveraction;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.swingcraft4j.treehoveraction.model.Item;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Standard tree label renderer, extended to also paint the row's hover action icons
 * (see {@link HoverAction}) on top - but only for whichever row the tree reports as hovered.
 */
class HoverCellRenderer extends DefaultTreeCellRenderer {

    private final HoverActionTree treeHover;

    protected boolean leaf;
    protected int row;

    public HoverCellRenderer(HoverActionTree treeHover) {
        this.treeHover = treeHover;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        this.leaf = leaf;
        this.row = row;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        if (node.getUserObject() instanceof Item e) {
            setIcon(e.getType().getIcon());
        }

        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (treeHover.getHoverAction() != null && treeHover.getRolloverIndex() == row) {
            Graphics2D g2 = (Graphics2D) g.create();
            FlatUIUtils.setRenderingHints(g2);
            try {
                treeHover.getHoverAction().paint(this, g2, selected, leaf, row, treeHover.getActionRolloverIndex());
            } finally {
                g2.dispose();
            }
        }
        super.paintComponent(g);
    }
}
