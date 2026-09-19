package com.swingcraft4j.treehoveraction.event;

import java.awt.event.MouseEvent;
import java.util.EventListener;

/**
 * Notified when one of a row's hover action icons is clicked (press and release on the same icon).
 */
public interface HoverActionListener extends EventListener {

    /**
     * @param row   the tree row the action icon belongs to
     * @param index the clicked icon's index within that row's action items
     */
    void actionPerformed(MouseEvent evt, int row, int index);
}
