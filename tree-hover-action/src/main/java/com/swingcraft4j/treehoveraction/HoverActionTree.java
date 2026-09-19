package com.swingcraft4j.treehoveraction;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.ui.FlatTreeUI;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.ColorFunctions;
import com.formdev.flatlaf.util.UIScale;
import com.swingcraft4j.treehoveraction.event.HoverActionListener;
import com.swingcraft4j.treehoveraction.event.TreeDropListener;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * {@link JTree} with per-row hover action icons ({@link HoverAction}), inline rename, and
 * reorder/reparent drag-and-drop.
 */
public class HoverActionTree extends JTree {

    private HoverAction hoverAction;
    private int rolloverIndex = -1;
    private int actionRolloverIndex = -1;
    private int actionPressedIndex = -1;

    public HoverActionTree() {
        init();
    }

    private void init() {
        HoverCellRenderer renderer = new HoverCellRenderer(this);
        HoverCellEditor editor = new HoverCellEditor();
        setLayout(new Layout());
        setCellRenderer(renderer);
        setCellEditor(editor);
        setEditable(true);
        ToolTipManager.sharedInstance().registerComponent(this);
        installRollover();
        installDragAndDrop();
    }

    /**
     * Enables reorder/reparent drag-and-drop via {@link NodesTransferHandler}.
     */
    private void installDragAndDrop() {
        setDragEnabled(true);
        setDropMode(DropMode.ON_OR_INSERT);
        setTransferHandler(new NodesTransferHandler(this));
    }

    /**
     * Tracks which row/action icon the mouse is over, and fires clicks on the hovered icon.
     */
    private void installRollover() {
        MouseAdapter mouseAdapter = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                actionPressedIndex = actionRolloverIndex;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                move(e);
                if (actionRolloverIndex == actionPressedIndex && actionRolloverIndex >= 0 && rolloverIndex >= 0) {
                    fireActionPerformed(e, rolloverIndex, actionRolloverIndex);
                }
                actionPressedIndex = -1;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                repaintRow(rolloverIndex);
                rolloverIndex = -1;
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                move(e);
            }

            private void move(MouseEvent e) {
                boolean paint = false;
                int index = getRowForY(e.getY());
                if (index >= 0) {
                    boolean leaf = ((TreeNode) getPathForRow(index).getLastPathComponent()).isLeaf();
                    int actionIndex = hoverAction.getIndexAtPoint(getBounds(index), e.getPoint(), leaf, index);
                    if (actionRolloverIndex != actionIndex) {
                        actionRolloverIndex = actionIndex;
                        paint = true;
                    }
                }
                if (index != rolloverIndex) {
                    repaintRow(rolloverIndex);
                    rolloverIndex = index;
                    paint = true;
                }
                if (paint) {
                    repaintRow(index);
                }
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    /**
     * Like {@link #getClosestRowForLocation}, but returns -1 for y below/above the last/first row.
     */
    private int getRowForY(int y) {
        int row = getClosestRowForLocation(0, y);
        if (row < 0) {
            return -1;
        }
        Rectangle bounds = getRowBounds(row);
        return (bounds != null && y >= bounds.y && y < bounds.y + bounds.height) ? row : -1;
    }

    private void repaintRow(int row) {
        if (row < 0) {
            return;
        }
        Rectangle bounds = getRowBounds(row);
        if (bounds != null) {
            repaint(0, bounds.y, getWidth(), bounds.height);
        }
    }

    private Rectangle getBounds(int row) {
        Rectangle bounds = getRowBounds(row);
        bounds.width = getWidth();
        return bounds;
    }

    public HoverAction getHoverAction() {
        return hoverAction;
    }

    public int getActionRolloverIndex() {
        return actionRolloverIndex;
    }

    public void setHoverAction(HoverAction hoverAction) {
        this.hoverAction = hoverAction;
    }

    public int getRolloverIndex() {
        return rolloverIndex;
    }

    /**
     * Shows a hover action icon's own tooltip, when the mouse is over one; otherwise the usual tree tooltip.
     */
    @Override
    public String getToolTipText(MouseEvent event) {
        if (hoverAction == null) return super.getToolTipText(event);

        int row = getRowForY(event.getY());
        if (row < 0 || actionRolloverIndex < 0 || actionRolloverIndex > hoverAction.getItems().length - 1) return null;
        return hoverAction.getItems()[actionRolloverIndex].getTooltip();
    }

    /**
     * Re-installs the custom {@link HoverActionTreeUI} whenever the look and feel changes.
     */
    @Override
    public void updateUI() {
        super.updateUI();
        setUI(new HoverActionTreeUI());
    }

    public void addHoverActionListener(HoverActionListener listener) {
        listenerList.add(HoverActionListener.class, listener);
    }

    public void removeHoverActionListener(HoverActionListener listener) {
        listenerList.remove(HoverActionListener.class, listener);
    }

    protected void fireActionPerformed(MouseEvent evt, int row, int index) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == HoverActionListener.class) {
                ((HoverActionListener) listeners[i + 1]).actionPerformed(evt, row, index);
            }
        }
    }

    public void addTreeDropListener(TreeDropListener listener) {
        listenerList.add(TreeDropListener.class, listener);
    }

    public void removeTreeDropListener(TreeDropListener listener) {
        listenerList.remove(TreeDropListener.class, listener);
    }

    /**
     * Fans a completed drop out to every {@link TreeDropListener}; false from any of them cancels it.
     */
    protected boolean fireNodesDropped(List<DefaultMutableTreeNode> nodes, DefaultMutableTreeNode newParent, int index) {
        boolean result = true;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TreeDropListener.class) {
                if (!((TreeDropListener) listeners[i + 1]).nodesDropped(nodes, newParent, index)) {
                    result = false;
                }
            }
        }
        return result;
    }

    /**
     * Polls every {@link TreeDropListener}; false from any of them vetoes the drop location.
     */
    protected boolean fireCanDrop(List<DefaultMutableTreeNode> nodes, DefaultMutableTreeNode newParent, int index) {
        boolean result = true;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TreeDropListener.class) {
                if (!((TreeDropListener) listeners[i + 1]).canDrop(nodes, newParent, index)) {
                    result = false;
                }
            }
        }
        return result;
    }

    /**
     * FlatLaf tree UI extended to paint rollover/drop-target row backgrounds and match the
     * expand-arrow icon patch to them.
     */
    private class HoverActionTreeUI extends FlatTreeUI {

        private HoverActionTreeUI() {
        }

        /**
         * Repaints a row whenever the drop-target location moves onto or off of it.
         */
        @Override
        protected PropertyChangeListener createPropertyChangeListener() {
            PropertyChangeListener superListener = super.createPropertyChangeListener();
            return e -> {
                superListener.propertyChange(e);
                if (e.getSource() == tree && "dropLocation".equals(e.getPropertyName())) {
                    repaintDropContainerRow((JTree.DropLocation) e.getOldValue());
                    repaintDropContainerRow((JTree.DropLocation) e.getNewValue());
                }
            };
        }

        /**
         * Repaints the ancestor row a given drop location would insert into, if any.
         */
        private void repaintDropContainerRow(JTree.DropLocation loc) {
            if (loc == null || loc.getChildIndex() == -1) {
                return;
            }
            Rectangle bounds = tree.getPathBounds(loc.getPath());
            if (bounds != null) {
                tree.repaint(0, bounds.y, tree.getWidth(), bounds.height);
            }
        }

        /**
         * Paints the expand-arrow icon's background patch to match the row's own current background.
         */
        @Override
        protected void paintExpandControl(Graphics g, Rectangle clipBounds, Insets insets, Rectangle bounds, TreePath path, int row, boolean isExpanded, boolean hasBeenExpanded, boolean isLeaf) {
            Icon icon = isExpanded ? getExpandedIcon() : getCollapsedIcon();
            if (icon != null) {
                boolean isSelected = tree.isRowSelected(row);
                boolean isHover = row == rolloverIndex;
                boolean isEditing = editingComponent != null && editingRow == row;
                JTree.DropLocation dropLocation = tree.getDropLocation();
                boolean isDropTarget = dropLocation != null && path.equals(dropLocation.getPath());

                Color color;
                if (isDropTarget) {
                    color = UIManager.getColor("Tree.dropCellBackground");
                } else if (isEditing) {
                    color = (isSelected && isWideSelection()) ? selectionInactiveBackground : tree.getBackground();
                } else if (isSelected) {
                    color = selectionBackground;
                } else {
                    color = isHover ? getRolloverColor() : tree.getBackground();
                }
                int iconWidth = icon.getIconWidth();
                int x = bounds.x - getRightChildIndent() - iconWidth / 2;
                int y = bounds.y;
                g.setColor(color);
                g.fillRect(x, y, iconWidth, bounds.height);
            }

            super.paintExpandControl(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
        }

        /**
         * Paints the rollover and drop-target backgrounds behind a row, before its normal content.
         */
        @Override
        protected void paintRow(Graphics g, Rectangle clipBounds, Insets insets, Rectangle bounds, TreePath path, int row, boolean isExpanded, boolean hasBeenExpanded, boolean isLeaf) {
            boolean isSelected = tree.isRowSelected(row);
            boolean isHover = row == rolloverIndex;
            Graphics2D g2 = (Graphics2D) g.create();
            FlatUIUtils.setRenderingHints(g2);

            // Paint rollover background
            if (!isSelected && isHover) {
                g2.setColor(getRolloverColor());
                float arc = UIScale.scale(selectionArc / 2f);
                FlatUIUtils.paintSelection(g2, 0, bounds.y, tree.getWidth(), bounds.height,
                        UIScale.scale(selectionInsets), arc, arc, arc, arc, 0);
            }

            // Highlight the parent row that will actually receive the dropped node.
            JTree.DropLocation dropLocation = tree.getDropLocation();
            if (dropLocation != null && dropLocation.getChildIndex() != -1 && path.equals(dropLocation.getPath())) {
                g2.setColor(UIManager.getColor("Tree.dropCellBackground"));
                float arc = UIScale.scale(selectionArc / 2f);
                FlatUIUtils.paintSelection(g2, 0, bounds.y, tree.getWidth(), bounds.height,
                        UIScale.scale(selectionInsets), arc, arc, arc, arc, 0);
            }
            g2.dispose();
            super.paintRow(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
        }

        private Color getRolloverColor() {
            if (FlatLaf.isLafDark()) {
                return ColorFunctions.tint(getBackground(), 0.02f);
            }
            return ColorFunctions.shade(getBackground(), 0.04f);
        }
    }

    /**
     * Stretches the active inline editor component to fill the row's width when the tree resizes.
     */
    private static class Layout implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return null;
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return null;
        }

        @Override
        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                int count = parent.getComponentCount();
                for (int i = 0; i < count; i++) {
                    Component com = parent.getComponent(i);
                    if (com instanceof HoverCellEditor.TreeCell e) {
                        Insets insets = parent.getInsets();
                        int width = parent.getWidth() - (insets.left + insets.right) - e.getX();
                        e.setSize(width, e.getHeight());
                    }
                }
            }
        }
    }
}
