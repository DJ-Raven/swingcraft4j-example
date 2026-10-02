package com.swingcraft4j.animatedchart;

import com.formdev.flatlaf.util.UIScale;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleInsets;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.HierarchyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Base of the animated charts: the series, a sharp themed {@link ChartPanel}, the hover popup and a clickable legend.
 */
public abstract class AnimatedChart extends JPanel {

    /**
     * One line of the hover popup: a colored dot, a name and a formatted value.
     */
    public record HoverRow(Color color, String name, String value) {
    }

    protected final ChartAnimator animator = new ChartAnimator();
    private final String[] categories;
    // name, color (null for the accent color), target values and visibility of each series
    private final List<String> names = new ArrayList<>();
    private final List<Color> colors = new ArrayList<>();
    private final List<double[]> values = new ArrayList<>();
    private final List<Boolean> visible = new ArrayList<>();
    private final DecimalFormat numberFormat = new DecimalFormat("#,##0.#");
    private final Runnable styleListener = this::styleChanged;
    private ChartStyle style = new ChartStyle();
    private JFreeChart chart;
    private ChartPanel chartPanel;
    private ChartHover hover;
    private ValueFormatter valueFormatter;
    private final List<ChartClickListener> clickListeners = new ArrayList<>();
    private TextTitle title;
    private TextTitle subtitle;
    private boolean introOnShow = true;
    private int introDelay;
    private boolean introPlayed;

    protected AnimatedChart(String... categories) {
        super(new BorderLayout());
        this.categories = categories.clone();
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing() && chart != null && !introPlayed) {
                introPlayed = true;
                if (introOnShow) {
                    animator.delayNext(introDelay);
                    playIntro();
                }
            }
        });
    }

    /**
     * Shows the chart; subclasses call it once their plot is built.
     */
    protected void setChart(JFreeChart chart) {
        this.chart = chart;
        // unbuffered, and never scaled as an image, so it stays sharp at any size and UI scale
        chartPanel = new ChartPanel(chart, false);
        chartPanel.setMouseZoomable(false);
        chartPanel.setPopupMenu(null);
        chartPanel.setMinimumDrawWidth(0);
        chartPanel.setMinimumDrawHeight(0);
        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
        chartPanel.setOpaque(false);
        // the hover popup replaces the Swing tooltips
        chartPanel.setDisplayToolTips(false);
        hover = new ChartHover(this, chartPanel);
        // clicking a legend item hides or shows its series
        chartPanel.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent e) {
                if (e.getEntity() instanceof LegendItemEntity item) {
                    int series = names.indexOf(String.valueOf(item.getSeriesKey()));
                    if (series >= 0) {
                        setSeriesVisible(series, !isSeriesVisible(series));
                    }
                } else if (getHoveredIndex() >= 0 && SwingUtilities.isLeftMouseButton(e.getTrigger())) {
                    // what is hovered is what is clicked
                    int index = getHoveredIndex();
                    ChartClickEvent event = new ChartClickEvent(AnimatedChart.this, index, hoverTitle(index), hoverRows(index), e.getTrigger());
                    List.copyOf(clickListeners).forEach(listener -> listener.chartClicked(event));
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent e) {
                chartPanel.setCursor(e.getEntity() instanceof LegendItemEntity
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        });
        add(chartPanel);
        applyStyle();
    }

    public JFreeChart getChart() {
        return chart;
    }

    protected ChartPanel getChartPanel() {
        return chartPanel;
    }

    /**
     * Fonts, colors, grid and legend of the chart; changing them restyles the chart right away.
     */
    public ChartStyle getStyle() {
        return style;
    }

    /**
     * Replaces the chart's style, e.g. with one shared by several charts.
     */
    public void setStyle(ChartStyle style) {
        this.style.removeListener(styleListener);
        this.style = style;
        if (isDisplayable()) {
            style.addListener(styleListener);
        }
        styleChanged();
    }

    /**
     * Follows the style only while in a window, so a shared style doesn't keep discarded charts alive.
     */
    @Override
    public void addNotify() {
        super.addNotify();
        style.addListener(styleListener);
        styleChanged();
    }

    @Override
    public void removeNotify() {
        style.removeListener(styleListener);
        super.removeNotify();
    }

    private void styleChanged() {
        if (chart != null) {
            applyStyle();
        }
    }

    public String getTitle() {
        return title != null ? title.getText() : null;
    }

    /**
     * Text above the chart, in the style's title font; {@code null} or empty for none.
     */
    public void setTitle(String text) {
        title = text == null || text.isEmpty() ? null : new TextTitle(text);
        chart.setTitle(title);
        applyStyle();
    }

    public String getSubtitle() {
        return subtitle != null ? subtitle.getText() : null;
    }

    /**
     * Smaller, muted text under the title; {@code null} or empty for none.
     */
    public void setSubtitle(String text) {
        if (subtitle != null) {
            chart.removeSubtitle(subtitle);
        }
        subtitle = text == null || text.isEmpty() ? null : new TextTitle(text);
        if (subtitle != null) {
            // before the legend, which is a subtitle of the chart too, so that it is above it
            chart.addSubtitle(0, subtitle);
        }
        applyStyle();
    }

    /**
     * Hears about clicks on what the chart shows: the category, slice, task and so on that hovering has a popup for.
     */
    public void addChartClickListener(ChartClickListener listener) {
        clickListeners.add(listener);
    }

    public void removeChartClickListener(ChartClickListener listener) {
        clickListeners.remove(listener);
    }

    /**
     * The chart as it looks now, without the hover popup, at a multiple of its size on screen.
     */
    public BufferedImage toImage(double scale) {
        Dimension size = getWidth() > 0 && getHeight() > 0 ? getSize() : getPreferredSize();
        BufferedImage image = new BufferedImage((int) Math.ceil(size.width * scale), (int) Math.ceil(size.height * scale),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            // on the background it is seen on, which a chart doesn't paint itself unless its style has one
            g.setColor(style.background());
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.scale(scale, scale);
            // with rendering info, which some charts need to paint all of themselves
            chart.draw(g, new Rectangle2D.Double(0, 0, size.width, size.height), new ChartRenderingInfo());
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Saves the chart as an image of the file's type, PNG unless its name ends in another, at a multiple of its size.
     */
    public void saveAsImage(File file, double scale) throws IOException {
        String name = file.getName();
        String type = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "png";
        if (!ImageIO.write(toImage(scale), type, file)) {
            throw new IOException("Can't write images of type " + type);
        }
    }

    /**
     * Copies the chart to the clipboard as an image, at the resolution of the screen it is on.
     */
    public void copyToClipboard() {
        GraphicsConfiguration screen = getGraphicsConfiguration();
        Image image = toImage(screen != null ? screen.getDefaultTransform().getScaleX() : 1);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{DataFlavor.imageFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return DataFlavor.imageFlavor.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (!isDataFlavorSupported(flavor)) {
                    throw new UnsupportedFlavorException(flavor);
                }
                return image;
            }
        }, null);
    }

    /**
     * Whether value changes and the intro are animated; when they aren't, the chart jumps straight to its values.
     */
    public boolean isAnimated() {
        return animator.isEnabled();
    }

    public void setAnimated(boolean animated) {
        animator.setEnabled(animated);
    }

    /**
     * Whether the intro plays by itself when the chart is first shown.
     */
    public boolean isIntroOnShow() {
        return introOnShow;
    }

    public void setIntroOnShow(boolean introOnShow) {
        this.introOnShow = introOnShow;
    }

    /**
     * Time in milliseconds that the intro waits when the chart is first shown, e.g. to stagger several charts.
     */
    public int getIntroDelay() {
        return introDelay;
    }

    public void setIntroDelay(int introDelay) {
        this.introDelay = introDelay;
    }

    /**
     * Plays the intro again from the start.
     */
    public abstract void playIntro();

    /**
     * Adds a series with one value per category; a {@code null} color uses the next color of the style's palette.
     */
    public void addSeries(String name, Color color, double... values) {
        checkLength(values);
        names.add(name);
        colors.add(color);
        this.values.add(values.clone());
        visible.add(true);
        seriesAdded(names.size() - 1);
        applyStyle();
    }

    /**
     * Puts a newly added series into the chart's dataset.
     */
    protected abstract void seriesAdded(int series);

    public int getSeriesCount() {
        return names.size();
    }

    public String getSeriesName(int series) {
        return names.get(series);
    }

    /**
     * Color of a series, resolved to its color of the style's palette if it has none of its own.
     */
    public Color getSeriesColor(int series) {
        return colors.get(series) != null ? colors.get(series) : style.seriesColor(series);
    }

    /**
     * Changes the color of a series; {@code null} goes back to its color of the style's palette.
     */
    public void setSeriesColor(int series, Color color) {
        colors.set(series, color);
        applyStyle();
    }

    public int getCategoryCount() {
        return categories.length;
    }

    public String getCategory(int index) {
        return categories[index];
    }

    public double[] getValues(int series) {
        return values.get(series).clone();
    }

    /**
     * Animates a series from the values shown now to new ones, one per category.
     */
    public void setValues(int series, double... values) {
        checkLength(values);
        this.values.set(series, values.clone());
        valuesChanged();
    }

    /**
     * Animates the chart from what it shows now to the series' current values.
     */
    protected abstract void valuesChanged();

    /**
     * Target values of every series, by series then category.
     */
    protected double[][] seriesValues() {
        double[][] all = new double[values.size()][];
        for (int s = 0; s < all.length; s++) {
            all[s] = values.get(s).clone();
        }
        return all;
    }

    public boolean isSeriesVisible(int series) {
        return visible.get(series);
    }

    /**
     * Hides or shows a series with an animation; clicking its legend item does the same.
     */
    public void setSeriesVisible(int series, boolean visible) {
        if (this.visible.get(series) != visible) {
            this.visible.set(series, visible);
            refreshLegend();
            visibilityChanged(series);
        }
    }

    /**
     * Animates a series out or in after its visibility changed.
     */
    protected abstract void visibilityChanged(int series);

    /**
     * Whether hovering shows a popup card with the values under the mouse.
     */
    public boolean isHoverPopup() {
        return hover.isPopup();
    }

    public void setHoverPopup(boolean hoverPopup) {
        hover.setPopup(hoverPopup);
    }

    public ValueFormatter getValueFormatter() {
        return valueFormatter;
    }

    /**
     * Text of the values on the value axis and in the hover popup; {@code null} restores the default number format.
     */
    public void setValueFormatter(ValueFormatter formatter) {
        valueFormatter = formatter;
        if (getValueAxis() != null) {
            getValueAxis().setNumberFormatOverride(formatter != null ? formatter.toNumberFormat() : null);
        }
        chart.fireChartChanged();
    }

    protected String formatValue(double value) {
        return valueFormatter != null ? valueFormatter.format(value) : numberFormat.format(value);
    }

    /**
     * The chart's value axis, or {@code null} if it has none.
     */
    protected abstract NumberAxis getValueAxis();

    /**
     * Index under the mouse, or -1: a category, or whatever {@link #hoverAt} of the chart returns.
     */
    protected int getHoveredIndex() {
        return hover.getIndex();
    }

    /**
     * How far, from 0 to 1, an index is emphasized now as the hovered one; it eases in and out.
     */
    protected float getHoverFocus(int index) {
        return hover.getFocus(index);
    }

    /**
     * Opacity of an index now, for charts that dim what isn't hovered: it eases down to the style's dimmed opacity
     * while another index is hovered, and back to 1.
     */
    protected float getHoverOpacity(int index) {
        return 1 - hover.getDim(index) * (1 - style.dimmedOpacity());
    }

    /**
     * Where the mouse last was over the data area, in the chart panel's coordinates, or {@code null}.
     */
    protected Point getHoverPoint() {
        return hover.getMouse();
    }

    /**
     * Index of what is at a point of the panel, inside the data area, or -1; a category by default.
     */
    protected abstract int hoverAt(Point2D point, Rectangle2D dataArea);

    /**
     * Called when the hovered index changed, for charts that restyle themselves on hover.
     */
    protected void hoverChanged() {
    }

    /**
     * Whether the popup may show for an index; false while an intro hasn't revealed it yet.
     */
    protected boolean isHoverShown(int index, Rectangle2D dataArea) {
        return true;
    }

    /**
     * Title of the popup card, or {@code null} for none; the category by default.
     */
    protected String hoverTitle(int index) {
        return categories[index];
    }

    /**
     * Rows of the popup card; every visible series' value at the category by default.
     */
    protected List<HoverRow> hoverRows(int index) {
        List<HoverRow> rows = new ArrayList<>();
        for (int s = 0; s < names.size(); s++) {
            if (visible.get(s)) {
                rows.add(new HoverRow(getSeriesColor(s), names.get(s), formatValue(values.get(s)[index])));
            }
        }
        return rows;
    }

    /**
     * Area of the panel that the popup card stays off for an index if it can, e.g. its marks; {@code null}, the
     * default, puts the card beside the mouse.
     */
    protected Shape hoverKeepClear(int index) {
        return null;
    }

    /**
     * Paints marks for the hovered index under the popup card, e.g. a guide line; nothing by default.
     */
    protected void paintHoverMarks(Graphics2D g, Rectangle2D dataArea, int index) {
    }

    public Easing getEasing() {
        return animator.getEasing();
    }

    public void setEasing(Easing easing) {
        animator.setEasing(easing);
    }

    /**
     * Length of the intro and of value changes, in milliseconds.
     */
    public int getDuration() {
        return animator.getDuration();
    }

    public void setDuration(int duration) {
        animator.setDuration(duration);
    }

    /**
     * Runs dataset changes with one repaint for all of them, instead of one per change.
     */
    protected void batch(Runnable changes) {
        chart.setNotify(false);
        try {
            changes.run();
        } finally {
            chart.setNotify(true);
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // the constructor of JPanel calls this before the chart exists
        if (chart != null) {
            applyStyle();
        }
    }

    /**
     * Colors and fonts from the chart's style; overrides call this, then style their plot.
     */
    protected void applyStyle() {
        chart.setBackgroundPaint(style.fill());
        chart.setPadding(new RectangleInsets(4, 4, 4, 4));
        style.applyTitle(title, false);
        style.applyTitle(subtitle, true);
        style.applyLegend(chart.getLegend());
        chart.getPlot().setBackgroundPaint(null);
        chart.getPlot().setOutlineVisible(false);
        refreshLegend();
    }

    /**
     * Rebuilds the legend: a dot and name per series, dimmed for the hidden ones.
     */
    private void refreshLegend() {
        float radius = UIScale.scale(4f);
        Shape dot = new Ellipse2D.Float(-radius, -radius, radius * 2, radius * 2);
        LegendItemCollection items = new LegendItemCollection();
        for (int s = 0; s < names.size(); s++) {
            boolean shown = visible.get(s);
            Color color = shown ? getSeriesColor(s) : ChartStyle.mix(style.mutedForeground(), style.background(), 0.5f);
            LegendItem item = new LegendItem(names.get(s), null, null, null, dot, color);
            item.setSeriesKey(names.get(s));
            item.setSeriesIndex(s);
            if (!shown) {
                item.setLabelPaint(style.mutedForeground());
            }
            items.add(item);
        }
        setLegendItems(items);
    }

    /**
     * Gives the plot the legend items to show instead of its own.
     */
    protected abstract void setLegendItems(LegendItemCollection items);

    private void checkLength(double[] values) {
        if (values.length != categories.length) {
            throw new IllegalArgumentException("Expected " + categories.length + " values, got " + values.length);
        }
    }

    /**
     * Value between {@code from} and {@code to} at {@code progress} (0 to 1).
     */
    protected static double lerp(double from, double to, double progress) {
        return from + (to - from) * progress;
    }
}
