package com.swingcraft4j.qrcode.shape;

import com.swingcraft4j.qrcode.QrMatrix;

import java.awt.*;

/**
 * Shape of the body modules, in module units: the module at (x, y) is the square x..x+1, y..y+1.
 */
@FunctionalInterface
public interface BodyShape {

    /**
     * Shape of the dark module at (x, y); the neighbors can be read from the matrix to join modules.
     */
    Shape getShape(QrMatrix matrix, int x, int y);

    /**
     * Optional shape painted in the light module at (x, y), such as a fillet between dark neighbors; {@code null} for none.
     */
    default Shape getGapShape(QrMatrix matrix, int x, int y) {
        return null;
    }
}
