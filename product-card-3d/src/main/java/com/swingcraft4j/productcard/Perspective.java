package com.swingcraft4j.productcard;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Draws a premultiplied ARGB image onto any quad with a perspective (projective) mapping.
 */
final class Perspective {

    private Perspective() {
    }

    /**
     * Blends {@code src} over {@code dst} mapped onto the quad {@code q} (TL, TR, BR, BL as x,y pairs) with an opacity of 0-256.
     */
    static void warp(int[] dst, int dw, int dh, BufferedImage src, double[] q, int opacity) {
        int sw = src.getWidth();
        int sh = src.getHeight();
        int[] sp = ((DataBufferInt) src.getRaster().getDataBuffer()).getData();
        if (isPlainCopy(q, sw, sh)) {
            copy(dst, dw, dh, sp, sw, sh, (int) Math.round(q[0]), (int) Math.round(q[1]), opacity);
            return;
        }
        double[] m = invert(squareToQuad(q));
        if (m == null) {
            return;
        }
        // the homography is defined up to a factor: keep w positive inside the quad
        double mx = (q[0] + q[2] + q[4] + q[6]) / 4;
        double my = (q[1] + q[3] + q[5] + q[7]) / 4;
        if (m[6] * mx + m[7] * my + m[8] < 0) {
            for (int i = 0; i < 9; i++) {
                m[i] = -m[i];
            }
        }
        int minX = Math.max(0, (int) Math.floor(min(q[0], q[2], q[4], q[6])) - 1);
        int maxX = Math.min(dw, (int) Math.ceil(max(q[0], q[2], q[4], q[6])) + 1);
        int minY = Math.max(0, (int) Math.floor(min(q[1], q[3], q[5], q[7])) - 1);
        int maxY = Math.min(dh, (int) Math.ceil(max(q[1], q[3], q[5], q[7])) + 1);
        for (int y = minY; y < maxY; y++) {
            double py = y + 0.5;
            double nu = m[0] * (minX + 0.5) + m[1] * py + m[2];
            double nv = m[3] * (minX + 0.5) + m[4] * py + m[5];
            double nw = m[6] * (minX + 0.5) + m[7] * py + m[8];
            int row = y * dw;
            for (int x = minX; x < maxX; x++, nu += m[0], nv += m[3], nw += m[6]) {
                if (nw <= 0) {
                    continue;
                }
                double u = nu / nw * sw - 0.5;
                double v = nv / nw * sh - 0.5;
                if (u <= -1 || v <= -1 || u >= sw || v >= sh) {
                    continue;
                }
                int x0 = (int) Math.floor(u);
                int y0 = (int) Math.floor(v);
                int fx = (int) ((u - x0) * 256);
                int fy = (int) ((v - y0) * 256);
                int c00 = texel(sp, sw, sh, x0, y0);
                int c10 = texel(sp, sw, sh, x0 + 1, y0);
                int c01 = texel(sp, sw, sh, x0, y0 + 1);
                int c11 = texel(sp, sw, sh, x0 + 1, y0 + 1);
                if ((c00 | c10 | c01 | c11) == 0) {
                    continue;
                }
                int s = 0;
                for (int shift = 0; shift < 32; shift += 8) {
                    int top = ((c00 >>> shift) & 0xff) * (256 - fx) + ((c10 >>> shift) & 0xff) * fx;
                    int bottom = ((c01 >>> shift) & 0xff) * (256 - fx) + ((c11 >>> shift) & 0xff) * fx;
                    int value = (top * (256 - fy) + bottom * fy) >>> 16;
                    s |= (value * opacity >>> 8) << shift;
                }
                dst[row + x] = over(s, dst[row + x]);
            }
        }
    }

    private static boolean isPlainCopy(double[] q, int sw, int sh) {
        double e = 1e-6;
        return Math.abs(q[1] - q[3]) < e && Math.abs(q[5] - q[7]) < e
                && Math.abs(q[0] - q[6]) < e && Math.abs(q[2] - q[4]) < e
                && Math.abs(q[2] - q[0] - sw) < e && Math.abs(q[7] - q[1] - sh) < e;
    }

    private static void copy(int[] dst, int dw, int dh, int[] sp, int sw, int sh, int ox, int oy, int opacity) {
        for (int y = Math.max(0, -oy); y < sh && y + oy < dh; y++) {
            for (int x = Math.max(0, -ox); x < sw && x + ox < dw; x++) {
                int s = sp[y * sw + x];
                if (s == 0) {
                    continue;
                }
                if (opacity < 256) {
                    s = scale(s, opacity);
                }
                int i = (y + oy) * dw + x + ox;
                dst[i] = over(s, dst[i]);
            }
        }
    }

    private static int texel(int[] sp, int sw, int sh, int x, int y) {
        return x < 0 || y < 0 || x >= sw || y >= sh ? 0 : sp[y * sw + x];
    }

    private static int scale(int c, int opacity) {
        int r = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            r |= (((c >>> shift) & 0xff) * opacity >>> 8) << shift;
        }
        return r;
    }

    /** Premultiplied source-over. */
    private static int over(int s, int d) {
        int sa = s >>> 24;
        if (sa == 255 || d == 0) {
            return s;
        }
        int inv = 255 - sa;
        int r = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int value = ((s >>> shift) & 0xff) + (((d >>> shift) & 0xff) * inv + 127) / 255;
            r |= Math.min(255, value) << shift;
        }
        return r;
    }

    /** Homography mapping the unit square onto the quad (Heckbert). */
    private static double[] squareToQuad(double[] q) {
        double x0 = q[0], y0 = q[1], x1 = q[2], y1 = q[3], x2 = q[4], y2 = q[5], x3 = q[6], y3 = q[7];
        double dx1 = x1 - x2, dx2 = x3 - x2, dx3 = x0 - x1 + x2 - x3;
        double dy1 = y1 - y2, dy2 = y3 - y2, dy3 = y0 - y1 + y2 - y3;
        double g = 0, h = 0;
        if (Math.abs(dx3) > 1e-9 || Math.abs(dy3) > 1e-9) {
            double det = dx1 * dy2 - dx2 * dy1;
            if (Math.abs(det) < 1e-12) {
                return null;
            }
            g = (dx3 * dy2 - dx2 * dy3) / det;
            h = (dx1 * dy3 - dx3 * dy1) / det;
        }
        return new double[]{
                x1 - x0 + g * x1, x3 - x0 + h * x3, x0,
                y1 - y0 + g * y1, y3 - y0 + h * y3, y0,
                g, h, 1};
    }

    private static double[] invert(double[] m) {
        if (m == null) {
            return null;
        }
        double a = m[0], b = m[1], c = m[2], d = m[3], e = m[4], f = m[5], g = m[6], h = m[7], i = m[8];
        double A = e * i - f * h, B = c * h - b * i, C = b * f - c * e;
        double D = f * g - d * i, E = a * i - c * g, F = c * d - a * f;
        double G = d * h - e * g, H = b * g - a * h, I = a * e - b * d;
        double det = a * A + b * D + c * G;
        if (Math.abs(det) < 1e-12) {
            return null;
        }
        return new double[]{A / det, B / det, C / det, D / det, E / det, F / det, G / det, H / det, I / det};
    }

    private static double min(double a, double b, double c, double d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    private static double max(double a, double b, double c, double d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }
}
