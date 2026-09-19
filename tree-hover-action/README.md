# Tree Hover Action

A `JTree` (`HoverActionTree`) that adds three things on top of FlatLaf's tree UI:

- **Hover action icons** — small trailing icon buttons (add / edit / more, or whatever you define) that appear on whichever row the mouse is over, right-aligned in the row.
- **Inline rename editing** — a text field with commit/cancel icon buttons, laid out to match the renderer so editing doesn't shift the row.
- **Reorder / reparent drag-and-drop** — drag one or more selected nodes onto another node (becomes its last child) or between two nodes (insert at that index), within the same tree.

The model, node content, and drop rules are entirely up to the caller — `HoverActionTree` only wires the mechanics together.

## Requirements

- Java 25.
- [FlatLaf](https://github.com/JFormDesigner/FlatLaf/) + [MigLayout](https://github.com/mikaelgrev/miglayout/).

## Usage

### 1. Create the tree and give it a model

`HoverActionTree` is a drop-in `JTree` subclass — build any `TreeModel` as usual:

```java
HoverActionTree tree = new HoverActionTree();
tree.setModel(new DefaultTreeModel(root));
```

### 2. Add hover action icons

Define the icons with `HoverAction`, then attach it:

```java
HoverAction hoverAction = new HoverAction(new HoverAction.Item[]{
        new HoverAction.Item(addIcon, "Add"),
        new HoverAction.Item(editIcon, "Edit"),
        new HoverAction.Item(moreIcon, "More action")
});
tree.setHoverAction(hoverAction);
```

Hide an icon per-row/leaf-state with `setActionVisible`:

```java
hoverAction.setActionVisible((actionIndex, leaf, row) -> !(leaf && actionIndex == 0));
```

Listen for icon clicks:

```java
tree.addHoverActionListener((evt, row, actionIndex) -> {
    // actionIndex is the index into the Item[] passed to HoverAction
});
```

### 3. Enable drag-and-drop rules (optional)

Drag-and-drop reordering is on by default. Veto or react to drops with `TreeDropListener`:

```java
tree.addTreeDropListener(new TreeDropListener() {
    @Override
    public boolean canDrop(List<DefaultMutableTreeNode> nodes, DefaultMutableTreeNode newParent, int index) {
        return !(newParent.getUserObject() instanceof Item); // leaf nodes can't accept children
    }

    @Override
    public boolean nodesDropped(List<DefaultMutableTreeNode> nodes, DefaultMutableTreeNode newParent, int index) {
        return true; // return false to cancel the drop
    }
});
```

`canDrop` is polled during drag purely to pick the cursor (must be cheap, no side effects) — structural checks (no dropping onto itself/its own descendant) are already applied before it's called. `nodesDropped` fires once, right before the model is actually mutated.

### 4. Inline rename

Rename starts via `tree.startEditingAtPath(path)` (e.g. from an "Edit" hover action or F2) — click-to-rename is disabled so a single click doesn't accidentally start editing.

### 5. Node content

`com.swingcraft4j.treehoveraction.model.Item` is a ready-made node payload (a display value + an optional `Item.Type` icon) you can use as a `DefaultMutableTreeNode`'s `userObject` — see `Main.java` for a full example tree. You aren't required to use it; `HoverActionTree` doesn't depend on it.

## Key classes

| Class | Role |
|---|---|
| `HoverActionTree` | The tree component itself — hover tracking, rollover painting, editor/DnD wiring. |
| `HoverAction` | Defines and paints the row of trailing action icons, and hit-tests clicks on them. |
| `HoverCellRenderer` / `HoverCellEditor` | Row rendering and the inline rename editor. |
| `NodesTransferHandler` | Implements the actual drag-and-drop move. |
| `event.HoverActionListener` | Notified when a hover action icon is clicked. |
| `event.TreeDropListener` | Notified before/after a drop, with veto power. |
| `model.Item` | Optional ready-made node payload (value + icon). |
