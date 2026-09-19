package com.swingcraft4j.treehoveraction.event;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.EventListener;
import java.util.List;

/**
 * Notified after nodes have been dragged and are about to be dropped into a new location.
 * <p>
 * Returning {@code false} cancels the drop entirely - the tree model is left unchanged.
 */
public interface TreeDropListener extends EventListener {

    /**
     * Polled during drag to pick the drop cursor; must be cheap and side-effect free. Structural
     * checks (self/descendant) already ran - implement only for extra placement rules.
     */
    default boolean canDrop(List<DefaultMutableTreeNode> nodes, DefaultMutableTreeNode newParent, int index) {
        return true;
    }

    boolean nodesDropped(List<DefaultMutableTreeNode> droppedNodes, DefaultMutableTreeNode newParent, int index);
}
