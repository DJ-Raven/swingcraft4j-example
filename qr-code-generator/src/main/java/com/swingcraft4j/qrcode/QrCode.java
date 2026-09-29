package com.swingcraft4j.qrcode;

import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.swingcraft4j.qrcode.shape.BodyShape;
import com.swingcraft4j.qrcode.shape.BodyStyle;
import com.swingcraft4j.qrcode.shape.Corner;
import com.swingcraft4j.qrcode.shape.EyeBallStyle;
import com.swingcraft4j.qrcode.shape.EyeFrameStyle;
import com.swingcraft4j.qrcode.shape.EyeShape;
import com.swingcraft4j.qrcode.shape.Shapes;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable styled QR code: the text, how it's drawn, and the encoded modules; paints itself or exports an image or SVG.
 */
public final class QrCode {

    // largest logo that the highest error correction can still recover from
    private static final float MAX_LOGO_SIZE = 0.3f;
    // logo pixels per module in SVG exports, so the embedded image stays sharp when zoomed
    private static final int SVG_LOGO_RESOLUTION = 24;

    private final String text;
    private final ErrorCorrectionLevel errorCorrection;
    private final QrMatrix matrix;
    private final BodyShape bodyShape;
    private final EyeShape eyeFrameShape;
    private final EyeShape eyeBallShape;
    private final Color background;
    private final Color bodyColor;
    private final Color gradientColor;
    private final Color eyeFrameColor;
    private final Color eyeBallColor;
    private final int margin;
    private final float backgroundRadius;
    private final Icon logo;
    private final float logoSize;
    private final int logoMargin;

    private QrCode(Builder b) {
        text = b.text;
        errorCorrection = b.errorCorrection;
        matrix = b.matrix;
        bodyShape = b.bodyShape;
        eyeFrameShape = b.eyeFrameShape;
        eyeBallShape = b.eyeBallShape;
        background = b.background;
        bodyColor = b.bodyColor;
        gradientColor = b.gradientColor;
        eyeFrameColor = b.eyeFrameColor;
        eyeBallColor = b.eyeBallColor;
        margin = b.margin;
        backgroundRadius = b.backgroundRadius;
        logo = b.logo;
        logoSize = b.logoSize;
        logoMargin = b.logoMargin;
    }

    public static Builder builder(String text) {
        return new Builder().text(text);
    }

    /**
     * Builder starting from this code's settings; the encoded modules are reused unless the text or error correction change.
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    public String getText() {
        return text;
    }

    public ErrorCorrectionLevel getErrorCorrection() {
        return errorCorrection;
    }

    /**
     * The encoded modules, or {@code null} when the text is empty or too long; then nothing can be drawn.
     */
    public QrMatrix getMatrix() {
        return matrix;
    }

    public BodyShape getBodyShape() {
        return bodyShape;
    }

    public EyeShape getEyeFrameShape() {
        return eyeFrameShape;
    }

    public EyeShape getEyeBallShape() {
        return eyeBallShape;
    }

    public Color getBackground() {
        return background;
    }

    public Color getBodyColor() {
        return bodyColor;
    }

    public Color getGradientColor() {
        return gradientColor;
    }

    public Color getEyeFrameColor() {
        return eyeFrameColor;
    }

    public Color getEyeBallColor() {
        return eyeBallColor;
    }

    public int getMargin() {
        return margin;
    }

    public float getBackgroundRadius() {
        return backgroundRadius;
    }

    public Icon getLogo() {
        return logo;
    }

    public float getLogoSize() {
        return logoSize;
    }

    public int getLogoMargin() {
        return logoMargin;
    }

    /**
     * Modules per side including the quiet zone on both sides, or 0 when there's nothing to draw.
     */
    public int getTotalSize() {
        return matrix == null ? 0 : matrix.getSize() + margin * 2;
    }

    /**
     * Whether the eye frame and ball can be used together; the dotted frame with the star ball can't be read by scanners.
     */
    public static boolean isSupported(EyeShape eyeFrameShape, EyeShape eyeBallShape) {
        return !(eyeFrameShape == EyeFrameStyle.DOTTED && eyeBallShape == EyeBallStyle.STAR);
    }

    /**
     * Paints the code into the square at (x, y), quiet zone included; does nothing when there's nothing to draw.
     */
    public void paint(Graphics2D g2, double x, double y, double size) {
        if (matrix == null) {
            return;
        }
        double unit = size / getTotalSize();
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.translate(x + margin * unit, y + margin * unit);
        g.scale(unit, unit);

        Paint bodyPaint = bodyPaint();
        for (Layer layer : layers()) {
            g.setPaint(layer.color() != null ? layer.color() : bodyPaint);
            g.fill(layer.shape());
        }
        int cells = logoCells();
        if (cells > 0) {
            paintLogo(g, (matrix.getSize() - cells) / 2.0, cells);
        }
        g.dispose();
    }

    /**
     * The code drawn into a new square ARGB image; transparent outside the modules when there's no background.
     */
    public BufferedImage toImage(int size) {
        requireMatrix();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        paint(g, 0, 0, size);
        g.dispose();
        return image;
    }

    /**
     * The code as an SVG document of the given width and height; shapes stay vector, the logo is embedded as a PNG.
     */
    public String toSvg(int size) {
        requireMatrix();
        int n = matrix.getSize();
        double unit = size / (double) getTotalSize();
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"")
                .append(" width=\"").append(size).append("\" height=\"").append(size)
                .append("\" viewBox=\"0 0 ").append(size).append(' ').append(size).append("\">\n");
        // everything below is in module units, like the shapes, with the code's top left at 0, 0
        svg.append("  <g transform=\"translate(").append(num(margin * unit)).append(' ').append(num(margin * unit))
                .append(") scale(").append(num(unit)).append(")\">\n");
        if (gradientColor != null) {
            svg.append("    <defs>\n      <linearGradient id=\"body\" gradientUnits=\"userSpaceOnUse\" x1=\"0\" y1=\"0\" x2=\"")
                    .append(n).append("\" y2=\"").append(n).append("\">\n")
                    .append("        <stop offset=\"0\" ").append(color("stop-color", "stop-opacity", bodyColor)).append("/>\n")
                    .append("        <stop offset=\"1\" ").append(color("stop-color", "stop-opacity", gradientColor)).append("/>\n")
                    .append("      </linearGradient>\n    </defs>\n");
        }
        for (Layer layer : layers()) {
            svg.append("    <path ");
            if (layer.color() != null) {
                svg.append(color("fill", "fill-opacity", layer.color()));
            } else if (gradientColor != null) {
                svg.append("fill=\"url(#body)\"");
            } else {
                svg.append(color("fill", "fill-opacity", bodyColor));
            }
            if (layer.evenOdd()) {
                svg.append(" fill-rule=\"evenodd\"");
            }
            svg.append(" d=\"").append(pathData(layer.shape())).append("\"/>\n");
        }
        int cells = logoCells();
        if (cells > 0) {
            svg.append(logoImage((n - cells) / 2.0, cells));
        }
        svg.append("  </g>\n</svg>\n");
        return svg.toString();
    }

    /**
     * Everything filled, in painting order and module units: background, body, then each eye's frame and ball.
     */
    private List<Layer> layers() {
        int n = matrix.getSize();
        int eye = QrMatrix.EYE_SIZE;
        List<Layer> layers = new ArrayList<>();
        if (background != null) {
            double side = n + margin * 2;
            layers.add(new Layer(Shapes.roundRect(-margin, -margin, side, side, Math.min(backgroundRadius, side / 2)), background, false));
        }

        // eyes and the logo's hole are cleared, so body shapes never join them
        QrMatrix body = matrix.clear(0, 0, eye, eye).clear(n - eye, 0, eye, eye).clear(0, n - eye, eye, eye);
        int cells = logoCells();
        if (cells > 0) {
            int hole = cells + logoMargin * 2;
            int start = (n - hole) / 2;
            body = body.clear(start, start, hole, hole);
        }
        // one path for the whole body, so touching modules have no antialiasing seams
        Path2D path = new Path2D.Double();
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                if (matrix.isEye(col, row)) {
                    continue;
                }
                Shape shape = body.isDark(col, row) ? bodyShape.getShape(body, col, row) : bodyShape.getGapShape(body, col, row);
                if (shape != null) {
                    path.append(shape, false);
                }
            }
        }
        layers.add(new Layer(path, null, false));

        addEye(layers, 0, 0, Corner.TOP_LEFT);
        addEye(layers, n - eye, 0, Corner.TOP_RIGHT);
        addEye(layers, 0, n - eye, Corner.BOTTOM_LEFT);
        return layers;
    }

    private void addEye(List<Layer> layers, int x, int y, Corner corner) {
        int eye = QrMatrix.EYE_SIZE;
        Shape frame = eyeFrameShape.getShape(new Rectangle2D.Double(x, y, eye, eye), corner);
        Shape ball = eyeBallShape.getShape(new Rectangle2D.Double(x + 2, y + 2, eye - 4, eye - 4), corner);
        layers.add(new Layer(frame, eyeFrameColor, evenOdd(frame)));
        layers.add(new Layer(ball, eyeBallColor, evenOdd(ball)));
    }

    private static boolean evenOdd(Shape shape) {
        return shape.getPathIterator(null).getWindingRule() == PathIterator.WIND_EVEN_ODD;
    }

    /**
     * A filled shape; a {@code null} color takes the body color or gradient.
     */
    private record Layer(Shape shape, Color color, boolean evenOdd) {
    }

    private Paint bodyPaint() {
        int n = matrix.getSize();
        return gradientColor == null ? bodyColor : new GradientPaint(0, 0, bodyColor, n, n, gradientColor);
    }

    /**
     * Logo scaled to fit a square of {@code cells} modules at (start, start), keeping its aspect ratio.
     */
    private void paintLogo(Graphics2D g, double start, int cells) {
        int iw = logo.getIconWidth();
        int ih = logo.getIconHeight();
        if (iw <= 0 || ih <= 0) {
            return;
        }
        double s = cells / (double) Math.max(iw, ih);
        Graphics2D lg = (Graphics2D) g.create();
        lg.translate(start + (cells - iw * s) / 2, start + (cells - ih * s) / 2);
        lg.scale(s, s);
        logo.paintIcon(null, lg, 0, 0);
        lg.dispose();
    }

    /**
     * The logo drawn into a PNG and embedded as an SVG image element, in module units.
     */
    private String logoImage(double start, int cells) {
        int pixels = cells * SVG_LOGO_RESOLUTION;
        BufferedImage image = new BufferedImage(pixels, pixels, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.scale(SVG_LOGO_RESOLUTION, SVG_LOGO_RESOLUTION);
        paintLogo(g, 0, cells);
        g.dispose();

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", png);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return "    <image x=\"" + num(start) + "\" y=\"" + num(start) + "\" width=\"" + cells + "\" height=\"" + cells
                + "\" xlink:href=\"data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray()) + "\"/>\n";
    }

    /**
     * Logo side in modules, with the same parity as the code so it's centered on the grid; 0 without a logo.
     */
    private int logoCells() {
        if (logo == null || logoSize <= 0) {
            return 0;
        }
        int n = matrix.getSize();
        int cells = (int) Math.round(n * logoSize);
        if (cells > 0 && (cells & 1) != (n & 1)) {
            // round down, so the logo never covers more than asked
            cells--;
        }
        return cells;
    }

    private void requireMatrix() {
        if (matrix == null) {
            throw new IllegalStateException(text == null || text.isEmpty()
                    ? "No text to encode"
                    : "Text is too long for a QR code at error correction " + errorCorrection);
        }
    }

    /**
     * SVG path data of a shape: M, L, Q, C and Z commands.
     */
    private static String pathData(Shape shape) {
        StringBuilder d = new StringBuilder();
        double[] c = new double[6];
        for (PathIterator it = shape.getPathIterator(null); !it.isDone(); it.next()) {
            switch (it.currentSegment(c)) {
                case PathIterator.SEG_MOVETO -> d.append('M').append(num(c[0])).append(' ').append(num(c[1]));
                case PathIterator.SEG_LINETO -> d.append('L').append(num(c[0])).append(' ').append(num(c[1]));
                case PathIterator.SEG_QUADTO -> d.append('Q').append(num(c[0])).append(' ').append(num(c[1]))
                        .append(' ').append(num(c[2])).append(' ').append(num(c[3]));
                case PathIterator.SEG_CUBICTO -> d.append('C').append(num(c[0])).append(' ').append(num(c[1]))
                        .append(' ').append(num(c[2])).append(' ').append(num(c[3]))
                        .append(' ').append(num(c[4])).append(' ').append(num(c[5]));
                case PathIterator.SEG_CLOSE -> d.append('Z');
                default -> throw new IllegalStateException();
            }
        }
        return d.toString();
    }

    /**
     * SVG color attribute, with an opacity attribute only when the color is translucent.
     */
    private static String color(String attribute, String opacityAttribute, Color color) {
        String value = attribute + "=\"" + String.format("#%06x", color.getRGB() & 0xffffff) + "\"";
        return color.getAlpha() == 255 ? value : value + " " + opacityAttribute + "=\"" + num(color.getAlpha() / 255.0) + "\"";
    }

    /**
     * Number with up to 4 decimals and no trailing zeros, independent of the default locale.
     */
    private static String num(double value) {
        String s = String.format(Locale.ROOT, "%.4f", value);
        s = s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
        return s.equals("-0") ? "0" : s;
    }

    /**
     * Collects a code's settings; {@link #build()} encodes the text only when it or the error correction changed.
     */
    public static final class Builder {

        private String text;
        private ErrorCorrectionLevel errorCorrection = ErrorCorrectionLevel.M;
        private QrMatrix matrix;
        private boolean encoded;
        private BodyShape bodyShape = BodyStyle.SQUARE;
        private EyeShape eyeFrameShape = EyeFrameStyle.SQUARE;
        private EyeShape eyeBallShape = EyeBallStyle.SQUARE;
        private Color background = Color.WHITE;
        private Color bodyColor = Color.BLACK;
        private Color gradientColor;
        private Color eyeFrameColor;
        private Color eyeBallColor;
        private int margin = 2;
        private float backgroundRadius;
        private Icon logo;
        private float logoSize = 0.22f;
        private int logoMargin = 1;

        private Builder() {
        }

        private Builder(QrCode code) {
            text = code.text;
            errorCorrection = code.errorCorrection;
            matrix = code.matrix;
            encoded = true;
            bodyShape = code.bodyShape;
            eyeFrameShape = code.eyeFrameShape;
            eyeBallShape = code.eyeBallShape;
            background = code.background;
            bodyColor = code.bodyColor;
            gradientColor = code.gradientColor;
            eyeFrameColor = code.eyeFrameColor;
            eyeBallColor = code.eyeBallColor;
            margin = code.margin;
            backgroundRadius = code.backgroundRadius;
            logo = code.logo;
            logoSize = code.logoSize;
            logoMargin = code.logoMargin;
        }

        public Builder text(String text) {
            if (!Objects.equals(this.text, text)) {
                this.text = text;
                encoded = false;
            }
            return this;
        }

        /**
         * How much of the code can be damaged or covered and still read; use {@code H} with a logo.
         */
        public Builder errorCorrection(ErrorCorrectionLevel errorCorrection) {
            Objects.requireNonNull(errorCorrection);
            if (this.errorCorrection != errorCorrection) {
                this.errorCorrection = errorCorrection;
                encoded = false;
            }
            return this;
        }

        public Builder bodyShape(BodyShape bodyShape) {
            this.bodyShape = Objects.requireNonNull(bodyShape);
            return this;
        }

        public Builder eyeFrameShape(EyeShape eyeFrameShape) {
            this.eyeFrameShape = Objects.requireNonNull(eyeFrameShape);
            return this;
        }

        public Builder eyeBallShape(EyeShape eyeBallShape) {
            this.eyeBallShape = Objects.requireNonNull(eyeBallShape);
            return this;
        }

        /**
         * Color of the code's square, quiet zone included; {@code null} leaves it transparent.
         */
        public Builder background(Color background) {
            this.background = background;
            return this;
        }

        public Builder bodyColor(Color bodyColor) {
            this.bodyColor = Objects.requireNonNull(bodyColor);
            return this;
        }

        /**
         * End color of a diagonal gradient from the body color; {@code null} for a solid body.
         */
        public Builder gradientColor(Color gradientColor) {
            this.gradientColor = gradientColor;
            return this;
        }

        /**
         * Color of the eye frames; {@code null} paints them like the body.
         */
        public Builder eyeFrameColor(Color eyeFrameColor) {
            this.eyeFrameColor = eyeFrameColor;
            return this;
        }

        /**
         * Color of the eye balls; {@code null} paints them like the body.
         */
        public Builder eyeBallColor(Color eyeBallColor) {
            this.eyeBallColor = eyeBallColor;
            return this;
        }

        /**
         * Quiet zone around the code, in modules; the QR spec asks for 4, most scanners are fine with 2.
         */
        public Builder margin(int margin) {
            this.margin = Math.max(0, margin);
            return this;
        }

        /**
         * Corner radius of the background, in modules.
         */
        public Builder backgroundRadius(float backgroundRadius) {
            this.backgroundRadius = Math.max(0, backgroundRadius);
            return this;
        }

        /**
         * Icon drawn in the center, scaled to fit; the modules behind it are cleared.
         */
        public Builder logo(Icon logo) {
            this.logo = logo;
            return this;
        }

        /**
         * Logo width as a fraction of the code's width, up to 0.3.
         */
        public Builder logoSize(float logoSize) {
            this.logoSize = Math.clamp(logoSize, 0, MAX_LOGO_SIZE);
            return this;
        }

        /**
         * Modules cleared around the logo.
         */
        public Builder logoMargin(int logoMargin) {
            this.logoMargin = Math.max(0, logoMargin);
            return this;
        }

        /**
         * Encodes the text if needed; if it's empty or too long, the code's matrix is {@code null}.
         *
         * @throws IllegalArgumentException for an eye frame and ball pair that scanners can't read
         */
        public QrCode build() {
            if (!isSupported(eyeFrameShape, eyeBallShape)) {
                throw new IllegalArgumentException("The " + eyeFrameShape + " eye frame with the " + eyeBallShape + " eye ball doesn't scan");
            }
            if (!encoded) {
                try {
                    matrix = text == null || text.isEmpty() ? null : QrMatrix.encode(text, errorCorrection);
                } catch (WriterException | IllegalArgumentException e) {
                    // too long for the largest QR version at this error correction
                    matrix = null;
                }
                encoded = true;
            }
            return new QrCode(this);
        }
    }
}
