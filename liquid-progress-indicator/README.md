# Liquid Progress Indicator

Progress meter (`LiquidProgress`) drawn as liquid filling a container: a round gauge, a box, a battery, a flask and more. Two sine waves move across the surface in opposite directions, bubbles rise through the liquid, and the percentage is shown in the middle.

- **Animated fill**: a new value doesn't jump. The liquid rises or falls to it at a set speed.
- **Two wave layers**: each wave swells up and down while it scrolls sideways, with its own speed, height and width.
- **Bubbles**: small bubbles rise from the bottom and start over when they reach the surface.
- **Theme aware**: the front wave defaults to the FlatLaf accent color, the back wave to that color blended into the meter background, and the rings, outlines and track to shades of the meter background, so every style works in light and dark themes.
- **Any size**: the meter scales to fit the component. It's 150 × 150 by default, scaled with FlatLaf's UI scale.
- **8 styles**: circle, gauge ring, box, battery, flask, test tube, heart and droplet. Each is drawn by a `LiquidPainter`, and you can add your own without changing the liquid.
- **Two-tone text**: styles can color the text differently over the empty track and inside the liquid, so it stays readable at any level.
- **Custom text**: show your own text for the current percent, such as "Loading 45%" or "Done". Long text shrinks to fit the container.

## Requirements

- Java 25.
- [FlatLaf](https://github.com/JFormDesigner/FlatLaf/).

## Usage

### 1. Make a meter

```java
LiquidProgress progress = new LiquidProgress(60);
panel.add(progress);

progress.setValue(80);  // the liquid rises from 60 to 80
```

The value is a percentage from 0 to 100. Values outside that range are clamped. `getValue()` returns the target and `getDisplayedValue()` returns the level currently shown.

### 2. Units

All lengths (wave height, border width, font size and so on) are in a 300 × 300 space that is scaled to the component's size. A border width of 19 is always 19/300 of the meter, whether the meter is 100 px or 400 px wide.

### 3. Waves

```java
LiquidWave front = progress.getFrontWave();
front.setColor(new Color(0x800080));
front.setMaxAmplitude(12);     // highest point of the wave above the level
front.setFrequency(30);        // wave length / 2π, so larger is wider
front.setAngularSpeed(100);    // how fast it swells, in degrees per second
front.setHorizontalSpeed(-150); // how fast it scrolls; negative moves right

progress.getBackWave().setColor(new Color(0xffc0cb));
```

| Wave | Color when `null` | Defaults |
|---|---|---|
| Front | `Component.accentColor` | amplitude 12, frequency 30, angular speed 100, horizontal speed -150 |
| Back | Front color blended into the meter background | amplitude 9, frequency 30, angular speed 100, horizontal speed 150 |

An angular speed of 0 keeps the wave flat.

### 4. Background

```java
progress.setBackground(Color.WHITE);                // fills the component (it's opaque)
progress.setMeterBackground(new Color(0xf0f0f0));  // base the rings, outlines, track and back wave are shaded from
```

The two are separate, so changing the component's background doesn't change the meter's colors. The component background defaults to `Panel.background`, and `setOpaque(false)` makes it transparent. If the meter background isn't set, it uses `Panel.background` too, so it follows theme changes.

### 5. Text, bubbles and speed

```java
progress.setTextPainted(true);                        // percentage in the middle
progress.setPercentSignPainted(true);                 // "60%" instead of "60"; off by default
progress.setTextFormatter(v -> "Loading " + v + "%"); // your own text; null shows the percentage
progress.setFontSize(70);                             // in the 300 × 300 space; 0 uses the style's size
progress.setTextColor(Color.WHITE);                   // inside the liquid, white by default
progress.setTrackTextColor(new Color(0x555555));      // over the empty track; null uses the style's
progress.setBubblesPainted(true);
progress.setFillSpeed(30);                            // percent per second; 0 or less jumps right away
```

The text uses the component's font at the font size, so `setFont` changes the family and style. The text color has a soft dark shadow, so it stays readable over any liquid color.

Each style has its own default text size, so the text fits narrow shapes like the test tube. If the track text color is set (or the style gives one), the text is split along the front wave: it's the track color above the liquid and the text color inside it. If it isn't, the whole text is the text color.

#### Custom text

`setTextFormatter` takes the displayed percent, rounded to a whole number, and returns the text to show. The formatter replaces the percentage, so the percent sign setting no longer applies. If it returns `null` or an empty string, no text is shown.

```java
progress.setTextFormatter(v -> v + " MB");
progress.setTextFormatter(v -> v < 100 ? "Loading " + v + "%" : "Done");
```

Text wider than 80% of the container shrinks to fit, so longer text stays inside the meter and inside narrow shapes.

### 6. Styles

Set a style with `setPainter`. The painters are in the `com.swingcraft4j.liquidprogress.painter` package:

```java
import com.swingcraft4j.liquidprogress.painter.*;

progress.setPainter(new GaugePainter());
progress.setPainter(ShapePainter.battery());
```

| Style | Painter | Text size | Two-tone text |
|---|---|---|---|
| Circle (default) | `new CirclePainter()` | 70 | No |
| Gauge ring | `new GaugePainter()` | 62 | Yes, a darker shade of the liquid |
| Box | `ShapePainter.box()` | 64 | Yes, gray |
| Battery | `ShapePainter.battery()` | 44 | Yes, gray |
| Flask | `ShapePainter.flask()` | 40 | Yes, gray |
| Test tube | `ShapePainter.testTube()` | 30 | Yes, gray |
| Heart | `ShapePainter.heart()` | 54 | Yes, gray |
| Droplet | `ShapePainter.droplet()` | 54 | Yes, gray |

A painter keeps images of its static parts for one meter, so give each meter its own painter instead of sharing one.

#### Circle

A circular track, a thick ring around it and a soft drop shadow. This is the style of the original web demo.

```java
CirclePainter circle = new CirclePainter();
circle.setBorderWidth(19);
circle.setRingColor(new Color(0xfafafa));
circle.setTrackColor(new Color(0xe2e2e2));
circle.setShadowPainted(false);
```

| Property | Meaning |
|---|---|
| `borderWidth` | Width of the ring. Defaults to 19. 0 hides it. |
| `ringColor` | Color of the ring. If it isn't set, the painter uses a lighter shade of the meter background. |
| `trackColor` | Color of the empty part of the meter. If it isn't set, the painter uses a darker shade of the meter background. |
| `shadowPainted` | Whether the drop shadow is painted. Defaults to `true`. |

#### Gauge ring

A thin colored ring, a gap, then the liquid circle. The text is a darker shade of the liquid above the surface and white inside it.

```java
GaugePainter gauge = new GaugePainter();
gauge.setRingWidth(8);
gauge.setGap(10);
gauge.setRingColor(Color.ORANGE);
```

| Property | Meaning |
|---|---|
| `ringWidth` | Width of the ring. Defaults to 8. 0 hides it. |
| `gap` | Space between the ring and the liquid. Defaults to 10. |
| `ringColor` | Color of the ring. If it isn't set, the painter uses the front wave color. |
| `trackColor` | Color of the empty part. If it isn't set, the painter uses a shade of the meter background. |

#### Shapes

`ShapePainter` draws an outlined container with a glass highlight. The factory methods give the six built-in shapes, and the constructor takes any shape:

```java
ShapePainter battery = ShapePainter.battery();
battery.setOutlineWidth(10);
battery.setOutlineColor(Color.GRAY);
battery.setHighlightPainted(false);

// your own shape: container, extra parts (cap, rim), glare, outline width, text size
Shape star = ...;
progress.setPainter(new ShapePainter(star, null, null, 8, 50));
```

| Property | Meaning |
|---|---|
| `outlineWidth` | Width of the outline, centered on the shape's edge. |
| `outlineColor` | Color of the outline and the extra parts. If it isn't set, the painter uses a darker shade of the meter background, or a lighter one in dark themes. |
| `trackColor` | Color of the empty part. If it isn't set, the painter uses a shade of the meter background. |
| `highlightPainted` | Whether the glass glare is painted. Defaults to `true`. Only box, battery and test tube have one. |

The shape should fit in the 300 × 300 space with room for half the outline width.

### 7. Custom styles

A style is a `LiquidPainter`. It gives the area the liquid fills and paints what's under and over it, all in the 300 × 300 space:

```java
public class SquarePainter implements LiquidPainter {
    public Shape getFluidShape(LiquidProgress c) {
        return new RoundRectangle2D.Float(40, 40, 220, 220, 30, 30);
    }
    public void paintBackground(Graphics2D g, LiquidProgress c) {
        g.setColor(new Color(0xe2e2e2));
        g.fill(getFluidShape(c));
    }
    public void paintForeground(Graphics2D g, LiquidProgress c) {
        g.setColor(Color.GRAY);
        g.setStroke(new BasicStroke(6));
        g.draw(getFluidShape(c));
    }
}

progress.setPainter(new SquarePainter());
```

The liquid fills the fluid shape from the bottom of its bounds (0%) to the top (100%), and the text is centered in those bounds. Anything painted outside the 300 × 300 square is cut off.

The liquid is cut to the fluid shape with a mask image, which is redrawn only when the shape changes. It checks for a change with `equals`. `RoundRectangle2D` and `Ellipse2D` compare their values, so returning a new one each time is fine. A `Path2D` or `Area` only equals itself, so build it once and return that same instance, as `ShapePainter` does. Otherwise the mask is redrawn on every frame.

The painter runs on every frame. If its parts don't change, draw them once into an image and reuse it, as the built-in painters do with `ImageCache`. The meter background comes from `c.getMeterBackground()`, not `c.getBackground()`, and the painted wave colors from `c.getFrontColor()` and `c.getBackColor()`.

Two optional methods give the style's text defaults:

- `getFontSize(c)` is the text size, 70 unless overridden.
- `getTrackTextColor(c)` is the text color over the empty track. It's `null` unless overridden, which keeps the text in one color.

## How it works

One Swing `Timer`, shared by every meter on screen, ticks about 60 times a second. A meter joins it when it's shown and leaves when it's hidden, and the timer stops when no meter is showing. Each tick, for each meter:

- moves the displayed value toward the target at `fillSpeed`;
- advances each wave: its swell angle by `angularSpeed` and its horizontal offset by `horizontalSpeed`;
- moves each bubble up, and restarts it lower in the liquid once it passes the surface.

Each wave is the curve `level + amplitude × sin((x + offset) / frequency)`, where `amplitude = maxAmplitude × sin(angle)`.

### Painting

Painting is done in the 300 × 300 space, scaled to the component. In order, it paints the painter's background, then the back and front waves with the bubbles, then the text, then the painter's foreground.

Each frame is kept cheap:

- **One image per frame**: the whole meter is painted into one image in software, then drawn to the screen once. Antialiased shapes drawn straight onto a hardware-accelerated surface, such as Direct3D on Windows, cost much more.
- **Liquid layer**: the waves and bubbles are drawn into a layer image. That layer is then cut to the fluid shape with a mask image, which is only redrawn when the size or shape changes. There's no shape intersection math per frame, and the edge stays antialiased.
- **Cached parts**: the built-in painters draw their static parts once as images, and redraw them only when the size, colors or widths change. Those parts are the shadow, track, rings, outlines and highlights. The text shadow is blurred again only when the text changes.
- **Whole pixels**: cached images are drawn 1:1 at whole device pixels, so they're never resampled.
- **Two-tone text**: the text is drawn twice into a layer. The first copy is cut away where the front liquid is, and the second is kept only there. The cut uses an antialiased mask of the front wave, so the color change along the wave stays smooth.

## Key classes

| Package | Class | Role |
|---|---|---|
| `liquidprogress` | `LiquidProgress` | Public API: the meter component. It holds the value, waves, bubbles, text options and painter. |
| `liquidprogress` | `LiquidWave` | One wave layer: color, height, width and speeds. |
| `liquidprogress.painter` | `LiquidPainter` | A style: the fluid area and what's painted under and over the liquid. |
| `liquidprogress.painter` | `CirclePainter` | The round style: circular track, ring and drop shadow. |
| `liquidprogress.painter` | `GaugePainter` | The gauge ring style: thin ring, gap and two-tone text. |
| `liquidprogress.painter` | `ShapePainter` | Outlined containers: box, battery, flask, test tube, heart, droplet, or any shape. |
| `liquidprogress.util` | `ImageCache` | Keeps a painter's static parts as a device-resolution image. |
| `liquidprogress.util` | `Shadow` | Blurred shadow of a shape, drawn from a cached image. |

All packages are under `com.swingcraft4j`.
