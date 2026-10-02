package com.swingcraft4j.animatedchart.chart;

import com.formdev.flatlaf.util.UIScale;
import com.swingcraft4j.animatedchart.AnimatedChart;
import com.swingcraft4j.animatedchart.ChartStyle;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.IntToDoubleFunction;

/**
 * JFreeChart Gantt chart of named tasks over dates, with progress, milestones, dependency arrows, a today line,
 * an animated intro and animated date changes.
 */
public class GanttChart extends AnimatedChart {

    /**
     * How the chart first appears.
     */
    public enum Intro {
        /**
         * Tasks are drawn in from left to right.
         */
        DRAW("Draw"),
        /**
         * Every task grows from its start at once.
         */
        GROW("Grow"),
        /**
         * Tasks grow from their start one after another.
         */
        CASCADE("Cascade");

        private final String name;

        Intro(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // what is kept of each task: its start and end in days since the epoch, its progress, and how far it has appeared
    private static final int START = 0;
    private static final int END = 1;
    private static final int PROGRESS = 2;
    private static final int APPEAR = 3;
    private static final int PARTS = 4;
    private static final long DAY = 86_400_000L;
    // share of the duration over which the tasks' start times are spread in the cascade
    private static final double CASCADE_SPREAD = 0.5;
    // share of a task's color in the part of its bar that isn't done yet
    private static final float TRACK_SHARE = 0.3f;
    private static final int BAND_ALPHA = 14;

    private final TaskDataset dataset = new TaskDataset();
    // in UTC, so a day is always the same length
    private final DateAxis dateAxis = new DateAxis(null, TimeZone.getTimeZone("UTC"), Locale.getDefault());
    private final RevealPlot plot;
    // target values of every task, by part then task, and each task's group or -1
    private final double[][] tasks;
    private final int[] groups;
    // opacity of each group as shown now, fading when it is hidden or shown
    private final List<Double> groupOpacity = new ArrayList<>();
    private final List<int[]> dependencies = new ArrayList<>();
    private Intro intro = Intro.CASCADE;
    private DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MMM d");
    private LocalDate today;
    private String todayText = "Today";
    // null for the style's negative color
    private Color todayColor;
    private float barHeight = 22;
    private float cornerRadius = 4;
    private boolean progressVisible = true;
    private boolean labelsVisible = true;
    private boolean dependenciesVisible = true;
    private boolean hoverHighlight = true;
    private boolean hasTasks;

    public GanttChart(String... names) {
        super(names);
        tasks = new double[PARTS][names.length];
        Arrays.fill(tasks[APPEAR], 1);
        groups = new int[names.length];
        Arrays.fill(groups, -1);

        SymbolAxis taskAxis = new SymbolAxis(null, names);
        taskAxis.setGridBandsVisible(false);
        taskAxis.setTickUnit(new NumberTickUnit(1));
        taskAxis.setRange(-0.5, names.length - 0.5);
        // the first task at the top
        taskAxis.setInverted(true);
        dateAxis.setDateFormatOverride(new AxisFormat());
        plot = new RevealPlot(dataset, dateAxis, taskAxis, new TaskRenderer()) {
            @Override
            public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor, PlotState parentState, PlotRenderingInfo info) {
                super.draw(g2, area, anchor, parentState, info);
                // after the plot, because the tag sits above the data area, where the renderer can't paint
                if (info != null) {
                    paintTodayTag(g2, info.getDataArea());
                }
            }
        };
        setChart(new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, true));
    }

    /**
     * Sets the start, end and progress (0 to 1) of every task; after the first time, animates to them.
     * A task lasts up to its end date, not through it, and one that ends where it starts is a milestone.
     */
    public void setTasks(LocalDate[] start, LocalDate[] end, double[] progress) {
        int count = getCategoryCount();
        if (start.length != count || end.length != count || progress.length != count) {
            throw new IllegalArgumentException("Expected " + count + " tasks");
        }
        for (int i = 0; i < count; i++) {
            tasks[START][i] = start[i].toEpochDay();
            tasks[END][i] = end[i].toEpochDay();
            tasks[PROGRESS][i] = Math.clamp(progress[i], 0, 1);
        }
        if (hasTasks) {
            valuesChanged();
        } else {
            hasTasks = true;
            if (dateAxis.isAutoRange() && count > 0) {
                double first = Arrays.stream(tasks[START]).min().getAsDouble();
                double last = Arrays.stream(tasks[END]).max().getAsDouble();
                dateAxis.setRange((first - 1) * DAY, (last + 1) * DAY);
            }
            dataset.show(targets());
        }
    }

    /**
     * Dates the chart spans, fixed so it doesn't rescale while tasks animate; by default it fits the tasks first set.
     */
    public void setRange(LocalDate from, LocalDate to) {
        dateAxis.setRange((double) from.toEpochDay() * DAY, (double) to.toEpochDay() * DAY);
    }

    /**
     * Adds a group of tasks with its own color and legend item; a {@code null} color uses the style's palette.
     */
    public void addGroup(String name, Color color, int... tasks) {
        for (int task : tasks) {
            groups[task] = getSeriesCount();
        }
        groupOpacity.add(1.0);
        // a group is a series of the chart, which gives it its legend item; its values aren't used
        addSeries(name, color, new double[getCategoryCount()]);
    }

    /**
     * Draws an arrow from the end of one task to the start of another that waits for it.
     */
    public void addDependency(int from, int to) {
        dependencies.add(new int[]{from, to});
        getChart().fireChartChanged();
    }

    @Override
    protected void seriesAdded(int series) {
    }

    @Override
    protected void valuesChanged() {
        if (hasTasks) {
            plot.setReveal(1);
            morph(dataset.shown());
        }
    }

    /**
     * The tasks of a hidden group fade out and those of a shown one fade in.
     */
    @Override
    protected void visibilityChanged(int series) {
        valuesChanged();
    }

    @Override
    public void playIntro() {
        if (!hasTasks) {
            return;
        }
        for (int g = 0; g < groupOpacity.size(); g++) {
            groupOpacity.set(g, isSeriesVisible(g) ? 1.0 : 0);
        }
        if (intro == Intro.DRAW) {
            animator.stop();
            dataset.show(targets());
            plot.setReveal(0);
            animator.start(plot::setReveal);
        } else if (intro == Intro.GROW) {
            plot.setReveal(1);
            animator.start(p -> grow(task -> p));
        } else {
            // each task starts a little later and eases on its own
            plot.setReveal(1);
            int count = getCategoryCount();
            double step = count > 1 ? CASCADE_SPREAD / (count - 1) : 0;
            double span = 1 - step * (count - 1);
            animator.startLinear(t -> grow(task -> getEasing().apply(Math.clamp((t - task * step) / span, 0, 1))), null);
        }
    }

    public Intro getIntro() {
        return intro;
    }

    public void setIntro(Intro intro) {
        this.intro = intro;
    }

    public DateTimeFormatter getDateFormat() {
        return dateFormat;
    }

    /**
     * Text of the dates on the date axis, in the hover popup and beside the milestones.
     */
    public void setDateFormat(DateTimeFormatter dateFormat) {
        this.dateFormat = dateFormat;
        getChart().fireChartChanged();
    }

    public LocalDate getToday() {
        return today;
    }

    /**
     * Marks a date with a dashed line across the tasks, tagged above the plot; {@code null} for none.
     */
    public void setToday(LocalDate today) {
        this.today = today;
        applyInsets();
    }

    public Color getTodayColor() {
        return todayColor != null ? todayColor : getStyle().negative();
    }

    /**
     * Color of the today line and its tag; {@code null} uses the style's negative color.
     */
    public void setTodayColor(Color todayColor) {
        this.todayColor = todayColor;
        getChart().fireChartChanged();
    }

    public String getTodayText() {
        return todayText;
    }

    /**
     * Text of the tag above the today line.
     */
    public void setTodayText(String todayText) {
        this.todayText = todayText;
        getChart().fireChartChanged();
    }

    /**
     * Height of a bar at most, before UI scaling; bars get thinner when the rows don't have room for it.
     */
    public float getBarHeight() {
        return barHeight;
    }

    public void setBarHeight(float barHeight) {
        this.barHeight = barHeight;
        getChart().fireChartChanged();
    }

    /**
     * Radius of the bars' corners, before UI scaling; 0 is square.
     */
    public float getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
        getChart().fireChartChanged();
    }

    /**
     * Whether each bar is filled only as far as the task's progress, over a faint track of its full length.
     */
    public boolean isProgressVisible() {
        return progressVisible;
    }

    public void setProgressVisible(boolean progressVisible) {
        this.progressVisible = progressVisible;
        getChart().fireChartChanged();
    }

    /**
     * Whether each bar has its progress beside it, if that is shown, and each milestone its date.
     */
    public boolean isLabelsVisible() {
        return labelsVisible;
    }

    public void setLabelsVisible(boolean labelsVisible) {
        this.labelsVisible = labelsVisible;
        getChart().fireChartChanged();
    }

    public boolean isDependenciesVisible() {
        return dependenciesVisible;
    }

    public void setDependenciesVisible(boolean dependenciesVisible) {
        this.dependenciesVisible = dependenciesVisible;
        getChart().fireChartChanged();
    }

    /**
     * Whether hovering a task marks its row and dims the other tasks.
     */
    public boolean isHoverHighlight() {
        return hoverHighlight;
    }

    public void setHoverHighlight(boolean hoverHighlight) {
        this.hoverHighlight = hoverHighlight;
        getChart().fireChartChanged();
    }

    /**
     * Target values of every task, fully appeared.
     */
    private double[][] targets() {
        double[][] targets = new double[PARTS][];
        for (int part = 0; part < PARTS; part++) {
            targets[part] = tasks[part].clone();
        }
        return targets;
    }

    /**
     * Animates every task from {@code from} to its target values, and every group to shown or hidden.
     */
    private void morph(double[][] from) {
        double[][] to = targets();
        List<Double> fadeFrom = new ArrayList<>(groupOpacity);
        animator.start(p -> {
            double[][] frame = new double[PARTS][getCategoryCount()];
            for (int part = 0; part < PARTS; part++) {
                for (int i = 0; i < frame[part].length; i++) {
                    frame[part][i] = lerp(from[part][i], to[part][i], p);
                }
            }
            for (int g = 0; g < fadeFrom.size(); g++) {
                // overshooting easings would take the opacity out of range
                groupOpacity.set(g, lerp(fadeFrom.get(g), isSeriesVisible(g) ? 1 : 0, Math.clamp(p, 0, 1)));
            }
            dataset.show(frame);
        });
    }

    /**
     * Shows every task grown from its start as far as the function gives for it, from 0 to 1.
     */
    private void grow(IntToDoubleFunction progress) {
        double[][] frame = targets();
        for (int i = 0; i < getCategoryCount(); i++) {
            double grown = progress.applyAsDouble(i);
            frame[END][i] = lerp(frame[START][i], frame[END][i], grown);
            frame[APPEAR][i] = grown;
        }
        dataset.show(frame);
    }

    private boolean isMilestone(int task) {
        return tasks[START][task] == tasks[END][task];
    }

    private Color taskColor(int task) {
        return groups[task] >= 0 ? getSeriesColor(groups[task]) : getStyle().accent();
    }

    /**
     * Opacity of a task as shown now: that of its group, which fades when hidden.
     */
    private double taskOpacity(int task) {
        return groups[task] >= 0 ? groupOpacity.get(groups[task]) : 1;
    }

    private String formatDate(double days) {
        return dateFormat.format(LocalDate.ofEpochDay(Math.round(days)));
    }

    private double dateX(double days, Rectangle2D dataArea) {
        return dateAxis.valueToJava2D(days * DAY, dataArea, plot.getDomainAxisEdge());
    }

    private double rowY(int task, Rectangle2D dataArea) {
        return plot.getRangeAxis().valueToJava2D(task, dataArea, plot.getRangeAxisEdge());
    }

    /**
     * Leaves room above the plot for the today line's tag.
     */
    private void applyInsets() {
        double top = 4;
        if (today != null) {
            top = ChartTag.height(getChartPanel().getFontMetrics(getStyle().font())) + UIScale.scale(4);
        }
        plot.setInsets(new RectangleInsets(top, 8, 4, 8));
    }

    /**
     * The tag at the top of the today line.
     */
    private void paintTodayTag(Graphics2D g, Rectangle2D dataArea) {
        if (today == null || !hasTasks) {
            return;
        }
        double x = dateX(today.toEpochDay(), dataArea);
        // not outside the dates shown, nor until the draw-in intro has reached it
        if (x < dataArea.getMinX() || x > dataArea.getMaxX() || !plot.isRevealed(x, dataArea)) {
            return;
        }
        FontMetrics metrics = g.getFontMetrics(getStyle().font());
        int width = ChartTag.width(metrics, todayText);
        double left = Math.clamp(x - width / 2.0, dataArea.getMinX(), Math.max(dataArea.getMinX(), dataArea.getMaxX() - width));
        ChartTag.paint(g, metrics, todayText, left, dataArea.getMinY() - ChartTag.height(metrics) - UIScale.scale(2),
                getTodayColor(), ChartStyle.contrast(getTodayColor()));
    }

    @Override
    protected NumberAxis getValueAxis() {
        return null;
    }

    /**
     * The legend lists the groups; without any, it is hidden.
     */
    @Override
    protected void setLegendItems(LegendItemCollection items) {
        plot.setFixedLegendItems(items);
        getChart().getLegend().setVisible(getStyle().legendVisible() && items.getItemCount() > 0);
    }

    /**
     * The task whose row the point is in.
     */
    @Override
    protected int hoverAt(Point2D point, Rectangle2D dataArea) {
        if (!hasTasks) {
            return -1;
        }
        double row = plot.getRangeAxis().java2DToValue(point.getY(), dataArea, plot.getRangeAxisEdge());
        return Math.clamp(Math.round(row), 0, getCategoryCount() - 1);
    }

    /**
     * Not for a task of a hidden group, nor while the intro hasn't reached the task yet.
     */
    @Override
    protected boolean isHoverShown(int task, Rectangle2D dataArea) {
        return (groups[task] < 0 || isSeriesVisible(groups[task])) && dataset.shown()[APPEAR][task] > 0
                && plot.isRevealed(dateX(tasks[START][task], dataArea), dataArea);
    }

    /**
     * The task's start, end, length and progress in its color; only the date of a milestone.
     */
    @Override
    protected List<HoverRow> hoverRows(int task) {
        Color color = taskColor(task);
        if (isMilestone(task)) {
            return List.of(new HoverRow(color, "Date", formatDate(tasks[START][task])));
        }
        long days = Math.round(tasks[END][task] - tasks[START][task]);
        List<HoverRow> rows = new ArrayList<>();
        rows.add(new HoverRow(color, "Start", formatDate(tasks[START][task])));
        rows.add(new HoverRow(color, "End", formatDate(tasks[END][task])));
        rows.add(new HoverRow(color, "Duration", days + (days == 1 ? " day" : " days")));
        if (progressVisible) {
            rows.add(new HoverRow(color, "Progress", Math.round(tasks[PROGRESS][task] * 100) + "%"));
        }
        return rows;
    }

    @Override
    protected void applyStyle() {
        super.applyStyle();
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(getStyle().gridVisible());
        plot.setDomainGridlinePaint(getStyle().gridColor());
        plot.setDomainGridlineStroke(new BasicStroke(1));
        plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
        getStyle().applyAxis(dateAxis);
        getStyle().applyAxis(plot.getRangeAxis());
        applyInsets();
    }

    /**
     * A line through the points with its corners rounded.
     */
    private static Path2D rounded(List<Point2D.Double> points, double radius) {
        Path2D path = new Path2D.Double();
        path.moveTo(points.getFirst().x, points.getFirst().y);
        for (int i = 1; i < points.size() - 1; i++) {
            Point2D.Double corner = points.get(i);
            Point2D.Double in = toward(corner, points.get(i - 1), radius);
            Point2D.Double out = toward(corner, points.get(i + 1), radius);
            path.lineTo(in.x, in.y);
            path.quadTo(corner.x, corner.y, out.x, out.y);
        }
        path.lineTo(points.getLast().x, points.getLast().y);
        return path;
    }

    /**
     * The point at {@code distance} from one point toward another, at most half way.
     */
    private static Point2D.Double toward(Point2D.Double from, Point2D.Double to, double distance) {
        double length = from.distance(to);
        double share = length > 0 ? Math.min(distance / length, 0.5) : 0;
        return new Point2D.Double(from.x + (to.x - from.x) * share, from.y + (to.y - from.y) * share);
    }

    /**
     * The values shown now, one item per task at y = its index; replaced as a whole on every animation frame.
     */
    private static class TaskDataset extends AbstractXYDataset {

        private double[][] parts = new double[PARTS][0];

        double[][] shown() {
            return parts;
        }

        void show(double[][] parts) {
            this.parts = parts;
            fireDatasetChanged();
        }

        @Override
        public int getSeriesCount() {
            return 1;
        }

        @Override
        public Comparable<?> getSeriesKey(int series) {
            return "Tasks";
        }

        @Override
        public int getItemCount(int series) {
            return parts[START].length;
        }

        @Override
        public Number getX(int series, int item) {
            return parts[START][item] * DAY;
        }

        @Override
        public Number getY(int series, int item) {
            return item;
        }
    }

    /**
     * The chart's date format as a {@link DateFormat}, which is what a JFreeChart date axis takes.
     */
    private class AxisFormat extends DateFormat {

        AxisFormat() {
            // a DateFormat expects to have both, though this one uses neither
            calendar = Calendar.getInstance();
            numberFormat = NumberFormat.getInstance();
        }

        @Override
        public StringBuffer format(Date date, StringBuffer text, FieldPosition position) {
            return text.append(formatDate(Math.floorDiv(date.getTime(), DAY)));
        }

        @Override
        public Date parse(String source, ParsePosition position) {
            return null;
        }
    }

    /**
     * Paints the dependency arrows, then each task as a rounded bar filled up to its progress, or a diamond for a
     * milestone, then the today line on top.
     */
    private class TaskRenderer extends AbstractXYItemRenderer {

        @Override
        public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
                             ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset data, int series, int item,
                             CrosshairState crosshairState, int pass) {
            double y = rowY(item, dataArea);
            double step = Math.abs(rowY(1, dataArea) - rowY(0, dataArea));
            // as tall as the rows allow
            double height = Math.min(step * 0.56, UIScale.scale(barHeight));
            // the band and the dimming ease in and out, so the tasks don't flash
            float focus = hoverHighlight ? getHoverFocus(item) : 0;
            if (focus > 0) {
                g2.setColor(ChartStyle.alpha(getStyle().foreground(), Math.round(BAND_ALPHA * focus)));
                g2.fill(new Rectangle2D.Double(dataArea.getX(), y - step / 2, dataArea.getWidth(), step));
            }

            // under the tasks, so an arrow is hidden where it passes behind one
            if (item == 0) {
                drawDependencies(g2, dataArea, step, height);
            }
            float opacity = (float) taskOpacity(item) * (hoverHighlight ? getHoverOpacity(item) : 1);
            if (opacity > 0) {
                drawTask(g2, dataArea, item, y, height, opacity);
            }
            if (item == dataset.getItemCount(series) - 1) {
                drawToday(g2, dataArea);
            }
        }

        /**
         * A task at an opacity, which blends its colors into the background so that it still hides the arrows behind it.
         */
        private void drawTask(Graphics2D g2, Rectangle2D dataArea, int task, double y, double height, float opacity) {
            double[][] shown = dataset.shown();
            double appear = shown[APPEAR][task];
            double left = dateX(shown[START][task], dataArea);
            // where the task ends, and what to write beside it
            double right = left;
            String label = null;
            Color background = getStyle().background();
            Color color = ChartStyle.mix(taskColor(task), background, opacity);
            g2.setColor(color);
            if (isMilestone(task)) {
                double half = height / 2 * Math.max(0, appear);
                Path2D diamond = new Path2D.Double();
                diamond.moveTo(left, y - half);
                diamond.lineTo(left + half, y);
                diamond.lineTo(left, y + half);
                diamond.lineTo(left - half, y);
                diamond.closePath();
                g2.fill(diamond);
                right = left + half;
                label = formatDate(tasks[START][task]);
            } else {
                double width = dateX(shown[END][task], dataArea) - left;
                double progress = Math.clamp(shown[PROGRESS][task], 0, 1);
                if (width > 0) {
                    double arc = Math.min(UIScale.scale(cornerRadius) * 2, Math.min(width, height));
                    RoundRectangle2D bar = new RoundRectangle2D.Double(left, y - height / 2, width, height, arc, arc);
                    if (progressVisible) {
                        g2.setColor(ChartStyle.mix(color, background, TRACK_SHARE));
                        g2.fill(bar);
                        // the part that is done, cut out of the bar so it keeps the rounded corners
                        Area done = new Area(bar);
                        done.intersect(new Area(new Rectangle2D.Double(left, y - height / 2, width * progress, height)));
                        g2.setColor(color);
                        g2.fill(done);
                    } else {
                        g2.fill(bar);
                    }
                    right = left + width;
                }
                if (progressVisible) {
                    label = Math.round(progress * 100) + "%";
                }
            }

            if (labelsVisible && label != null && appear > 0) {
                g2.setFont(getStyle().font());
                g2.setColor(ChartStyle.mix(getStyle().mutedForeground(), background, opacity));
                FontMetrics metrics = g2.getFontMetrics();
                g2.drawString(label, (float) right + UIScale.scale(6), (float) (y + (metrics.getAscent() - metrics.getDescent()) / 2.0));
            }
        }

        private void drawToday(Graphics2D g2, Rectangle2D dataArea) {
            if (today == null) {
                return;
            }
            double x = dateX(today.toEpochDay(), dataArea);
            float dash = UIScale.scale(4f);
            g2.setColor(getTodayColor());
            g2.setStroke(new BasicStroke(UIScale.scale(1.2f), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{dash, dash}, 0));
            g2.draw(new Line2D.Double(x, dataArea.getMinY(), x, dataArea.getMaxY()));
        }

        /**
         * An arrow per dependency, from under the end of the one task to the start of the other.
         */
        private void drawDependencies(Graphics2D g2, Rectangle2D dataArea, double step, double height) {
            if (!dependenciesVisible) {
                return;
            }
            double[][] shown = dataset.shown();
            double head = UIScale.scale(5f);
            double stub = UIScale.scale(8f);
            Composite composite = g2.getComposite();
            g2.setColor(ChartStyle.mix(getStyle().foreground(), getStyle().background(), 0.45f));
            g2.setStroke(new BasicStroke(UIScale.scale(1.2f), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            for (int[] dependency : dependencies) {
                int from = dependency[0];
                int to = dependency[1];
                double opacity = Math.min(taskOpacity(from), taskOpacity(to)) * Math.clamp(shown[APPEAR][to], 0, 1);
                if (opacity <= 0) {
                    continue;
                }
                double fromY = rowY(from, dataArea);
                double toY = rowY(to, dataArea);
                double side = Math.signum(toY - fromY);
                double fromStart = dateX(shown[START][from], dataArea);
                double fromEnd = dateX(shown[END][from], dataArea);
                double tip = dateX(shown[START][to], dataArea) - (isMilestone(to) ? height / 2 : 0) - UIScale.scale(2f);
                // straight down from under the first task, as near its end as leaves room to turn in to the other
                double inset = Math.min(stub, (fromEnd - fromStart) / 2);
                double x = Math.min(fromEnd - inset, tip - head - stub);
                List<Point2D.Double> points = new ArrayList<>();
                if (x < fromStart + inset) {
                    // the other starts too early for that: back around between the rows
                    double turn = fromY + side * step / 2;
                    points.add(new Point2D.Double(fromEnd - inset, fromY + side * height / 2));
                    points.add(new Point2D.Double(fromEnd - inset, turn));
                    points.add(new Point2D.Double(x, turn));
                } else {
                    points.add(new Point2D.Double(x, fromY + side * height / 2));
                }
                points.add(new Point2D.Double(x, toY));
                points.add(new Point2D.Double(tip - head, toY));

                Path2D arrow = new Path2D.Double();
                arrow.moveTo(tip, toY);
                arrow.lineTo(tip - head, toY - head * 0.6);
                arrow.lineTo(tip - head, toY + head * 0.6);
                arrow.closePath();
                g2.setComposite(AlphaComposite.SrcOver.derive((float) opacity));
                g2.draw(rounded(points, UIScale.scale(4f)));
                g2.fill(arrow);
            }
            g2.setComposite(composite);
        }
    }
}
