package com.swingcraft4j.qrcode;

import com.formdev.flatlaf.util.UIScale;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.swingcraft4j.qrcode.shape.BodyShape;
import com.swingcraft4j.qrcode.shape.EyeShape;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Component that shows a {@link QrCode}, with setters for each of its settings.
 */
public class QrCodeView extends JComponent {

    private static final int DEFAULT_SIZE = 200;

    private QrCode code;

    public QrCodeView() {
        this((String) null);
    }

    public QrCodeView(String text) {
        this(QrCode.builder(text).build());
    }

    public QrCodeView(QrCode code) {
        this.code = Objects.requireNonNull(code);
    }

    /**
     * The code shown, with every setting; use it to export or to copy the style to another view.
     */
    public QrCode getCode() {
        return code;
    }

    /**
     * Shows another code, replacing every setting at once.
     */
    public void setCode(QrCode code) {
        QrMatrix old = this.code.getMatrix();
        this.code = Objects.requireNonNull(code);
        firePropertyChange("matrix", old, code.getMatrix());
        repaint();
    }

    /**
     * Rebuilds the code with one setting changed; encoding only runs again for new text or error correction.
     */
    private void update(UnaryOperator<QrCode.Builder> change) {
        setCode(change.apply(code.toBuilder()).build());
    }

    public String getText() {
        return code.getText();
    }

    /**
     * Encodes the text; if it's empty or too long, nothing is drawn and {@link #getMatrix()} is {@code null}.
     */
    public void setText(String text) {
        update(b -> b.text(text));
    }

    public ErrorCorrectionLevel getErrorCorrection() {
        return code.getErrorCorrection();
    }

    /**
     * How much of the code can be damaged or covered and still read; use {@code H} with a logo.
     */
    public void setErrorCorrection(ErrorCorrectionLevel errorCorrection) {
        update(b -> b.errorCorrection(errorCorrection));
    }

    /**
     * The encoded modules, or {@code null} when there's nothing to draw.
     */
    public QrMatrix getMatrix() {
        return code.getMatrix();
    }

    public BodyShape getBodyShape() {
        return code.getBodyShape();
    }

    public void setBodyShape(BodyShape bodyShape) {
        update(b -> b.bodyShape(bodyShape));
    }

    public EyeShape getEyeFrameShape() {
        return code.getEyeFrameShape();
    }

    /**
     * @throws IllegalArgumentException if it doesn't go with the eye ball, see {@link QrCode#isSupported}; use {@link #setCode} to change both
     */
    public void setEyeFrameShape(EyeShape eyeFrameShape) {
        update(b -> b.eyeFrameShape(eyeFrameShape));
    }

    public EyeShape getEyeBallShape() {
        return code.getEyeBallShape();
    }

    /**
     * @throws IllegalArgumentException if it doesn't go with the eye frame, see {@link QrCode#isSupported}; use {@link #setCode} to change both
     */
    public void setEyeBallShape(EyeShape eyeBallShape) {
        update(b -> b.eyeBallShape(eyeBallShape));
    }

    public Color getCodeBackground() {
        return code.getBackground();
    }

    /**
     * Color of the code's square, quiet zone included; {@code null} leaves it transparent.
     */
    public void setCodeBackground(Color codeBackground) {
        update(b -> b.background(codeBackground));
    }

    public Color getBodyColor() {
        return code.getBodyColor();
    }

    public void setBodyColor(Color bodyColor) {
        update(b -> b.bodyColor(bodyColor));
    }

    public Color getGradientColor() {
        return code.getGradientColor();
    }

    /**
     * End color of a diagonal gradient from the body color; {@code null} for a solid body.
     */
    public void setGradientColor(Color gradientColor) {
        update(b -> b.gradientColor(gradientColor));
    }

    public Color getEyeFrameColor() {
        return code.getEyeFrameColor();
    }

    /**
     * Color of the eye frames; {@code null} paints them like the body.
     */
    public void setEyeFrameColor(Color eyeFrameColor) {
        update(b -> b.eyeFrameColor(eyeFrameColor));
    }

    public Color getEyeBallColor() {
        return code.getEyeBallColor();
    }

    /**
     * Color of the eye balls; {@code null} paints them like the body.
     */
    public void setEyeBallColor(Color eyeBallColor) {
        update(b -> b.eyeBallColor(eyeBallColor));
    }

    public int getMargin() {
        return code.getMargin();
    }

    /**
     * Quiet zone around the code, in modules; the QR spec asks for 4, most scanners are fine with 2.
     */
    public void setMargin(int margin) {
        update(b -> b.margin(margin));
    }

    public float getBackgroundRadius() {
        return code.getBackgroundRadius();
    }

    /**
     * Corner radius of the background, in modules.
     */
    public void setBackgroundRadius(float backgroundRadius) {
        update(b -> b.backgroundRadius(backgroundRadius));
    }

    public Icon getLogo() {
        return code.getLogo();
    }

    /**
     * Icon drawn in the center, scaled to fit; the modules behind it are cleared.
     */
    public void setLogo(Icon logo) {
        update(b -> b.logo(logo));
    }

    public float getLogoSize() {
        return code.getLogoSize();
    }

    /**
     * Logo width as a fraction of the code's width, up to 0.3.
     */
    public void setLogoSize(float logoSize) {
        update(b -> b.logoSize(logoSize));
    }

    public int getLogoMargin() {
        return code.getLogoMargin();
    }

    /**
     * Modules cleared around the logo.
     */
    public void setLogoMargin(int logoMargin) {
        update(b -> b.logoMargin(logoMargin));
    }

    /**
     * The code drawn into a new square image; same as {@code getCode().toImage(size)}.
     */
    public BufferedImage createImage(int size) {
        return code.toImage(size);
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        Insets insets = getInsets();
        int size = UIScale.scale(DEFAULT_SIZE);
        return new Dimension(size + insets.left + insets.right, size + insets.top + insets.bottom);
    }

    @Override
    protected void paintComponent(Graphics g) {
        QrCode c = code;
        int total = c.getTotalSize();
        if (total == 0) {
            return;
        }
        Insets insets = getInsets();
        int width = getWidth() - insets.left - insets.right;
        int height = getHeight() - insets.top - insets.bottom;
        Graphics2D g2 = (Graphics2D) g;

        // snap modules to whole device pixels, so square edges stay crisp, unless that shrinks the code too much
        double scale = g2.getTransform().getScaleX();
        double exact = Math.min(width, height) / (double) total;
        double unit = Math.floor(exact * scale) / scale;
        if (unit < exact * 0.9) {
            unit = exact;
        }
        double size = unit * total;
        double x = Math.round((insets.left + (width - size) / 2) * scale) / scale;
        double y = Math.round((insets.top + (height - size) / 2) * scale) / scale;
        c.paint(g2, x, y, size);
    }
}
