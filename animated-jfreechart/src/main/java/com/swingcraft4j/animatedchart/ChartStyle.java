package com.swingcraft4j.animatedchart;

import com.formdev.flatlaf.FlatLaf;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Fonts, colors, grid and legend of a chart; whatever isn't set, or is set to {@code null}, follows the FlatLaf theme.
 */
public final class ChartStyle {

    // colors after the accent color, for the series that have none of their own
    private static final Color[] PALETTE = {new Color(0x14b8a6), new Color(0xf59e0b), new Color(0xf43f5e), new Color(0x8b5cf6)};

    private final List<Runnable> listeners = new ArrayList<>();
    private Font font;
    private Font titleFont;
    private HorizontalAlignment titleAlignment = HorizontalAlignment.CENTER;
    private Font axisFont;
    private Font legendFont;
    private Color background;
    private Color foreground;
    private Color mutedForeground;
    private Color accent;
    private Color gridColor;
    private Color popupBackground;
    private Color popupBorder;
    private Color positive;
    private Color negative;
    private Color[] palette;
    private float dimmedOpacity = 0.35f;
    private boolean gridVisible = true;
    private boolean legendVisible = true;
    private RectangleEdge legendPosition = RectangleEdge.TOP;

    public Font font() {
        return font != null ? font : UIManager.getFont("Label.font");
    }

    /**
     * Font of every text of the chart, unless the axes or the legend have their own.
     */
    public ChartStyle setFont(Font font) {
        this.font = font;
        return changed();
    }

    public Font boldFont() {
        return font().deriveFont(Font.BOLD);
    }

    /**
     * Font of a chart's title: by default the bold font, a quarter larger.
     */
    public Font titleFont() {
        return titleFont != null ? titleFont : boldFont().deriveFont(font().getSize2D() * 1.25f);
    }

    public ChartStyle setTitleFont(Font titleFont) {
        this.titleFont = titleFont;
        return changed();
    }

    public HorizontalAlignment titleAlignment() {
        return titleAlignment;
    }

    /**
     * Side of the chart its title and subtitle are on; the center by default.
     */
    public ChartStyle setTitleAlignment(HorizontalAlignment titleAlignment) {
        this.titleAlignment = titleAlignment;
        return changed();
    }

    public Font axisFont() {
        return axisFont != null ? axisFont : font();
    }

    public ChartStyle setAxisFont(Font axisFont) {
        this.axisFont = axisFont;
        return changed();
    }

    public Font legendFont() {
        return legendFont != null ? legendFont : font();
    }

    public ChartStyle setLegendFont(Font legendFont) {
        this.legendFont = legendFont;
        return changed();
    }

    public Color background() {
        return background != null ? background : UIManager.getColor("Panel.background");
    }

    /**
     * Fills the chart with a color; by default it is transparent, over the panel's background.
     */
    public ChartStyle setBackground(Color background) {
        this.background = background;
        return changed();
    }

    /**
     * The background to fill the chart with, or {@code null} if it is left transparent.
     */
    Color fill() {
        return background;
    }

    public Color foreground() {
        return foreground != null ? foreground : UIManager.getColor("Label.foreground");
    }

    public ChartStyle setForeground(Color foreground) {
        this.foreground = foreground;
        return changed();
    }

    public Color mutedForeground() {
        return mutedForeground != null ? mutedForeground : UIManager.getColor("Label.disabledForeground");
    }

    /**
     * Color of the secondary text: axis labels, names in the hover popup and labels beside the data.
     */
    public ChartStyle setMutedForeground(Color mutedForeground) {
        this.mutedForeground = mutedForeground;
        return changed();
    }

    public Color accent() {
        return accent != null ? accent : UIManager.getColor("Component.accentColor");
    }

    public ChartStyle setAccent(Color accent) {
        this.accent = accent;
        return changed();
    }

    /**
     * Faint line color for grid lines: by default the foreground blended into the background.
     */
    public Color gridColor() {
        return gridColor != null ? gridColor : mix(foreground(), background(), 0.12f);
    }

    public ChartStyle setGridColor(Color gridColor) {
        this.gridColor = gridColor;
        return changed();
    }

    public boolean gridVisible() {
        return gridVisible;
    }

    public ChartStyle setGridVisible(boolean gridVisible) {
        this.gridVisible = gridVisible;
        return changed();
    }

    /**
     * Background of the hover popup: by default white on light themes, a step lighter than the panel on dark ones.
     */
    public Color popupBackground() {
        if (popupBackground != null) {
            return popupBackground;
        }
        return dark() ? mix(foreground(), background(), 0.08f) : Color.WHITE;
    }

    public ChartStyle setPopupBackground(Color popupBackground) {
        this.popupBackground = popupBackground;
        return changed();
    }

    public Color popupBorder() {
        return popupBorder != null ? popupBorder : mix(foreground(), background(), dark() ? 0.2f : 0.14f);
    }

    public ChartStyle setPopupBorder(Color popupBorder) {
        this.popupBorder = popupBorder;
        return changed();
    }

    /**
     * Color of one layer of the hover popup's shadow; the layers stack up to the full shadow.
     */
    public Color popupShadow() {
        return new Color(0, 0, 0, dark() ? 5 : 2);
    }

    /**
     * Color of what went up, e.g. a candle that closed above its open.
     */
    public Color positive() {
        return positive != null ? positive : new Color(0x1faa6b);
    }

    public ChartStyle setPositive(Color positive) {
        this.positive = positive;
        return changed();
    }

    /**
     * Color of what went down, and of warnings such as the today line.
     */
    public Color negative() {
        return negative != null ? negative : new Color(0xe5484d);
    }

    public ChartStyle setNegative(Color negative) {
        this.negative = negative;
        return changed();
    }

    /**
     * Color of a series that has none of its own, by its index; the colors repeat when they run out.
     */
    public Color seriesColor(int index) {
        if (palette != null) {
            return palette[index % palette.length];
        }
        int color = index % (PALETTE.length + 1);
        return color == 0 ? accent() : PALETTE[color - 1];
    }

    /**
     * Colors of the series that have none of their own; none restores the default, which starts with the accent color.
     */
    public ChartStyle setPalette(Color... palette) {
        this.palette = palette == null || palette.length == 0 ? null : palette.clone();
        return changed();
    }

    public float dimmedOpacity() {
        return dimmedOpacity;
    }

    /**
     * Opacity, from 0 to 1, of the data other than what is hovered, in the charts that highlight on hover.
     */
    public ChartStyle setDimmedOpacity(float dimmedOpacity) {
        this.dimmedOpacity = Math.clamp(dimmedOpacity, 0, 1);
        return changed();
    }

    public boolean legendVisible() {
        return legendVisible;
    }

    public ChartStyle setLegendVisible(boolean legendVisible) {
        this.legendVisible = legendVisible;
        return changed();
    }

    public RectangleEdge legendPosition() {
        return legendPosition;
    }

    /**
     * Side of the chart the legend is on; the top by default.
     */
    public ChartStyle setLegendPosition(RectangleEdge legendPosition) {
        this.legendPosition = legendPosition;
        return changed();
    }

    /**
     * Axis without its line and tick marks, with muted tick labels.
     */
    public void applyAxis(Axis axis) {
        axis.setAxisLineVisible(false);
        axis.setTickMarksVisible(false);
        axis.setTickLabelFont(axisFont());
        axis.setTickLabelPaint(mutedForeground());
        axis.setLabelFont(axisFont());
        axis.setLabelPaint(mutedForeground());
        axis.setTickLabelInsets(new RectangleInsets(4, 6, 4, 6));
    }

    /**
     * Title in the title font and the foreground, or subtitle in the font and the muted foreground.
     */
    public void applyTitle(TextTitle title, boolean subtitle) {
        if (title == null) {
            return;
        }
        title.setFont(subtitle ? font() : titleFont());
        title.setPaint(subtitle ? mutedForeground() : foreground());
        title.setHorizontalAlignment(titleAlignment);
        title.setTextAlignment(titleAlignment);
        title.setPadding(subtitle ? new RectangleInsets(0, 8, 6, 8) : new RectangleInsets(2, 8, 2, 8));
    }

    /**
     * Legend without frame or background, in the style's font, foreground and position.
     */
    public void applyLegend(LegendTitle legend) {
        if (legend == null) {
            return;
        }
        legend.setFrame(BlockBorder.NONE);
        legend.setBackgroundPaint(null);
        legend.setItemFont(legendFont());
        legend.setItemPaint(foreground());
        legend.setItemLabelPadding(new RectangleInsets(2, 4, 2, 12));
        legend.setPosition(legendPosition);
        legend.setVisible(legendVisible);
    }

    /**
     * Runs the listener whenever the style changes, so its charts restyle themselves.
     */
    void addListener(Runnable listener) {
        listeners.add(listener);
    }

    void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private ChartStyle changed() {
        List.copyOf(listeners).forEach(Runnable::run);
        return this;
    }

    /**
     * Whether the chart is on a dark background: that of the style if it has one, else the theme's.
     */
    private boolean dark() {
        return background != null ? luminance(background) < 128 : FlatLaf.isLafDark();
    }

    private static double luminance(Color color) {
        return color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114;
    }

    /**
     * Black or white, whichever reads better as text on the color.
     */
    public static Color contrast(Color color) {
        return luminance(color) > 160 ? Color.BLACK : Color.WHITE;
    }

    public static Color alpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /**
     * {@code color} at an opacity from 0 to 1.
     */
    public static Color alpha(Color color, float opacity) {
        return alpha(color, Math.round(Math.clamp(opacity, 0, 1) * 255));
    }

    /**
     * {@code a} blended into {@code b}: 0 is all {@code b}, 1 is all {@code a}.
     */
    public static Color mix(Color a, Color b, float amount) {
        return new Color(
                Math.round(b.getRed() + (a.getRed() - b.getRed()) * amount),
                Math.round(b.getGreen() + (a.getGreen() - b.getGreen()) * amount),
                Math.round(b.getBlue() + (a.getBlue() - b.getBlue()) * amount));
    }
}
