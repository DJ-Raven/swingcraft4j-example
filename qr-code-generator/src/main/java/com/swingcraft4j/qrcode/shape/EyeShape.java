package com.swingcraft4j.qrcode.shape;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Shape of an eye part, in module units: the 7 × 7 frame ring or the 3 × 3 ball inside it.
 */
@FunctionalInterface
public interface EyeShape {

    Shape getShape(Rectangle2D bounds, Corner corner);
}
