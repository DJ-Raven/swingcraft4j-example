# Skeleton

`Skeleton` is a `LayerUI` for `JLayer` that shows loading placeholders. While loading, it hides the real view and draws shimmering placeholder shapes that follow the view's layout. Because the shapes come from the real components, you don't need to build a separate "loading" screen:

- **Labels** become a bar the width of their text, plus a rounded block where an icon would be.
- **Text areas** become one bar per line, with the last line shorter, like a paragraph.
- **Other components** (text fields, buttons, lists, tables, combo boxes…) become a rounded block the size of the component.
- **Panels** are containers only; their children are drawn, and their backgrounds and borders are kept.

While loading, the layer also blocks mouse and keyboard input.

## Requirements

- Java 25.
- [FlatLaf](https://github.com/JFormDesigner/FlatLaf/) + [MigLayout](https://github.com/mikaelgrev/miglayout/).

## Usage

### 1. Wrap the content in a `JLayer`

```java
Skeleton skeleton = new Skeleton();
JLayer<JComponent> layer = new JLayer<>(content, skeleton);
panel.add(layer);
```

Only put the part that loads inside the layer. Controls that should stay usable while loading belong outside it.

### 2. Toggle loading

```java
skeleton.setLoading(true);   // show placeholders, block input
// ... load data off the EDT ...
skeleton.setLoading(false);  // show the real content
```

The shimmer animation runs only while loading and the layer is showing.

### 3. Give the placeholders a realistic shape

Placeholders are measured from the real components, so fill them with placeholder data before loading. The text sets the width of each bar, and a text area's height sets how many lines it shows:

```java
title.setText("Placeholder title");
body.setText("A few sentences of placeholder text, about as long as the real content.");
skeleton.setLoading(true);
```

### 4. Circle avatars

Set `Skeleton.CIRCLE` on a `JLabel` to draw the whole label as a circle, with or without an icon. Give the label a fixed square size:

```java
JLabel avatar = new JLabel();
avatar.putClientProperty(Skeleton.CIRCLE, true);
avatar.setPreferredSize(new Dimension(40, 40));
```

## API

| Member | Description |
|---|---|
| `new Skeleton()` | Creates the layer UI; pass it to a `JLayer`. |
| `setLoading(boolean)` / `isLoading()` | Shows the placeholders and blocks input, or restores the real view. |
| `Skeleton.CIRCLE` | `JLabel` client property (`Boolean`): draw the label as a circle placeholder. |
