# Switch Button

Animated switch (`SwitchIcon`) for a `JCheckBox` or `JToggleButton`. It's a plain Swing `Icon` built on FlatLaf's `FlatAnimatedIcon`, so the button keeps its normal selection model, events and keyboard handling. Only the look changes.

- **8 types**: `CLASSIC`, `INSET`, `OVERHANG`, `LINE`, `SOFT`, `TONAL`, `MATERIAL`, `LINE_TINTED`.
- **Animated toggle**: the thumb slides and the colors blend from off to on. Toggling again mid-animation reverses from where it is.
- **Theme aware**: the on color defaults to the FlatLaf accent color, the off color to a shade of the background, and soft parts are the accent blended into the background, so it works in light and dark themes.
- **Any size**: 36 × 20 by default, scaled with FlatLaf's UI scale.
- **States**: hover, focus ring, disabled, and right-to-left orientation.

## Requirements

- Java 25.
- [FlatLaf](https://github.com/JFormDesigner/FlatLaf/).

## Usage

### 1. Make a switch

```java
JCheckBox wifi = new JCheckBox("Wi-Fi", new SwitchIcon(SwitchType.CLASSIC), true);
wifi.addActionListener(e -> setWifi(wifi.isSelected()));
```

`new SwitchIcon()` with no type uses `CLASSIC`. Set the icon on any `AbstractButton` whose selection means "on", such as `JCheckBox` or `JToggleButton`.

### 2. Types

| Type | Look |
|---|---|
| `CLASSIC` | Filled track with a white thumb inset. |
| `INSET` | Filled track with a smaller white thumb. |
| `OVERHANG` | Outlined thumb overhanging a slimmer filled track. |
| `LINE` | Outlined thumb on a line. |
| `SOFT` | Outlined thumb on a soft full-height track. |
| `TONAL` | Filled track with a soft thumb inset. |
| `MATERIAL` | Filled thumb on a soft, slimmer track. |
| `LINE_TINTED` | Outlined soft thumb on a line. |

"Soft" is the current color blended into the component's background.

### 3. Size

```java
new SwitchIcon(SwitchType.INSET, 48, 26);  // unscaled width and height
```

The icon is bigger than the switch on each side by a 1 px gap plus the focus ring width, which leaves room for the focus ring.

### 4. Colors and speed

```java
SwitchIcon icon = new SwitchIcon(SwitchType.MATERIAL);
icon.setOnColor(new Color(0xa855f7));
icon.setOffColor(Color.GRAY);
icon.setThumbColor(Color.WHITE);  // the white thumb of CLASSIC, INSET, OVERHANG, LINE and SOFT
icon.setDuration(300);            // toggle animation in ms, 200 by default
icon.setFocusPainted(false);      // no focus ring, while the switch stays focusable
icon.setFocusWidth(3);            // focus ring width, from the theme by default
```

Or with a FlatLaf style string:

```java
icon.setStyle("onColor:$Actions.Green;duration:300;focusPainted:false");
```

| Key | Meaning |
|---|---|
| `onColor` | Color when selected. If it isn't set, the icon uses `Switch.onColor`, then `Component.accentColor`. |
| `offColor` | Color when not selected. If it isn't set, the icon uses `Switch.offColor`, then a shade of the background. |
| `thumbColor` | Color of white thumbs. If it isn't set, the icon uses `Switch.thumbColor`, then white. |
| `duration` | Length of the toggle animation in milliseconds. |
| `focusPainted` | Whether the focus ring is painted. Defaults to `true`. |
| `focusWidth` | Width of the focus ring. If it isn't set, the icon uses `Switch.focusWidth`, then `CheckBox.icon.focusWidth`, then `Component.focusWidth`, then 2. |

Colors are resolved on every paint, so the switch follows theme changes. Changing a setting or style repaints every button showing the icon, so a visible switch updates right away.

### Focus ring

The focus ring follows the switch's outer shape: around the track for `CLASSIC`, `INSET`, `SOFT` and `TONAL`, and around the thumb for the other types. It's painted only when all of these are true:

- the icon's `focusPainted` is `true`;
- the button is focusable, so `setFocusable(false)` hides it;
- the button's own `isFocusPainted()` is `true`, so `button.setFocusPainted(false)` also hides it;
- the button has focus.

`icon.setFocusPainted(false)` keeps the switch reachable with the keyboard. It only drops the ring.

### Theme defaults

The defaults can also come from your FlatLaf properties file:

```properties
Switch.onColor=$Component.accentColor
Switch.offColor=#c4c8cf
Switch.thumbColor=#ffffff
Switch.focusWidth=2
```

The focus ring width follows the theme like FlatLaf's own check box: 1 px in FlatLaf Light and Dark, 2 px in IntelliJ and Darcula. The icon's size follows it too, so a theme change re-lays out the switch.

### 5. Settings row

To put the switch at the right edge of a row, use a separate label and a check box with no text:

```java
JPanel row = new JPanel(new MigLayout("insets 0", "[grow][]"));
row.add(new JLabel("Bluetooth"));
row.add(new JCheckBox(null, new SwitchIcon(SwitchType.SOFT), false));
```

## How it works

`SwitchIcon` extends FlatLaf's `FlatAnimatedIcon`. `getValue` returns 1 when the button is selected and 0 otherwise. When that value changes, FlatLaf's `AnimationSupport` runs an animator that repaints the icon with values in between, and `paintIconAnimated` draws every frame from that one value:

- the thumb's x position is interpolated between the two ends of the track;
- the accent color is `ColorFunctions.mix(onColor, offColor, value)`;
- soft parts are the accent color mixed into the background.

Each `SwitchType` is a set of proportions (track height and thumb size relative to the switch height) plus which color the track and thumb take, so all eight types share the same painting code.

## Key classes

| Class | Role |
|---|---|
| `SwitchIcon` | Public API: the animated switch `Icon`. It holds type, size, colors and duration. |
| `SwitchType` | The eight looks, as track and thumb proportions and colors. |
