# Badge Notification

An animated notification badge (`Badge`) that decorates any component — button, text field, label — through a `JLayer`, without touching the component's own border or UI.

- **Count, text or dot** — `setCount(5)` shows "5" (capped at "99+", adjustable with `setMaxCount`), `setText("new")` shows any text, and `null` text paints a small dot.
- **Any corner** — top/bottom and leading/trailing, following the component's orientation (right-to-left flips leading and trailing).
- **Overhangs the corner** — the layer reserves room on the badge's two sides (enough for the whole alert animation), so the badge sits on the component's visible corner instead of covering its content.
- **Animated show/hide** — the badge pops in with a slight overshoot and shrinks away when hidden.
- **Alert animation** — `alert()` plays a pulse, a settling wobble and a fading ripple to draw attention.
- **FlatLaf styling** — colors, font and sizes come from `UIManager` keys and can be overridden per badge with a FlatLaf style string.

Animations are driven by FlatLaf's `AnimatedIcon`, so they respect FlatLaf's animation settings and restart smoothly when triggered again mid-animation.

## Requirements

- Java 25.
- [FlatLaf](https://github.com/JFormDesigner/FlatLaf/) + [MigLayout](https://github.com/mikaelgrev/miglayout/).

## Usage

### 1. Wrap the component

`wrap` returns a `JLayer` — add the layer to the container instead of the component:

```java
JButton messages = new JButton("Messages");
Badge badge = new Badge();
panel.add(badge.wrap(messages));
```

Any `JComponent` works the same way, e.g. a combo box whose badge follows the selection:

```java
JComboBox<String> folder = new JComboBox<>(new String[]{"Inbox", "Sent", "Drafts"});
Badge folderBadge = new Badge();
folder.addActionListener(e -> folderBadge.setCount(unread[folder.getSelectedIndex()]));
panel.add(folderBadge.wrap(folder));
```

The component keeps its own border, background and focus ring, and the layer reports the component's baseline (shifted by the reserved room), so baseline alignment still works. One `Badge` decorates one component.

With MigLayout, the layer also publishes its reserved room as the `visualPadding` client property: MigLayout then aligns the component itself with its neighbors and lets the badge overhang into the surrounding gaps and insets. Leave enough insets around wrapped components at a container's edge so the badge isn't clipped by the container.

### 2. Show, hide and alert

```java
badge.setCount(3);              // show "3" (0 hides it)
badge.setMaxCount(100);         // counts above 100 show "100+" (default 99 → "99+")
badge.setText(null);            // dot badge
badge.setBadgeVisible(true);    // pop in
badge.setBadgeVisible(false);   // shrink away

badge.alert();                  // attention animation (shows the badge if hidden)
badge.alert(4);                 // set count + alert
badge.alert("new");             // set text + alert
```

### 3. Position

The badge sits on the top-trailing corner by default. Move it with `setPosition`:

```java
badge.setPosition(Badge.Position.TOP_LEADING);
```

| Position | Left-to-right | Right-to-left |
|---|---|---|
| `TOP_TRAILING` (default) | top-right | top-left |
| `TOP_LEADING` | top-left | top-right |
| `BOTTOM_TRAILING` | bottom-right | bottom-left |
| `BOTTOM_LEADING` | bottom-left | bottom-right |

The reserved room moves with the badge, and wide text always grows from the corner towards the component's center.

### 4. Styling

Defaults come from `UIManager`, e.g. in your FlatLaf properties file:

```properties
Badge.background=lazy(Actions.Red)
Badge.foreground=#fff
Badge.borderColor=$Panel.background
Badge.font=-2 bold
Badge.height=16
Badge.dotDiameter=8
Badge.borderWidth=2
Badge.offsetX=0
Badge.offsetY=0
Badge.margin=0,0,0,0
```

Override them per badge with a FlatLaf style string:

```java
badge.setStyle("background:$Actions.Blue;height:14;font:bold -3;offsetX:3;offsetY:3;");
```

| Key | Meaning |
|---|---|
| `background` / `foreground` | Badge fill and text color. |
| `borderColor` / `borderWidth` | Ring around the badge that separates it from the component; defaults to the parent's background. |
| `font` | Text font. |
| `height` | Height of a text badge (width grows inward with the text). |
| `dotDiameter` | Size of the dot badge. |
| `offsetX` / `offsetY` | Moves the badge from the component's visible corner towards its center, e.g. for borderless icon buttons. |
| `margin` | Extra room around the component inside the layer (`top,left,bottom,right`), on top of the room reserved for the badge and its animation. |

## Key classes

| Class | Role |
|---|---|
| `Badge` | Public API: badge state, position, `wrap(...)`, and FlatLaf-styleable properties. |
| `BadgeLayerUI` | The `LayerUI`: reserves room for the badge, forwards the baseline, and paints the badge with two `AnimatedIcon`s for the show/hide and alert animations. |
