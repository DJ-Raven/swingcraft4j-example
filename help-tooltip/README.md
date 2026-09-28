# Help Tooltip

Rich tooltip (`HelpTooltip`) for any `JComponent`, a Swing and FlatLaf port of IntelliJ's `HelpTooltip`. On top of a title it can show a shortcut, a description of one or more paragraphs, and a link. It opens in a popup that can extend past the window, with FlatLaf's drop shadow and rounded corners.

- **Content**: title, shortcut, HTML description with paragraphs, and an action or browser link.
- **Smart width**: a description, and the title above it, wraps at 250 px and then shrinks to its widest line, so short tooltips stay compact. A title on its own stays on one line.
- **Locations**: under the cursor (the default), or to the right, left, top or bottom of the component, or above a help button.
- **Timing**: shows after 500 ms and hides 150 ms after the mouse leaves. It also closes on its own 10 s (one line) or 30 s (multiline) after the mouse stops moving, or after a delay you set.
- **Reachable link**: while the tooltip has a link, it stays open as long as the mouse is over it.
- **Theme aware**: colors come from the FlatLaf `ToolTip.*` defaults, so it works in light and dark themes.

## Requirements

- Java 25.
- [FlatLaf](https://github.com/JFormDesigner/FlatLaf/) and FlatLaf Extras (for the link arrow icon).

## Usage

### 1. Install a tooltip

```java
JButton run = new JButton("Run");
new HelpTooltip()
        .setPlainTextTitle("Run 'Main'")
        .setShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK))
        .installOn(run);
```

Every setter returns the tooltip, so you can chain them. A component holds one `HelpTooltip`, and installing a new one replaces the old one.

### 2. Content

```java
new HelpTooltip()
        .setPlainTextTitle("Commit")
        .setShortcut("Ctrl+K")
        .setDescription("Records the staged changes.<p>Use <b>Push</b> to send them to the remote.")
        .setLink("Commit settings", () -> openSettings())
        .installOn(commit);
```

| Method | Meaning |
|---|---|
| `setPlainTextTitle(String)` | Title as plain text; any HTML in it is escaped. |
| `setTitle(String)` / `setTitle(Supplier<String>)` | Title that may contain HTML, without the `<html>` tags. A supplier is asked again on every show. |
| `setShortcut(String)` / `setShortcut(KeyStroke)` | Shortcut in a lighter color after the title. With no title, the shortcut is shown on its own. |
| `setDescription(String)` | HTML description. `<p>` starts a new paragraph, and `<br>` forces a line break. |
| `setLink(text, action)` | Link below the description. A click hides the tooltip, then runs the action. `setLink(text, action, true)` adds an external-link arrow. |
| `setBrowserLink(text, uri)` | Link with an arrow that opens the URI in the browser. |

With a title, the description is drawn in a lighter color than the title.

### 3. Location

```java
new HelpTooltip().setPlainTextTitle("Top").setLocation(HelpTooltip.Alignment.TOP).installOn(button);
```

| `Alignment` | Position |
|---|---|
| `CURSOR` | Below the mouse, moved up above it if it would not fit on the screen. The default. |
| `RIGHT`, `LEFT`, `TOP`, `BOTTOM` | Next to that side of the component. |
| `HELP_BUTTON` | Above a help button, slightly to the left. |

The tooltip is always kept on the screen.

For toolbars on the window edges, open the tooltip toward the window's center: `BOTTOM` for a top toolbar, `RIGHT` for a left one, `LEFT` for a right one and `TOP` for a bottom one or a status bar.

### 4. Timing

```java
new HelpTooltip()
        .setPlainTextTitle("Slow")
        .setInitialDelay(1000)        // ms before showing, 500 by default
        .setHideDelay(500)            // ms after the mouse leaves, 150 by default
        .setDismissDelay(5000)        // ms it stays open, 10 s (one line) / 30 s (multiline) by default
        .setNeverHideOnTimeout(true)  // no auto close at all
        .installOn(button);
```

The dismiss delay counts from the last mouse move over the component, not from when the mouse leaves; use the hide delay for that. Coming back onto the component before the hide delay runs out keeps the tooltip open. `setNeverHideOnTimeout(true)` takes priority over the dismiss delay. A FlatLaf help button (`JButton.buttonType = help`) never hides on timeout. A mouse press on the component hides the tooltip.

For toolbar buttons, a short show and hide delay makes the tooltips feel instant while still staying open as long as you hover:

```java
new HelpTooltip()
        .setPlainTextTitle("Run 'Main'")
        .setLocation(HelpTooltip.Alignment.BOTTOM)
        .setInitialDelay(100)
        .setHideDelay(50)
        .installOn(runButton);
```

### 5. Popup menus, disabling and removing

```java
HelpTooltip.setMasterPopup(moreButton, popupMenu);    // no tooltip while the menu is open
HelpTooltip.setMasterPopupOpenCondition(c, () -> ...); // show only while this returns true
HelpTooltip.disableTooltip(c);                        // stop showing, keep installed
HelpTooltip.enableTooltip(c);
HelpTooltip.hide(c);                                  // hide now, keep installed
HelpTooltip.dispose(c);                               // hide and uninstall
```

Set the master popup or condition after `installOn`, since it's stored on the installed tooltip; `dispose` clears it.

### 6. Tooltip manager

`HelpTooltipManager` shows a component's normal tooltip text (`getToolTipText(MouseEvent)`) as a help tooltip. That fits components whose tooltip depends on the hovered spot, such as a list or a table. The shortcut comes from a client property, either a `String` or a `Supplier<String>`:

```java
list.putClientProperty(HelpTooltipManager.SHORTCUT_PROPERTY, (Supplier<String>) () -> shortcutOfHoveredRow());
new HelpTooltipManager().register(list);  // takes the list away from Swing's ToolTipManager
```

The tooltip is replaced when the tooltip text under the mouse changes.

### Theme defaults

Colors and sizes come from `UIManager`, so they can be set in your FlatLaf properties file:

```properties
# description under a title
ToolTip.infoForeground=#8c8c8c
ToolTip.shortcutForeground=#8c8c8c
ToolTip.linkForeground=$Component.linkColor
HelpTooltip.regularDismissDelay=10000
HelpTooltip.fullDismissDelay=30000
HelpTooltip.maxWidth=250
HelpTooltip.verticalGap=4
HelpTooltip.defaultTextBorderInsets=8,10,10,16
HelpTooltip.smallTextBorderInsets=4,8,5,8
# font size change of the title and of a description under it
HelpTooltip.fontSizeDelta=0
HelpTooltip.descriptionSizeDelta=0
# shift of the side locations, and the gap below the cursor
HelpTooltip.xOffset=0
HelpTooltip.yOffset=0
HelpTooltip.mouseCursorOffset=20
```

If a key isn't set, the tooltip uses `ToolTip.background` and `ToolTip.foreground`, and blends the lighter colors from them.

The popup border follows FlatLaf's own tooltip keys, the same as a `JToolTip`:

```properties
ToolTip.borderCornerRadius=8
ToolTip.roundedBorderWidth=1
```

The line border follows `ToolTip.border`, the same as a regular tooltip. By default light themes draw a line and dark themes don't. Set it per theme to change the line; its insets are ignored, since the padding comes from the insets keys above:

```properties
[light]ToolTip.border=4,6,4,6,shade(@background,40%)
[dark]ToolTip.border=4,6,4,6,lighten(@background,12%)
```

## How it works

`installOn` adds a mouse listener to the component. Entering it starts a timer. When the timer fires, the tooltip builds a panel with a header label (the title plus the shortcut as HTML), one label per description paragraph, and the link. It then shows the panel with `PopupFactory` as a heavy-weight popup, so FlatLaf adds the shadow and rounded corners.

HTML labels normally take the full wrap width. Each label wraps its text in a `<div width=...>` only when the text is wider than the max width. It then walks the HTML view's rows and resizes the view to the widest row, so wrapped text doesn't leave empty space on the right.

A single timer handles showing, hiding and the auto close. A mouse move over the component while the tooltip is showing restarts the auto close. Leaving the component schedules a hide, which is skipped while the mouse is over a tooltip that has a link.

## Key classes

| Class | Role |
|---|---|
| `HelpTooltip` | Public API: content, location and timing, plus the static helpers. |
| `HelpTooltip.Alignment` | Where the tooltip opens relative to the component. |
| `HelpTooltipManager` | Shows `getToolTipText` as a help tooltip, with a shortcut from a client property. |
