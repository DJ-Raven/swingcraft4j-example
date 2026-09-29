package com.swingcraft4j.qrcode;

import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Immutable grid of QR code modules, without the quiet zone.
 */
public final class QrMatrix {

    /**
     * Side of a finder pattern (eye), in modules.
     */
    public static final int EYE_SIZE = 7;

    private final int size;
    private final int version;
    private final boolean[] dark;

    private QrMatrix(int size, int version, boolean[] dark) {
        this.size = size;
        this.version = version;
        this.dark = dark;
    }

    /**
     * Encodes the text with ZXing; UTF-8 is only declared when the text needs it, so plain text stays widely readable.
     */
    public static QrMatrix encode(String text, ErrorCorrectionLevel level) throws WriterException {
        Map<EncodeHintType, ?> hints = StandardCharsets.ISO_8859_1.newEncoder().canEncode(text)
                ? Map.of()
                : Map.of(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        QRCode code = Encoder.encode(text, level, hints);
        ByteMatrix matrix = code.getMatrix();
        int size = matrix.getWidth();
        boolean[] dark = new boolean[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                dark[y * size + x] = matrix.get(x, y) == 1;
            }
        }
        return new QrMatrix(size, code.getVersion().getVersionNumber(), dark);
    }

    /**
     * Modules per side.
     */
    public int getSize() {
        return size;
    }

    /**
     * QR version, from 1 (21 × 21) to 40 (177 × 177).
     */
    public int getVersion() {
        return version;
    }

    /**
     * Whether the module is dark; anything outside the grid is light.
     */
    public boolean isDark(int x, int y) {
        return x >= 0 && y >= 0 && x < size && y < size && dark[y * size + x];
    }

    /**
     * Whether the module is part of one of the three finder patterns.
     */
    public boolean isEye(int x, int y) {
        int far = size - EYE_SIZE;
        return (x < EYE_SIZE && y < EYE_SIZE) || (x >= far && y < EYE_SIZE) || (x < EYE_SIZE && y >= far);
    }

    /**
     * Copy with the modules in the given rectangle made light.
     */
    public QrMatrix clear(int x, int y, int width, int height) {
        boolean[] copy = dark.clone();
        for (int row = Math.max(0, y); row < Math.min(size, y + height); row++) {
            for (int col = Math.max(0, x); col < Math.min(size, x + width); col++) {
                copy[row * size + col] = false;
            }
        }
        return new QrMatrix(size, version, copy);
    }
}
