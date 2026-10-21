package com.swingcraft4j.login;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

/**
 * Small line icons drawn with Java 2D, so they stay sharp at any scale and take any color.
 */
public class GlyphIcon implements Icon {

    public enum Kind {
        USER, LOCK, CHECK, SUN, MOON, SPINNER
    }

    private final Kind kind;
    private final int size;
    private Color color;
    // the turn of the spinner in degrees
    private double angle;

    public GlyphIcon(Kind kind, int size, Color color) {
        this.kind = kind;
        this.size = size;
        this.color = color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Turns the spinner one frame forward.
     */
    public void step() {
        angle = (angle + 10) % 360;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.scale(size / 16.0, size / 16.0);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (kind) {
                case USER:
                    g2.draw(new Ellipse2D.Double(5, 1.5, 6, 6));
                    g2.draw(new Arc2D.Double(2.25, 9.5, 11.5, 10, 0, 180, Arc2D.OPEN));
                    break;
                case LOCK:
                    g2.draw(new RoundRectangle2D.Double(2.75, 7, 10.5, 7.75, 3, 3));
                    g2.draw(new Arc2D.Double(5, 2, 6, 7, 0, 180, Arc2D.OPEN));
                    g2.draw(new Line2D.Double(5, 5.5, 5, 7));
                    g2.draw(new Line2D.Double(11, 5.5, 11, 7));
                    g2.fill(new Ellipse2D.Double(7.1, 9.9, 1.8, 1.8));
                    break;
                case CHECK:
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Path2D.Double check = new Path2D.Double();
                    check.moveTo(3.5, 8.5);
                    check.lineTo(6.5, 11.5);
                    check.lineTo(12.5, 4.5);
                    g2.draw(check);
                    break;
                case SUN:
                    g2.fill(new Ellipse2D.Double(5, 5, 6, 6));
                    for (int i = 0; i < 8; i++) {
                        double a = Math.PI / 4 * i;
                        g2.draw(new Line2D.Double(8 + Math.cos(a) * 5.2, 8 + Math.sin(a) * 5.2,
                                8 + Math.cos(a) * 6.8, 8 + Math.sin(a) * 6.8));
                    }
                    break;
                case MOON:
                    Area moon = new Area(new Ellipse2D.Double(2, 2, 12, 12));
                    moon.subtract(new Area(new Ellipse2D.Double(6, 0, 11, 11)));
                    g2.fill(moon);
                    break;
                case SPINNER:
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
                    g2.draw(new Ellipse2D.Double(2, 2, 12, 12));
                    g2.setColor(color);
                    g2.draw(new Arc2D.Double(2, 2, 12, 12, -angle, 100, Arc2D.OPEN));
                    break;
                default:
                    break;
            }
        } finally {
            g2.dispose();
        }
    }
}
