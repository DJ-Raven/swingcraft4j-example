package com.swingcraft4j.treehoveraction;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Drag-and-drop reorder/reparent for {@link HoverActionTree} nodes within the same tree.
 */
class NodesTransferHandler extends TransferHandler {

    private final HoverActionTree tree;
    private List<DefaultMutableTreeNode> draggedNodes;

    NodesTransferHandler(HoverActionTree tree) {
        this.tree = tree;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    /**
     * Snapshots the current selection into a {@link TreeNodesTransferable}, pruning redundant descendants.
     */
    @Override
    protected Transferable createTransferable(JComponent c) {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null || paths.length == 0) {
            return null;
        }

        List<DefaultMutableTreeNode> selected = new ArrayList<>();
        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node.getParent() != null) {
                // never drag the (invisible) root
                selected.add(node);
            }
        }
        if (selected.isEmpty()) {
            draggedNodes = null;
            return null;
        }

        // drop nodes whose ancestor is also selected - moving the ancestor already carries them along
        List<DefaultMutableTreeNode> filtered = new ArrayList<>();
        for (DefaultMutableTreeNode node : selected) {
            boolean descendantOfSelected = false;
            for (DefaultMutableTreeNode other : selected) {
                // isNodeAncestor(x) asks "is x an ancestor of me", so this checks "is other an
                // ancestor of node" - i.e. is node the redundant descendant, not other
                if (other != node && node.isNodeAncestor(other)) {
                    descendantOfSelected = true;
                    break;
                }
            }
            if (!descendantOfSelected) {
                filtered.add(node);
            }
        }
        filtered.sort(NodesTransferHandler::comparePosition);

        draggedNodes = filtered;
        return new TreeNodesTransferable(filtered);
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        draggedNodes = null;
    }

    /**
     * Validates a drop location: structural rules, then {@code TreeDropListener.canDrop}.
     */
    @Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDrop() || !support.isDataFlavorSupported(TreeNodesTransferable.NODES_FLAVOR)) {
            return false;
        }
        if (draggedNodes == null || draggedNodes.isEmpty()) {
            return false;
        }

        DropTarget dropTarget = resolveDropTarget(support);
        if (dropTarget == null) {
            return false;
        }

        for (DefaultMutableTreeNode node : draggedNodes) {
            // reject dropping a node onto itself or into one of its own descendants;
            // isNodeAncestor(x) asks "is x an ancestor of me", so this checks "is node an ancestor of target"
            if (node == dropTarget.parent() || dropTarget.parent().isNodeAncestor(node)) {
                return false;
            }
        }

        if (!tree.fireCanDrop(draggedNodes, dropTarget.parent(), dropTarget.index())) {
            return false;
        }

        support.setShowDropLocation(true);
        return true;
    }

    /**
     * Performs the actual move: removes the dragged nodes and re-inserts them at the drop location.
     */
    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }

        List<DefaultMutableTreeNode> nodes;
        try {
            //noinspection unchecked
            nodes = (List<DefaultMutableTreeNode>) support.getTransferable().getTransferData(TreeNodesTransferable.NODES_FLAVOR);
        } catch (UnsupportedFlavorException | IOException e) {
            return false;
        }

        DropTarget dropTarget = resolveDropTarget(support);
        if (dropTarget == null) {
            return false;
        }
        DefaultMutableTreeNode newParent = dropTarget.parent();
        int index = dropTarget.index();

        if (isNoOpMove(nodes, newParent, index)) {
            // dropped back where it already was - nothing changed, so don't fire the callback
            return true;
        }

        if (!tree.fireNodesDropped(nodes, newParent, index)) {
            return false;
        }

        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        int insertIndex = index;
        for (DefaultMutableTreeNode node : nodes) {
            if (node.getParent() == newParent && newParent.getIndex(node) < insertIndex) {
                insertIndex--;
            }
            model.removeNodeFromParent(node);
        }

        List<TreePath> newSelection = new ArrayList<>();
        for (DefaultMutableTreeNode node : nodes) {
            model.insertNodeInto(node, newParent, insertIndex++);
            newSelection.add(new TreePath(node.getPath()));
        }

        tree.expandPath(new TreePath(newParent.getPath()));
        tree.setSelectionPaths(newSelection.toArray(new TreePath[0]));

        return true;
    }

    /**
     * Translates a raw {@code JTree.DropLocation} into a concrete (parent, child-index) target.
     */
    private DropTarget resolveDropTarget(TransferSupport support) {
        JTree.DropLocation location = (JTree.DropLocation) support.getDropLocation();
        TreePath path = location.getPath();
        if (path == null) {
            return null;
        }
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) path.getLastPathComponent();
        int childIndex = location.getChildIndex();
        int index = childIndex == -1 ? parent.getChildCount() : childIndex;
        return new DropTarget(parent, index);
    }

    /**
     * True if dropping here would leave the child order unchanged (no-op move).
     */
    private boolean isNoOpMove(List<DefaultMutableTreeNode> nodes, DefaultMutableTreeNode newParent, int index) {
        for (DefaultMutableTreeNode node : nodes) {
            if (node.getParent() != newParent) {
                return false;
            }
        }

        List<DefaultMutableTreeNode> current = new ArrayList<>();
        for (int i = 0; i < newParent.getChildCount(); i++) {
            current.add((DefaultMutableTreeNode) newParent.getChildAt(i));
        }

        int insertAt = index;
        for (DefaultMutableTreeNode node : nodes) {
            if (current.indexOf(node) < index) {
                insertAt--;
            }
        }

        List<DefaultMutableTreeNode> simulated = new ArrayList<>(current);
        simulated.removeAll(nodes);
        simulated.addAll(insertAt, nodes);

        return simulated.equals(current);
    }

    private record DropTarget(DefaultMutableTreeNode parent, int index) {
    }

    /**
     * Orders two nodes as they currently appear in the tree (document order), for a stable drag order.
     */
    private static int comparePosition(DefaultMutableTreeNode a, DefaultMutableTreeNode b) {
        TreeNode[] pathA = a.getPath();
        TreeNode[] pathB = b.getPath();
        int i = 0;
        while (i < pathA.length && i < pathB.length && pathA[i] == pathB[i]) {
            i++;
        }
        if (i == pathA.length || i == pathB.length) {
            return Integer.compare(pathA.length, pathB.length);
        }
        TreeNode commonParent = pathA[i - 1];
        return Integer.compare(commonParent.getIndex(pathA[i]), commonParent.getIndex(pathB[i]));
    }

    /**
     * Carries the dragged nodes themselves (in-process drag and drop only, no serialization).
     */
    private static class TreeNodesTransferable implements Transferable {

        static final DataFlavor NODES_FLAVOR =
                new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=java.util.List", "Tree nodes");

        private final List<DefaultMutableTreeNode> nodes;

        TreeNodesTransferable(List<DefaultMutableTreeNode> nodes) {
            this.nodes = nodes;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{NODES_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return NODES_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return nodes;
        }
    }
}
