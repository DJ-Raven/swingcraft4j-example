# QR Code Generator

Component (`QrCodeView`) that draws a QR code with custom styles. You choose the shape of the body modules, the eye frames and the eye balls, add colors and a gradient, and put a logo in the center. [ZXing](https://github.com/zxing/zxing) only encodes the text. Everything else is drawn with Java2D, so the code stays sharp at any size.

- **12 body shapes**: square, rounded, dots, fluid (joined modules with rounded corners), horizontal lines, vertical lines, leaf, diamond, connected dots, hexagon, cross and star.
- **Eye shapes**: 6 frames (square, rounded, circle, leaf, octagon, dotted) and 7 balls (square, rounded, circle, leaf, diamond, octagon, star), in any combination.
- **Colors**: a body color, an optional diagonal gradient, separate eye frame and eye ball colors, and a background with optional rounded corners, or no background for a transparent code.
- **Center logo**: any `Icon`, scaled to fit. The modules behind it are cleared, so it doesn't sit on top of broken modules.
- **Custom shapes**: the body and eye shapes are small interfaces, so you can add your own.
- **No component needed**: `QrCode` is a plain immutable object. Build one and get a `BufferedImage`, paint it into any `Graphics2D`, or export an SVG. It works headless, so you can use it on a server.
- **SVG export**: shapes, colors and the gradient stay vector, and the logo is embedded as a PNG.
- **Crisp on screen**: modules are snapped to whole device pixels, so edges stay sharp on HiDPI screens. Snapping is skipped when it would shrink the code by more than 10%, which can happen with small codes.

## Requirements

- Java 25.
- [FlatLaf](https://github.com/JFormDesigner/FlatLaf/).
- [ZXing](https://github.com/zxing/zxing) `com.google.zxing:core`.

## Usage

### 1. Make a code

```java
QrCodeView qr = new QrCodeView("https://example.com");
panel.add(qr);

qr.setText("Hello");  // encodes again
```

The code is drawn as a square centered in the component. The default size is 200 × 200, scaled with FlatLaf's UI scale. If the text is empty or too long for a QR code, nothing is drawn and `getMatrix()` returns `null`. Listen for the `"matrix"` property to find out when that changes:

```java
qr.addPropertyChangeListener("matrix", e -> {
    QrMatrix m = qr.getMatrix();
    status.setText(m == null ? "Too long" : "Version " + m.getVersion());
});
```

Text that fits in ISO-8859-1 is encoded without a charset declaration, which most scanners can read. Other text is encoded as UTF-8.

### 2. Error correction

```java
qr.setErrorCorrection(ErrorCorrectionLevel.H);
```

| Level | Recovers | Use |
|---|---|---|
| `L` | ~7% | Smallest code |
| `M` (default) | ~15% | General use |
| `Q` | ~25% | Heavy styles |
| `H` | ~30% | With a logo |

Higher levels make the code denser. Use `H` whenever you show a logo, because the logo covers modules.

### 3. Shapes

```java
import com.swingcraft4j.qrcode.shape.*;

qr.setBodyShape(BodyStyle.FLUID);
qr.setEyeFrameShape(EyeFrameStyle.ROUNDED);
qr.setEyeBallShape(EyeBallStyle.CIRCLE);
```

| Part | Styles |
|---|---|
| Body (`BodyStyle`) | `SQUARE` (default), `ROUNDED`, `DOTS`, `FLUID`, `HORIZONTAL`, `VERTICAL`, `LEAF`, `DIAMOND`, `CONNECTED`, `HEXAGON`, `CROSS`, `STAR` |
| Eye frame (`EyeFrameStyle`) | `SQUARE` (default), `ROUNDED`, `CIRCLE`, `LEAF`, `OCTAGON`, `DOTTED` |
| Eye ball (`EyeBallStyle`) | `SQUARE` (default), `ROUNDED`, `CIRCLE`, `LEAF`, `DIAMOND`, `OCTAGON`, `STAR` |

- `FLUID` joins neighboring modules. It rounds each outer corner that has no neighbor, and fills inner corners with a small curve, so the code looks like liquid.
- `HORIZONTAL` and `VERTICAL` join modules into bars with round ends.
- `CONNECTED` draws a dot per module and joins it to its dark neighbors with thin bars, like beads on a string.
- `CROSS` draws plus signs whose arms join their neighbors into a lattice.
- The `LEAF` eyes are rounded except at the corner facing the code's center, so all three point inward.
- The `DOTTED` frame is a ring of dots, one per module. The dots overlap slightly, so scanners still see a solid ring.

The `DOTTED` frame with the `STAR` ball can't be read by scanners, so that pair isn't allowed. `build()` throws an `IllegalArgumentException` for it, and so do the view's eye setters if one of them would create it. Change both shapes at once with `setCode`, or check a pair first with `QrCode.isSupported(frame, ball)`.

The other combinations were tested by decoding them with ZXing's reader at error correction `H`, without a logo and with the largest logo (0.3), at sizes from 250 to 800 pixels. They all decode at every tested size, except the `DOTTED` frame with the `DIAMOND` ball, which fails at some sizes.

Phone scanners are often more forgiving than ZXing, but test any code you publish with the scanners your users have.

### 4. Colors

```java
qr.setBodyColor(new Color(0x0369a1));
qr.setGradientColor(new Color(0x0891b2));   // diagonal gradient; null for a solid body
qr.setEyeFrameColor(new Color(0x0c4a6e));   // null paints the eyes like the body
qr.setEyeBallColor(new Color(0x0c4a6e));
qr.setCodeBackground(Color.WHITE);          // null for transparent
qr.setBackgroundRadius(2);                  // corner radius, in modules
qr.setMargin(2);                            // quiet zone, in modules
```

The colors don't follow the FlatLaf theme, because scanners need a dark code on a light background. The defaults are black on white. Keep enough contrast: a light gradient end color or a dark background can make the code hard to read.

The QR spec asks for a 4-module quiet zone. Most scanners read a code with 2, which is the default.

#### Transparent background

```java
qr.setCodeBackground(null);
```

With no background, nothing is painted behind the modules: the quiet zone, the light modules and the hole around the logo are all transparent. The component isn't opaque, so on screen its parent shows through, and `createImage` returns an ARGB image, so a PNG saved from it keeps the transparency.

Scanners still need dark modules on a light surface. A transparent code scans when it's placed on white or a light color, but not on a dark one, so only use it where you know the surface is light, like a printed page or a light web page.

### 5. Logo

```java
FlatSVGIcon logo = new FlatSVGIcon("icons/logo.svg");
qr.setLogo(logo);
qr.setLogoSize(0.22f);   // width as a fraction of the code, up to 0.3
qr.setLogoMargin(1);     // modules cleared around it
qr.setErrorCorrection(ErrorCorrectionLevel.H);
```

The logo is scaled to fit its square and keeps its aspect ratio. An SVG icon stays sharp at any size. The square is a whole number of modules, centered on the grid. The modules under it, plus the margin, are cleared, so the background shows around the logo (or, with a transparent background, whatever is behind the code).

### 6. Export

```java
BufferedImage image = qr.createImage(1024);
ImageIO.write(image, "png", new File("qr.png"));

String svg = qr.getCode().toSvg(1024);
Files.writeString(Path.of("qr.svg"), svg);
```

`createImage` and `toSvg` throw an `IllegalStateException` when there's nothing to draw, because the text is empty or too long. Check `getMatrix()` first.

The image is exactly the given size, including the quiet zone. With no background set, it's transparent everywhere except the modules, eyes and logo. It isn't snapped to whole pixels like the on-screen code, so modules may be a fraction of a pixel wide. Pick a size that's a multiple of the module count plus the margins if you need exact pixels. `getCode().getTotalSize()` gives that count.

In an SVG, all shapes are paths in module units, so the file stays sharp at any zoom. The gradient becomes a `linearGradient`, a translucent color gets a `fill-opacity`, and a transparent background leaves out the background path. An `Icon` can't be turned into vector paths, so the logo is drawn into a PNG at 24 pixels per module and embedded as a base64 image.

### 7. Without a component

`QrCode` holds the text, the style and the encoded modules. It's immutable and doesn't need Swing to be showing, so it works headless, on a server or in a batch job:

```java
QrCode code = QrCode.builder("https://example.com")
        .errorCorrection(ErrorCorrectionLevel.H)
        .bodyShape(BodyStyle.FLUID)
        .eyeFrameShape(EyeFrameStyle.ROUNDED)
        .bodyColor(new Color(0x0369a1))
        .gradientColor(new Color(0x0891b2))
        .logo(icon)
        .build();

BufferedImage image = code.toImage(1024);  // ARGB image
String svg = code.toSvg(1024);             // SVG document
code.paint(g2, x, y, size);                // into any Graphics2D: your own image, a print job, a PDF
```

The builder has one method per setting, named like the view's setters without `set` (`background` is the view's `codeBackground`), with the same defaults. `build()` only throws for the unsupported eye pair (see Shapes). If the text is empty or too long, `getMatrix()` returns `null`, `paint` draws nothing, and `toImage` and `toSvg` throw an `IllegalStateException`.

To change a code, copy it with `toBuilder()`. Encoding is the expensive part, so the copy keeps the encoded modules unless you change the text or the error correction:

```java
QrCode dots = code.toBuilder().bodyShape(BodyStyle.DOTS).build();  // same modules, not encoded again
```

A view shows a `QrCode`. Its setters rebuild the code with one setting changed, `getCode()` returns the current one, and `setCode` or the `QrCodeView(QrCode)` constructor shows a code you built:

```java
QrCodeView view = new QrCodeView(code);
view.setCode(code.toBuilder().bodyColor(Color.BLACK).build());
```

`paint` sets antialiasing on its own copy of the graphics and leaves yours unchanged. The view also snaps modules to whole device pixels, but `paint` draws exactly the square you give it.

### 8. Custom shapes

A body shape returns the shape of one dark module. Coordinates are in modules: the module at `(x, y)` covers `x..x+1`, `y..y+1`. Read the neighbors from the matrix to join modules:

```java
// small dots with more space between them
BodyShape dots = (matrix, x, y) -> new Ellipse2D.Double(x + 0.15, y + 0.15, 0.7, 0.7);
qr.setBodyShape(dots);

// bars joined across each row, like HORIZONTAL but square
BodyShape bars = (matrix, x, y) -> new Rectangle2D.Double(x, y + 0.15, 1, 0.7);
```

Override `getGapShape` to paint something in a light module too, like `FLUID` does for its inner corners. It returns `null` by default.

An eye shape gets its bounds (7 × 7 for a frame, 3 × 3 for a ball) and which corner the eye is in:

```java
EyeShape frame = (b, corner) -> Shapes.ring(
        Shapes.roundRect(b.getX(), b.getY(), b.getWidth(), b.getHeight(), 1),
        new Rectangle2D.Double(b.getX() + 1, b.getY() + 1, b.getWidth() - 2, b.getHeight() - 2));
qr.setEyeFrameShape(frame);
```

A frame must be a one-module ring, and a ball should cover about the same area as its 3 × 3 square. Scanners find the code by the eyes, so keep them close to the standard proportions. `Shapes` has helpers for rounded rectangles with a radius per corner, leaves, diamonds, octagons, hexagons, stars, polygons, rings and fillets.

## How it works

`QrCode.Builder.build()` calls `QrMatrix.encode`, which runs ZXing's `Encoder` and copies the modules into a grid. The quiet zone isn't part of the grid.

Drawing works in module units, with one module as one unit. `QrCode` builds a list of filled shapes, in order:

1. The background square with its rounded corners, unless the background is `null`.
2. The body. The code makes a copy of the grid with the three eyes and the logo's hole cleared, so body shapes never join them. Then it asks the body shape for each dark module, and the gap shape for each light one, and appends them all to one path. Touching modules are one fill, so there are no antialiasing seams between them.
3. Each eye's frame and ball.

`paint` and `toImage` scale the graphics to the target size, fill those shapes, then draw the logo scaled to its square. `toSvg` writes the same shapes as `<path>` elements inside a group scaled to the target size, then embeds the logo as a PNG. So an image and an SVG of the same code match.

The gradient runs in module units from the top-left to the bottom-right of the code, so it's the same at any size. `QrCodeView` only works out a size and position with whole-pixel modules, and calls `paint`.

## Key classes

| Package | Class | Role |
|---|---|---|
| `qrcode` | `QrCode` | Public API: an immutable styled code. Paints it, and exports it as an image or SVG. |
| `qrcode` | `QrCodeView` | Public API: the component. Shows a `QrCode`, with a setter for each setting. |
| `qrcode` | `QrMatrix` | The encoded modules, from ZXing. |
| `qrcode.shape` | `BodyShape` | Shape of a body module, and optionally of the gap in a light module. |
| `qrcode.shape` | `EyeShape` | Shape of an eye frame or eye ball. |
| `qrcode.shape` | `BodyStyle` | Built-in body shapes. |
| `qrcode.shape` | `EyeFrameStyle` | Built-in eye frames. |
| `qrcode.shape` | `EyeBallStyle` | Built-in eye balls. |
| `qrcode.shape` | `Corner` | Which corner an eye is in. |
| `qrcode.shape` | `Shapes` | Helpers for building shapes. |

All packages are under `com.swingcraft4j`.
