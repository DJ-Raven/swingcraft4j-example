# Loading

Animated loading indicators (`LoadingIcon`). Each one is a plain Swing `Icon`, so it works anywhere an icon does: labels, buttons, text-field trailing components, and so on.

- **Built-in types, one class each**, grouped by family in `painter.arc`, `painter.dots`, `painter.shape` and `painter.text`.
- **Only what you use is loaded**: a type's class loads the first time you create it, so using one loader loads one painter class, however many types exist.
- **Custom animations**: implement `LoadingPainter`, which paints a single frame for a given point in the cycle.
- **Sizes**: presets `XS`–`XL` (16–32 px), or any pixel size, scaled with FlatLaf's UI scale.
- **Colors**: follows the component's foreground by default, or set a color directly or from a FlatLaf color reference such as `$Actions.Blue`.
- **Speed**: each type has its own cycle duration, which you can override per icon.
- **Starts and stops by itself**: there is no `start()`/`stop()`. The icon animates while it's being painted and its timer stops once no component is showing it.
- **Disabled state**: with a FlatLaf look and feel, disabled labels and buttons draw the same animation in the disabled foreground color.

## Requirements

- Java 25.
- [FlatLaf](https://github.com/JFormDesigner/FlatLaf/).

## Usage

### 1. Show a loader

```java
JLabel loader = new JLabel(new LoadingIcon(new SpinnerPainter(), LoadingIcon.Size.MD));
panel.add(loader);
```

`new LoadingIcon()` with no painter uses `SpinnerPainter`.

Put one inside a button, for example while a save is running:

```java
LoadingIcon spinner = new LoadingIcon(new SpinnerPainter(), LoadingIcon.Size.XS);

save.setEnabled(false);
save.setIcon(spinner);      // animates in the disabled color
save.setText("Saving");
// ...when done:
save.setIcon(null);
save.setEnabled(true);
```

Or as a text field's trailing component:

```java
JLabel searching = new JLabel(new LoadingIcon(new RingPainter(), LoadingIcon.Size.XS));
search.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, searching);
searching.setVisible(true);  // hide it again when the search finishes
```

To stop an animation, remove the icon or hide the component; the timer stops on its own. A single `LoadingIcon` can be shared by several components, and painters are immutable, so a painter instance can be shared too.

### 2. Types and sizes

```java
icon.setPainter(new TypingPainter());
icon.setSize(LoadingIcon.Size.LG);  // XS 16, SM 20, MD 24, LG 28, XL 32
icon.setSize(48);                   // or any unscaled pixel size
```

| Package | Class | Animation | Cycle |
|---|---|---|---|
| `painter.arc` | `SpinnerPainter` (default) | Arc that grows and shrinks while it rotates. | 1500 ms |
| `painter.arc` | `RingPainter` | Arc spinning with easing around a faint track. | 1000 ms |
| `painter.arc` | `DualRingPainter` | Two opposite arcs spinning with easing. | 1100 ms |
| `painter.arc` | `ClockPainter` | Clock face with a fast minute hand and a slow hour hand. | 4000 ms |
| `painter.arc` | `RadarPainter` | Radar screen with a sweeping beam and a fading trail. | 2000 ms |
| `painter.arc` | `AtomPainter` | Nucleus with three tilted orbits, each carrying an electron. | 1500 ms |
| `painter.arc` | `FadeArcPainter` | Open ring spinning steadily, fading from a solid head to a transparent tail. | 1000 ms |
| `painter.dots` | `DotsPainter` | Three dots pulsing one after another. | 1000 ms |
| `painter.dots` | `TypingPainter` | Three dots hopping in turn, like a typing indicator. | 1200 ms |
| `painter.dots` | `WavePainter` | Five dots riding a sine wave. | 1100 ms |
| `painter.dots` | `GridPainter` | Three-by-three dots pulsing in a diagonal wave. | 1200 ms |
| `painter.dots` | `SpokesPainter` | Twelve spokes with a fading trail, like a classic activity indicator. | 1000 ms |
| `painter.dots` | `SwapPainter` | Two dots swapping places around the center. | 1000 ms |
| `painter.dots` | `ChasingDotsPainter` | Two dots circling each other while growing and shrinking in turn. | 2000 ms |
| `painter.dots` | `CometPainter` | Comet circling with a tail of shrinking dots that stretches out and snaps back. | 1700 ms |
| `painter.dots` | `EllipsisPainter` | Three dots scrolling right, growing in on the left and shrinking away on the right. | 700 ms |
| `painter.dots` | `HelixPainter` | DNA double helix turning, near dots bigger and brighter. | 1600 ms |
| `painter.shape` | `BallPainter` | Ball bouncing, squashing as it lands. | 900 ms |
| `painter.shape` | `BarsPainter` | Three bars stretching one after another. | 1000 ms |
| `painter.shape` | `PulsePainter` | Two discs growing from the center and fading out. | 1600 ms |
| `painter.shape` | `RipplePainter` | Two outlined rings expanding and fading. | 1500 ms |
| `painter.shape` | `FlipPainter` | Rounded square flipping over one axis, then the other. | 1400 ms |
| `painter.shape` | `LinePainter` | Segment sliding along a track, like an indeterminate progress bar. | 1400 ms |
| `painter.shape` | `InfinityPainter` | Segment moving along a figure-eight track. | 2000 ms |
| `painter.shape` | `PacmanPainter` | Pac-Man chomping on a row of dots sliding into its mouth. | 1000 ms |
| `painter.shape` | `HourglassPainter` | Hourglass draining its sand, then flipping over. | 2400 ms |
| `painter.text` | `TextShimmerWavePainter` | Custom text whose letters brighten and lift one after another in a wave. | 1000 ms + 50 ms per letter |

`TextShimmerWavePainter` takes your own text (`"Loading..."` if none is given), and optionally a font whose family and style it uses (the look and feel's default font otherwise). Its icon is as wide as the text, and the icon size sets the text height:

```java
LoadingIcon thinking = new LoadingIcon(new TextShimmerWavePainter("Generating response..."));
thinking.setSize(26);  // about 13 pt text
```

### 3. Color and speed

```java
icon.setColor(new Color(0xa855f7));
icon.setColor(null);          // back to Loading.color, or the component's foreground
icon.setDuration(600);        // one cycle in ms; -1 uses the painter's own duration
```

All animations run on the system clock, so loaders of the same type and duration stay in sync with each other.

### 4. Styling

The default color can come from `UIManager`, for example in your FlatLaf properties file:

```properties
Loading.color=$Component.accentColor
```

Each icon can also be styled with a FlatLaf style string:

```java
icon.setStyle("color:$Actions.Blue;size:32;duration:800;");
```

| Key | Meaning |
|---|---|
| `size` | Unscaled height in pixels, which is also the width for square types. Defaults to 24 (`Size.MD`). |
| `color` | Loader color. If it isn't set, the icon uses `Loading.color`, then the component's foreground. |
| `duration` | Length of one animation cycle in milliseconds. |

### 5. Custom animation

A new type is one class implementing `LoadingPainter`. It paints one frame into a `size × size` square (`size` is already UI-scaled), unless it gives its own width. `fraction` goes from 0 to 1 over each cycle, and `g` arrives already antialiased, translated and set to the loader's color:

```java
public class OrbitPainter implements LoadingPainter {

    @Override
    public void paint(Graphics2D g, float size, float fraction) {
        Color color = g.getColor();
        float r = size / 10f;
        float radius = size / 2 - r;
        for (int i = 0; i < 8; i++) {
            double angle = 2 * Math.PI * i / 8 - Math.PI / 2;
            float x = size / 2 + radius * (float) Math.cos(angle);
            float y = size / 2 + radius * (float) Math.sin(angle);
            float behind = Math.floorMod((int) (fraction * 8) - i, 8);
            g.setColor(LoadingPainter.alpha(color, 1 - behind / 8f));
            g.fill(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2));
        }
    }

    @Override
    public int getDuration() {
        return 900;   // optional; 1000 ms if not overridden
    }
}

JLabel loader = new JLabel(new LoadingIcon(new OrbitPainter()));
```

For a frame that isn't square, also override `getWidth(size)` to return its width for a given height; the icon sizes itself to match.

`LoadingPainter.alpha(color, a)` fades the loader color, which is useful for tracks and trails. `PainterMath` has the helpers the built-in painters use: `ease`, `easeOut`, `pulse`, `wrap` and `roundStroke`.

## Key classes

| Class | Role |
|---|---|
| `LoadingIcon` | Public API: the animated `Icon`. It holds size, color, duration and FlatLaf style, and repaints only the icon area of each component showing it. |
| `LoadingPainter` | Interface for painting one frame of an animation, plus its default duration and, for non-square types, its width. |
| `painter.arc`, `painter.dots`, `painter.shape`, `painter.text` | One class per built-in type, grouped by family. |
| `PainterMath` | Shared easing and timing helpers. |
