package com.swingcraft4j;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.FontUtils;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.formdev.flatlaf.util.UIScale;
import com.swingcraft4j.animatedchart.AnimatedChart;
import com.swingcraft4j.animatedchart.ChartStyle;
import com.swingcraft4j.animatedchart.Easing;
import com.swingcraft4j.animatedchart.chart.BarChart;
import com.swingcraft4j.animatedchart.chart.BubbleChart;
import com.swingcraft4j.animatedchart.chart.CandlestickChart;
import com.swingcraft4j.animatedchart.chart.DeviationChart;
import com.swingcraft4j.animatedchart.chart.DifferenceChart;
import com.swingcraft4j.animatedchart.chart.FlowChart;
import com.swingcraft4j.animatedchart.chart.GanttChart;
import com.swingcraft4j.animatedchart.chart.LineChart;
import com.swingcraft4j.animatedchart.chart.PieChart;
import com.swingcraft4j.animatedchart.chart.PolarChart;
import com.swingcraft4j.animatedchart.chart.SpiderWebChart;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Demo entry point: a tab per animated chart and a sidebar of controls for the selected one.
 */
public class Main extends JFrame {

    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    // 50 daily readings, mostly between 50 and 130 with two spikes
    private static final double[] VISITS = {
            116, 129, 135, 86, 73, 85, 73, 68, 92, 130, 245, 139, 115, 111, 309, 206, 137, 128, 85, 94,
            71, 106, 84, 93, 85, 73, 83, 125, 107, 82, 44, 72, 106, 107, 66, 91, 92, 113, 107, 131,
            111, 64, 69, 88, 77, 83, 111, 57, 55, 60};
    // what each tab's chart is, in the order of the tabs
    private static final String[] DESCRIPTIONS = {
            "Lines over named categories: smooth, monotone, straight or stepped, with an optional area fill, points, dashed series "
                    + "and a gradient by value.",
            "The same line chart with many points: one straight line over 50 days, without points and colored by value. "
                    + "The axis labels only as many days as fit.",
            "Bars over named categories, side by side or stacked, upright or horizontal, with rounded corners. "
                    + "Hovering a category dims the others.",
            "Named slices as a pie, a donut with the total in the middle, a half circle or a rose, with gaps, rounded corners "
                    + "and labels beside the slices.",
            "Open, high, low and close per category as candles, hollow candles, OHLC bars or Heikin-Ashi, with volume bars, "
                    + "moving averages, a line at the last close and a crosshair.",
            "Named tasks over dates, in groups, with their progress, milestones, dependency arrows and a line at today.",
            "Series over named axes that meet in a center, on a web of polygons or circles, each as a closed line over its filled area.",
            "A line chart of two series with the area between them filled, in one color where the first is above the second "
                    + "and in another where it is below.",
            "A line chart whose series each have a band around their line, from a low to a high value: a range, an error margin "
                    + "or the uncertainty of a forecast.",
            "Series of bubbles placed by two values and sized by a third, with optional labels inside them. "
                    + "Bubbles move, resize, appear and disappear when the data changes.",
            "Series of radii at evenly spaced angles around a circle, clockwise from the top, drawn as closed curves or straight segments.",
            "Columns of named nodes joined by flows as wide as their values, also known as a Sankey chart. "
                    + "Hovering a node or a flow dims what isn't connected to it."};
    // how to make each tab's chart, in the order of the tabs
    private static final String[] EXAMPLES = {
            """
            LineChart chart = new LineChart("Jan", "Feb", "Mar", "Apr");
            chart.addSeries("Revenue", null, 42, 55, 48, 63);
            chart.addSeries("Expenses", null, 30, 38, 35, 44);
            chart.setLineStyle(LineChart.LineStyle.SMOOTH);
            chart.setValueFormatter(value -> "$" + Math.round(value) + "k");

            // animates to the new values
            chart.setValues(0, 50, 61, 58, 70);
            """,
            """
            LineChart chart = new LineChart(days);        // a name per day
            chart.setValueRange(0, 350);
            chart.setLineStyle(LineChart.LineStyle.STRAIGHT);
            chart.setPointsVisible(false);
            chart.setValueGradient(true);
            chart.addSeries("Visits", null, visits);      // a value per day
            """,
            """
            BarChart chart = new BarChart("Mon", "Tue", "Wed");
            chart.addSeries("Desktop", null, 62, 75, 58);
            chart.addSeries("Mobile", null, 48, 52, 66);
            chart.setCornerRadius(6);

            // one bar per category, which needs room for both series
            chart.setStacked(true);
            chart.setValueRange(0, 200);
            """,
            """
            PieChart chart = new PieChart();
            chart.addSlice("Direct", null, 38);
            chart.addSlice("Search", null, 27);
            chart.addSlice("Social", null, 18);
            chart.setDonut(true);
            chart.setLabelsVisible(true);

            // animates the slice to the new value
            chart.setValues(0, 50);
            """,
            """
            CandlestickChart chart = new CandlestickChart("Mar 1", "Mar 2", "Mar 3");
            chart.setValueRange(60, 180);
            chart.setCandles(open, high, low, close, volume);   // a value of each per day
            chart.addMovingAverage(5, null);
            chart.setCandleStyle(CandlestickChart.CandleStyle.HOLLOW);

            // animates to the new candles
            chart.setCandles(newOpen, newHigh, newLow, newClose, newVolume);
            """,
            """
            GanttChart chart = new GanttChart("Research", "Design", "Launch");
            chart.addGroup("Planning", null, 0);
            chart.addGroup("Build", null, 1, 2);
            chart.addDependency(0, 1);                // Design waits for Research
            chart.setToday(LocalDate.now());

            // a start, an end and a progress of 0 to 1 per task; setting them again animates
            chart.setTasks(start, end, progress);
            """,
            """
            SpiderWebChart chart = new SpiderWebChart("Speed", "Power", "Range", "Defense");
            chart.addSeries("Striker", null, 88, 72, 55, 40);
            chart.addSeries("Guardian", null, 48, 60, 70, 90);
            chart.setMaxValue(100);
            chart.setCircular(true);

            // animates to the new values
            chart.setValues(0, 70, 80, 60, 55);
            """,
            """
            DifferenceChart chart = new DifferenceChart("Jan", "Feb", "Mar", "Apr");
            chart.addSeries("Actual", null, 52, 46, 48, 38);
            chart.addSeries("Target", null, 27, 41, 49, 53);
            chart.setSeriesDashed(1, true);

            // where the first series is above the second, and where it is below
            chart.setColors(Color.GREEN, Color.RED);
            """,
            """
            DeviationChart chart = new DeviationChart("Jan", "Feb", "Mar", "Apr");
            chart.addSeries("North", null, values, low, high);   // one of each per category

            // animates the line and its band
            chart.setValues(0, newValues, newLow, newHigh);
            """,
            """
            BubbleChart chart = new BubbleChart();
            chart.setNames("Price", "Rating", "Sales");
            chart.addSeries("Hardware", null, x, y, size);   // a bubble per index
            chart.setLabels(0, "H1", "H2", "H3");

            // moves and resizes the bubbles; new ones grow in
            chart.setBubbles(0, newX, newY, newSize);
            """,
            """
            PolarChart chart = new PolarChart(36);        // a radius every 10 degrees
            chart.addSeries("Signal", null, radii);       // 36 values
            chart.setSmooth(true);
            chart.setAngleStep(30);

            // animates to the new radii
            chart.setValues(0, newRadii);
            """,
            """
            FlowChart chart = new FlowChart();
            chart.addFlow(0, "Search", "Landing", 44);     // from column 0 to column 1
            chart.addFlow(0, "Social", "Landing", 12);
            chart.addFlow(1, "Landing", "Purchase", 27);   // from column 1 to column 2

            // animates; a flow the chart doesn't have yet grows in
            chart.setFlow(0, "Search", "Landing", 90);
            """};
    // share of the height that the chart has over its description and example
    private static final double CHART_SHARE = 0.6;
    // series colors to pick from, by name; the first is the style's own
    private static final String[] PALETTES = {"Default", "Ocean", "Sunset", "Forest", "Berry", "Pastel", "Earth", "Neon"};
    private static final int[][] PALETTE_COLORS = {
            {},
            {0x2563eb, 0x06b6d4, 0x6366f1, 0x0ea5e9, 0x14b8a6},
            {0xf97316, 0xec4899, 0xf59e0b, 0xef4444, 0xa855f7},
            {0x16a34a, 0x84cc16, 0x0d9488, 0xca8a04, 0x65a30d},
            {0xbe185d, 0x7c3aed, 0xdb2777, 0x4f46e5, 0xe11d48},
            {0x93c5fd, 0xf9a8d4, 0xfcd34d, 0x86efac, 0xc4b5fd},
            {0xb45309, 0x78716c, 0xa16207, 0x9a3412, 0x57534e},
            {0x22d3ee, 0xa3e635, 0xf472b6, 0xfacc15, 0x818cf8}};
    // the project plan: each task's length in days (0 for a milestone), the tasks it waits for, and its slack after them
    private static final String[] TASKS = {"Research", "Requirements", "Wireframes", "UI design", "Backend API", "Frontend",
            "Integration", "Testing", "Documentation", "Launch"};
    private static final int[] TASK_DAYS = {9, 9, 11, 15, 28, 21, 9, 12, 18, 0};
    private static final int[][] TASK_AFTER = {{}, {0}, {1}, {2}, {1}, {3}, {4, 5}, {6}, {3}, {7, 8}};
    private static final int[] TASK_SLACK = {0, 0, 0, 0, 3, 0, 0, 0, 19, 5};
    private static final LocalDate PROJECT_START = LocalDate.of(2024, 3, 4);
    private static final LocalDate TODAY = LocalDate.of(2024, 4, 22);

    private final Random random = new Random();
    private final JTabbedPane tabs = new JTabbedPane();
    private final JComboBox<Easing> easing = new JComboBox<>(Easing.values());
    private final JSlider duration = new JSlider(200, 3000, 1000);
    private final JLabel durationLabel = new JLabel();
    private final JLabel clicked = new JLabel();
    private final JLabel chartClass = new JLabel();
    private final JLabel chartPackage = new JLabel();
    private final JTextArea chartDescription = new JTextArea();
    private final JTextArea chartExample = new JTextArea();
    private final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    // whether the charts have a title, which is one option for all of them
    private boolean titled;
    // one style for every chart, so the sidebar restyles them all at once
    private final ChartStyle chartStyle = new ChartStyle();
    // a button per palette, showing its colors, four to a row
    private final JPanel palettes = new JPanel(new MigLayout("wrap 4,fillx", "[grow,fill,sg][grow,fill,sg][grow,fill,sg][grow,fill,sg]"));
    private LineChart lineChart;
    private LineChart timeChart;
    private BarChart barChart;
    private PieChart pieChart;
    private CandlestickChart candleChart;
    private GanttChart ganttChart;
    private SpiderWebChart spiderChart;
    private DifferenceChart differenceChart;
    private DeviationChart deviationChart;
    private BubbleChart bubbleChart;
    private PolarChart polarChart;
    private FlowChart flowChart;
    private List<AnimatedChart> charts;
    // grid of the sidebar: a label, a control and the duration's value; headers and option groups span all three
    private final JPanel sidebar = card("Customize", "wrap 3", "[right,pref!][grow,fill][60!,right]");

    public Main() {
        super("Animated JFreeChart");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        init();
        pack();
        // taller by what the tab with the most options needs over the first tab, which the window was packed for
        int first = sidebar.getPreferredSize().height;
        int tallest = first;
        for (int tab = tabs.getTabCount() - 1; tab >= 0; tab--) {
            tabs.setSelectedIndex(tab);
            tallest = Math.max(tallest, sidebar.getPreferredSize().height);
        }
        setSize(getWidth(), getHeight() + tallest - first);
        // the chart over 60% of the height, its description and example under it; a split pane needs its size for that
        validate();
        split.setDividerLocation(CHART_SHARE);
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    private void init() {
        // more tabs than fit in a row scroll, instead of wrapping onto a second row
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.addTab("Line", chartPage(lineChart()));
        tabs.addTab("Time Series", chartPage(timeChart()));
        tabs.addTab("Bar", chartPage(barChart()));
        tabs.addTab("Pie", chartPage(pieChart()));
        tabs.addTab("Candlestick", chartPage(candleChart()));
        tabs.addTab("Gantt", chartPage(ganttChart()));
        tabs.addTab("Spider Web", chartPage(spiderChart()));
        tabs.addTab("Difference", chartPage(differenceChart()));
        tabs.addTab("Deviation", chartPage(deviationChart()));
        tabs.addTab("Bubble", chartPage(bubbleChart()));
        tabs.addTab("Polar", chartPage(polarChart()));
        tabs.addTab("Flow", chartPage(flowChart()));
        charts = List.of(lineChart, timeChart, barChart, pieChart, candleChart, ganttChart, spiderChart, differenceChart, deviationChart,
                bubbleChart, polarChart, flowChart);
        charts.forEach(c -> c.setStyle(chartStyle));
        clicked.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground");
        // a click names what was clicked: its popup's title, or its first row where the popup has no title
        charts.forEach(c -> c.addChartClickListener(e -> clicked.setText("Clicked: "
                + (e.title() != null ? e.title() : e.rows().getFirst().name()))));
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < PALETTES.length; i++) {
            int[] colors = PALETTE_COLORS[i];
            JToggleButton button = new JToggleButton(new PaletteIcon(colors), i == 0);
            button.setToolTipText(PALETTES[i]);
            // flat like a toolbar's buttons: no border, and a background only when hovered or selected
            button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
            button.addActionListener(e -> chartStyle.setPalette(Arrays.stream(colors).mapToObj(Color::new).toArray(Color[]::new)));
            group.add(button);
            palettes.add(button);
        }
        tabs.addChangeListener(e -> {
            showInfo();
            buildSidebar();
            selectedChart().playIntro();
        });

        // easing and duration are shared by every chart
        easing.setSelectedItem(Easing.EASE_OUT);
        easing.addActionListener(e -> {
            charts.forEach(c -> c.setEasing((Easing) easing.getSelectedItem()));
            selectedChart().playIntro();
        });
        duration.addChangeListener(e -> {
            charts.forEach(c -> c.setDuration(duration.getValue()));
            durationLabel.setText(duration.getValue() + " ms");
        });
        durationLabel.setText(duration.getValue() + " ms");
        buildSidebar();

        // under the chart, what the chart is: its class, its package and a description
        chartClass.putClientProperty(FlatClientProperties.STYLE, "font: bold +2");
        chartPackage.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground");
        chartDescription.setEditable(false);
        chartDescription.setLineWrap(true);
        chartDescription.setWrapStyleWord(true);
        chartDescription.setOpaque(false);
        chartDescription.setBorder(null);
        chartExample.setEditable(false);
        chartExample.putClientProperty(FlatClientProperties.STYLE_CLASS, "monospaced");
        // the example takes the height the texts above it leave
        JPanel info = new JPanel(new MigLayout("wrap,fill", "[grow,fill]", "[][][][grow,fill]"));
        info.add(chartClass);
        info.add(chartPackage);
        info.add(chartDescription, "wmin 0");
        info.add(new JScrollPane(chartExample), "wmin 0,hmin 0");
        // it gets its share of the window's height, without making the window any taller for its example
        info.setPreferredSize(new Dimension());
        info.setMinimumSize(new Dimension());
        showInfo();

        // the divider between them drags up to see the chart at a small height
        split.setTopComponent(tabs);
        split.setBottomComponent(info);
        split.setContinuousLayout(true);
        // the chart keeps 60% of the height when the window is resized, and can be made little taller than its tabs
        split.setResizeWeight(CHART_SHARE);
        tabs.setMinimumSize(UIScale.scale(new Dimension(0, 80)));

        JPanel panel = new JPanel(new MigLayout("insets 16", "[fill,grow][fill]", "[fill,grow]"));
        panel.add(split);
        panel.add(sidebar);
        add(panel);
    }

    private LineChart lineChart() {
        lineChart = new LineChart(MONTHS);
        lineChart.addSeries("Revenue", null, 42, 55, 48, 63, 58, 72, 66, 80, 74, 86, 79, 92);
        lineChart.addSeries("Expenses", null, 30, 38, 35, 44, 51, 46, 53, 49, 58, 55, 63, 60);
        lineChart.setValueFormatter(value -> "$" + Math.round(value) + "k");
        return lineChart;
    }

    /**
     * A dense line: one straight-segment series over 50 days, without points.
     */
    private LineChart timeChart() {
        String[] dates = new String[VISITS.length];
        for (int i = 0; i < dates.length; i++) {
            dates[i] = LocalDate.of(2000, 6, 5).plusDays(i).toString();
        }
        timeChart = new LineChart(dates);
        timeChart.setValueRange(0, 350);
        timeChart.setLineStyle(LineChart.LineStyle.STRAIGHT);
        timeChart.setPointsVisible(false);
        timeChart.setValueGradient(true);
        timeChart.addSeries("Visits", null, VISITS);
        return timeChart;
    }

    private BarChart barChart() {
        barChart = new BarChart(DAYS);
        barChart.addSeries("Desktop", null, 62, 75, 58, 81, 70, 45, 38);
        barChart.addSeries("Mobile", null, 48, 52, 66, 60, 72, 80, 85);
        return barChart;
    }

    private PieChart pieChart() {
        pieChart = new PieChart();
        pieChart.addSlice("Direct", null, 38);
        pieChart.addSlice("Search", null, 27);
        pieChart.addSlice("Social", null, 18);
        pieChart.addSlice("Referral", null, 11);
        pieChart.addSlice("Email", null, 6);
        return pieChart;
    }

    /**
     * 40 days of prices, the same every run.
     */
    private CandlestickChart candleChart() {
        String[] days = new String[40];
        for (int i = 0; i < days.length; i++) {
            days[i] = LocalDate.of(2024, 3, 1).plusDays(i).format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH));
        }
        candleChart = new CandlestickChart(days);
        candleChart.setValueRange(60, 180);
        setCandles(candleChart, new Random(7));
        candleChart.addMovingAverage(5, new Color(0xf59e0b));
        candleChart.addMovingAverage(20, new Color(0x8b5cf6));
        return candleChart;
    }

    /**
     * A random walk of candles: each opens where the last closed, with wicks beyond the body and more volume on big moves.
     */
    private static void setCandles(CandlestickChart chart, Random random) {
        int count = chart.getCategoryCount();
        double[] open = new double[count];
        double[] high = new double[count];
        double[] low = new double[count];
        double[] close = new double[count];
        double[] volume = new double[count];
        double price = 105 + random.nextInt(30);
        for (int i = 0; i < count; i++) {
            open[i] = price;
            // pulled gently back toward 120, so the walk stays inside the value range
            close[i] = Math.clamp(Math.round(price + random.nextGaussian() * 7 + (120 - price) * 0.1), 75, 165);
            high[i] = Math.max(open[i], close[i]) + 1 + random.nextInt(7);
            low[i] = Math.min(open[i], close[i]) - 1 - random.nextInt(7);
            volume[i] = 1200 + random.nextInt(2400) + Math.abs(close[i] - open[i]) * 220;
            price = close[i];
        }
        chart.setCandles(open, high, low, close, volume);
    }

    /**
     * A project plan in four groups, with its dependencies, the same every run.
     */
    private GanttChart ganttChart() {
        ganttChart = new GanttChart(TASKS);
        ganttChart.setDateFormat(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH));
        ganttChart.setRange(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 6, 24));
        ganttChart.addGroup("Planning", null, 0, 1);
        ganttChart.addGroup("Design", null, 2, 3);
        ganttChart.addGroup("Development", null, 4, 5, 6);
        ganttChart.addGroup("Release", null, 7, 8, 9);
        for (int task = 0; task < TASK_AFTER.length; task++) {
            for (int before : TASK_AFTER[task]) {
                ganttChart.addDependency(before, task);
            }
        }
        ganttChart.setToday(TODAY);
        setTasks(ganttChart, new Random(7));
        return ganttChart;
    }

    private SpiderWebChart spiderChart() {
        spiderChart = new SpiderWebChart("Speed", "Power", "Range", "Defense", "Agility", "Stamina");
        spiderChart.addSeries("Striker", null, 88, 72, 55, 40, 80, 64);
        spiderChart.addSeries("Guardian", null, 48, 60, 70, 90, 45, 82);
        return spiderChart;
    }

    /**
     * Sales against their target, which is the dashed line.
     */
    private DifferenceChart differenceChart() {
        differenceChart = new DifferenceChart(MONTHS);
        differenceChart.addSeries("Actual", null, 52, 46, 48, 38, 33, 27, 14, 19, 33, 36, 28, 16);
        differenceChart.addSeries("Target", null, 27, 41, 49, 53, 41, 28, 31, 46, 60, 68, 83, 87);
        differenceChart.setSeriesDashed(1, true);
        differenceChart.setPointsVisible(false);
        differenceChart.setValueFormatter(value -> "$" + Math.round(value) + "k");
        return differenceChart;
    }

    /**
     * Two forecasts, each with a band of uncertainty.
     */
    private DeviationChart deviationChart() {
        deviationChart = new DeviationChart(MONTHS);
        deviationChart.setPointsVisible(false);
        setBands(deviationChart, new Random(7), false);
        return deviationChart;
    }

    /**
     * Products in three lines: their price, their rating and, as the size, their sales.
     */
    /**
     * Two curves with a radius every 10 degrees.
     */
    private PolarChart polarChart() {
        polarChart = new PolarChart(36);
        setCurves(polarChart, new Random(7), false);
        return polarChart;
    }

    /**
     * A random closed curve per series: a few waves around the circle, so it ends where it starts.
     */
    private static void setCurves(PolarChart chart, Random random, boolean update) {
        String[] names = {"Signal", "Noise"};
        for (int s = 0; s < names.length; s++) {
            double[] values = new double[chart.getCategoryCount()];
            double base = s == 0 ? 55 : 28;
            double[] phases = {random.nextDouble() * Math.PI * 2, random.nextDouble() * Math.PI * 2, random.nextDouble() * Math.PI * 2};
            double[] sizes = {8 + random.nextInt(14), 4 + random.nextInt(12), random.nextInt(8)};
            for (int i = 0; i < values.length; i++) {
                double angle = Math.PI * 2 * i / values.length;
                double value = base;
                for (int wave = 0; wave < sizes.length; wave++) {
                    value += sizes[wave] * Math.cos((wave + 1) * angle + phases[wave]);
                }
                values[i] = Math.clamp(value, 5, 95);
            }
            if (update) {
                chart.setValues(s, values);
            } else {
                chart.addSeries(names[s], null, values);
            }
        }
    }

    /**
     * Visits of a site: where they come from, the page they land on and how they end.
     */
    private FlowChart flowChart() {
        flowChart = new FlowChart();
        setFlows(flowChart, new Random(7), false);
        return flowChart;
    }

    /**
     * Random visits from every channel to every page, and each page's visits shared out over the outcomes.
     */
    private static void setFlows(FlowChart chart, Random random, boolean update) {
        String[] channels = {"Search", "Social", "Direct", "Email"};
        String[] pages = {"Landing", "Product", "Blog"};
        String[] outcomes = {"Purchase", "Signup", "Bounce"};
        double[] visits = new double[pages.length];
        for (String channel : channels) {
            for (int p = 0; p < pages.length; p++) {
                double value = 8 + random.nextInt(40);
                visits[p] += value;
                setFlow(chart, 0, channel, pages[p], value, update);
            }
        }
        for (int p = 0; p < pages.length; p++) {
            int[] shares = {1 + random.nextInt(9), 1 + random.nextInt(9), 1 + random.nextInt(9)};
            for (int o = 0; o < outcomes.length; o++) {
                setFlow(chart, 1, pages[p], outcomes[o], Math.round(visits[p] * shares[o] / (shares[0] + shares[1] + shares[2])), update);
            }
        }
    }

    private static void setFlow(FlowChart chart, int column, String from, String to, double value, boolean update) {
        if (update) {
            chart.setFlow(column, from, to, value);
        } else {
            chart.addFlow(column, from, to, value);
        }
    }

    private BubbleChart bubbleChart() {
        bubbleChart = new BubbleChart();
        bubbleChart.setNames("Price", "Rating", "Sales");
        bubbleChart.setXFormatter(value -> "$" + Math.round(value));
        bubbleChart.setSizeFormatter(value -> Math.round(value) + "k");
        setBubbles(bubbleChart, new Random(7), false);
        return bubbleChart;
    }

    /**
     * Random bubbles per series, between five and seven of them, labeled with the series' initial and a number.
     */
    private static void setBubbles(BubbleChart chart, Random random, boolean update) {
        String[] names = {"Hardware", "Software", "Services"};
        for (int s = 0; s < names.length; s++) {
            int count = 5 + random.nextInt(3);
            double[] x = new double[count];
            double[] y = new double[count];
            double[] size = new double[count];
            String[] labels = new String[count];
            for (int i = 0; i < count; i++) {
                x[i] = 10 + random.nextInt(81);
                y[i] = 14 + random.nextInt(73);
                size[i] = 6 + random.nextInt(95);
                labels[i] = names[s].substring(0, 1) + (i + 1);
            }
            if (update) {
                chart.setBubbles(s, x, y, size);
            } else {
                chart.addSeries(names[s], null, x, y, size);
            }
            chart.setLabels(s, labels);
        }
    }

    /**
     * A random walk per series, inside a band that widens toward the end of the year like a forecast's.
     */
    private static void setBands(DeviationChart chart, Random random, boolean update) {
        String[] names = {"North", "South"};
        for (int s = 0; s < names.length; s++) {
            int count = chart.getCategoryCount();
            double[] values = new double[count];
            double[] low = new double[count];
            double[] high = new double[count];
            double value = s == 0 ? 62 : 30;
            for (int i = 0; i < count; i++) {
                value = Math.clamp(value + random.nextInt(13) - 5, 18, 82);
                values[i] = value;
                low[i] = value - 3 - i * 0.8 - random.nextInt(3);
                high[i] = value + 3 + i * 0.8 + random.nextInt(3);
            }
            if (update) {
                chart.setValues(s, values, low, high);
            } else {
                chart.addSeries(names[s], null, values, low, high);
            }
        }
    }

    /**
     * A random schedule of the plan: each task starts once the ones it waits for have ended, and is done up to about today.
     */
    private static void setTasks(GanttChart chart, Random random) {
        int count = chart.getCategoryCount();
        LocalDate[] start = new LocalDate[count];
        LocalDate[] end = new LocalDate[count];
        double[] progress = new double[count];
        for (int i = 0; i < count; i++) {
            start[i] = PROJECT_START;
            for (int before : TASK_AFTER[i]) {
                if (end[before].isAfter(start[i])) {
                    start[i] = end[before];
                }
            }
            start[i] = start[i].plusDays(TASK_SLACK[i]);
            // a milestone stays one
            int days = TASK_DAYS[i] == 0 ? 0 : TASK_DAYS[i] + random.nextInt(5) - 2;
            end[i] = start[i].plusDays(days);
            // a little behind the share of its days that have passed
            double passed = days > 0 ? (double) ChronoUnit.DAYS.between(start[i], TODAY) / days : 0;
            progress[i] = passed <= 0 ? 0 : Math.clamp(passed - random.nextDouble() * 0.25, 0.05, 1);
        }
        chart.setTasks(start, end, progress);
    }

    private static JPanel chartPage(AnimatedChart chart) {
        // more room above and below the chart than beside it
        JPanel page = new JPanel(new MigLayout("insets 20 10 20 10", "[fill,grow]", "[fill,grow]"));
        chart.setPreferredSize(UIScale.scale(new Dimension(720, 400)));
        page.add(chart);
        return page;
    }

    private AnimatedChart selectedChart() {
        return charts.get(tabs.getSelectedIndex());
    }

    /**
     * Shows the class and package of the selected chart, with the description of its tab.
     */
    private void showInfo() {
        Class<?> type = selectedChart().getClass();
        chartClass.setText(type.getSimpleName());
        chartPackage.setText(type.getPackageName());
        chartDescription.setText(DESCRIPTIONS[tabs.getSelectedIndex()]);
        chartExample.setText(EXAMPLES[tabs.getSelectedIndex()]);
        chartExample.setCaretPosition(0);
    }

    /**
     * Fills the sidebar with the shared controls and the options of the selected chart, in titled sections.
     */
    private void buildSidebar() {
        sidebar.removeAll();
        AnimatedChart chart = selectedChart();
        addHeader("Animation");
        // the chart's options in three groups: its shape, what it draws, and what hovering does
        List<JCheckBox> type = new ArrayList<>();
        List<JCheckBox> show = new ArrayList<>();
        List<JCheckBox> hover = new ArrayList<>();
        switch (chart) {
            case LineChart line -> {
                addIntro(LineChart.Intro.values(), line.getIntro(), line::setIntro);
                JComboBox<LineChart.LineStyle> style = new JComboBox<>(LineChart.LineStyle.values());
                style.setSelectedItem(line.getLineStyle());
                style.addActionListener(e -> line.setLineStyle(style.getItemAt(style.getSelectedIndex())));
                sidebar.add(new JLabel("Line"));
                sidebar.add(style, "span 2");

                // what a line's area is depends on the kind of line chart
                String area = line instanceof DifferenceChart ? "Difference" : line instanceof DeviationChart ? "Band" : "Area fill";
                show.add(option(area, line.isAreaFilled(), line::setAreaFilled));
                show.add(option("Points", line.isPointsVisible(), line::setPointsVisible));
                // dashes the last series
                int last = line.getSeriesCount() - 1;
                show.add(option("Dashed", line.isSeriesDashed(last), b -> line.setSeriesDashed(last, b)));
                show.add(option("Value gradient", line.isValueGradient(), line::setValueGradient));
            }
            case BarChart bar -> {
                addIntro(BarChart.Intro.values(), bar.getIntro(), bar::setIntro);
                type.add(option("Stacked", bar.isStacked(), b -> {
                    // two series of up to 100 each need twice the range when stacked
                    bar.setValueRange(0, b ? 200 : 100);
                    bar.setStacked(b);
                }));
                type.add(option("Horizontal", bar.isHorizontal(), bar::setHorizontal));
                show.add(option("Rounded corners", bar.getCornerRadius() > 0, b -> bar.setCornerRadius(b ? 6 : 0)));
                hover.add(option("Highlight", bar.isHoverHighlight(), bar::setHoverHighlight));
            }
            case PieChart pie -> {
                addIntro(PieChart.Intro.values(), pie.getIntro(), pie::setIntro);
                type.add(option("Donut", pie.isDonut(), pie::setDonut));
                type.add(option("Half circle", pie.isHalf(), pie::setHalf));
                type.add(option("Rose", pie.isRose(), pie::setRose));
                show.add(option("Rounded corners", pie.getCornerRadius() > 0, b -> pie.setCornerRadius(b ? 8 : 0)));
                show.add(option("Wide gap", pie.getSliceGap() > 2, b -> pie.setSliceGap(b ? 10 : 2)));
                show.add(option("Labels", pie.isLabelsVisible(), pie::setLabelsVisible));
                hover.add(option("Highlight", pie.isHoverHighlight(), pie::setHoverHighlight));
            }
            case CandlestickChart candles -> {
                addIntro(CandlestickChart.Intro.values(), candles.getIntro(), candles::setIntro);
                JComboBox<CandlestickChart.CandleStyle> style = new JComboBox<>(CandlestickChart.CandleStyle.values());
                style.setSelectedItem(candles.getCandleStyle());
                style.addActionListener(e -> candles.setCandleStyle(style.getItemAt(style.getSelectedIndex())));
                sidebar.add(new JLabel("Style"));
                sidebar.add(style, "span 2");

                show.add(option("Volume", candles.isVolumeVisible(), candles::setVolumeVisible));
                show.add(option("Price line", candles.isPriceLineVisible(), candles::setPriceLineVisible));
                hover.add(option("Crosshair", candles.isCrosshair(), candles::setCrosshair));
                hover.add(option("Highlight", candles.isHoverHighlight(), candles::setHoverHighlight));
            }
            case GanttChart gantt -> {
                addIntro(GanttChart.Intro.values(), gantt.getIntro(), gantt::setIntro);
                show.add(option("Progress", gantt.isProgressVisible(), gantt::setProgressVisible));
                show.add(option("Labels", gantt.isLabelsVisible(), gantt::setLabelsVisible));
                show.add(option("Dependencies", gantt.isDependenciesVisible(), gantt::setDependenciesVisible));
                show.add(option("Today line", gantt.getToday() != null, b -> gantt.setToday(b ? TODAY : null)));
                hover.add(option("Highlight", gantt.isHoverHighlight(), gantt::setHoverHighlight));
            }
            case SpiderWebChart spider -> {
                addIntro(SpiderWebChart.Intro.values(), spider.getIntro(), spider::setIntro);
                type.add(option("Circular", spider.isCircular(), spider::setCircular));
                show.add(option("Area fill", spider.isAreaFilled(), spider::setAreaFilled));
                show.add(option("Points", spider.isPointsVisible(), spider::setPointsVisible));
                show.add(option("Value labels", spider.isValueLabelsVisible(), spider::setValueLabelsVisible));
            }
            case BubbleChart bubble -> {
                addIntro(BubbleChart.Intro.values(), bubble.getIntro(), bubble::setIntro);
                show.add(option("Labels", bubble.isLabelsVisible(), bubble::setLabelsVisible));
                show.add(option("Outline", bubble.isOutlineVisible(), bubble::setOutlineVisible));
                show.add(option("Axis names", bubble.isAxisNamesVisible(), bubble::setAxisNamesVisible));
                hover.add(option("Highlight", bubble.isHoverHighlight(), bubble::setHoverHighlight));
            }
            case PolarChart polar -> {
                addIntro(PolarChart.Intro.values(), polar.getIntro(), polar::setIntro);
                type.add(option("Smooth", polar.isSmooth(), polar::setSmooth));
                show.add(option("Area fill", polar.isAreaFilled(), polar::setAreaFilled));
                show.add(option("Points", polar.isPointsVisible(), polar::setPointsVisible));
                show.add(option("Value labels", polar.isValueLabelsVisible(), polar::setValueLabelsVisible));
            }
            case FlowChart flow -> {
                addIntro(FlowChart.Intro.values(), flow.getIntro(), flow::setIntro);
                show.add(option("Labels", flow.isLabelsVisible(), flow::setLabelsVisible));
                show.add(option("Values", flow.isValuesVisible(), flow::setValuesVisible));
                hover.add(option("Highlight", flow.isHoverHighlight(), flow::setHoverHighlight));
            }
            default -> {
            }
        }
        hover.add(option("Popup", chart.isHoverPopup(), chart::setHoverPopup));
        // a pie has no grid, and a flow chart neither a grid nor a legend
        if (!(chart instanceof PieChart) && !(chart instanceof FlowChart)) {
            show.add(option("Grid lines", chartStyle.gridVisible(), chartStyle::setGridVisible));
        }
        if (!(chart instanceof FlowChart)) {
            show.add(option("Legend", chartStyle.legendVisible(), chartStyle::setLegendVisible));
        }

        sidebar.add(new JLabel("Easing"));
        sidebar.add(easing, "span 2");

        sidebar.add(new JLabel("Duration"));
        sidebar.add(duration);
        sidebar.add(durationLabel);
        // off, every chart jumps straight to its values
        sidebar.add(option("Animated", chart.isAnimated(), b -> charts.forEach(c -> c.setAnimated(b))), "skip 1,span 2");

        show.add(option("Title", titled, b -> {
            titled = b;
            for (int i = 0; i < charts.size(); i++) {
                charts.get(i).setTitle(b ? tabs.getTitleAt(i) + " chart" : null);
                charts.get(i).setSubtitle(b ? "Sample data, made up for the demo" : null);
            }
        }));
        addGroup("Type", type);
        addGroup("Show", show);
        addGroup("Hover", hover);

        addHeader("Palette");
        sidebar.add(palettes, "span 3,growx");

        JButton replay = new JButton("Replay intro");
        replay.addActionListener(e -> selectedChart().playIntro());
        JButton randomize = new JButton("Randomize");
        randomize.addActionListener(e -> randomize(selectedChart()));
        addHeader("Data");
        sidebar.add(replay, "span 3,split 2,growx");
        sidebar.add(randomize, "growx");

        JButton copy = new JButton("Copy image");
        copy.addActionListener(e -> selectedChart().copyToClipboard());
        JButton save = new JButton("Save image…");
        save.addActionListener(e -> saveImage(selectedChart()));
        sidebar.add(copy, "span 3,split 2,growx");
        sidebar.add(save, "growx");
        // says what was clicked on the chart last
        clicked.setText("Click the chart");
        sidebar.add(clicked, "span 3,growx,wmin 0");

        addGroup("Theme", List.of(darkMode()));
        sidebar.revalidate();
        sidebar.repaint();
    }

    /**
     * Asks for a file and saves the chart to it as an image, at twice its size on screen.
     */
    private void saveImage(AnimatedChart chart) {
        // the operating system's own dialog
        SystemFileChooser chooser = new SystemFileChooser();
        chooser.addChoosableFileFilter(new SystemFileChooser.FileNameExtensionFilter("PNG image", "png"));
        chooser.addChoosableFileFilter(new SystemFileChooser.FileNameExtensionFilter("JPEG image", "jpg", "jpeg"));
        chooser.setSelectedFile(new File(tabs.getTitleAt(tabs.getSelectedIndex()).toLowerCase().replace(' ', '-') + ".png"));
        if (chooser.showSaveDialog(this) != SystemFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try {
            chart.saveAsImage(file.getName().contains(".") ? file : new File(file.getPath() + ".png"), 2);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Save image", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Section header across the sidebar: a small muted caption followed by a line.
     */
    private void addHeader(String title) {
        JLabel caption = new JLabel(title);
        caption.putClientProperty(FlatClientProperties.STYLE, "font: -1; foreground: $Label.disabledForeground");
        sidebar.add(caption, "span 3,split 2");
        sidebar.add(new JSeparator(), "growx");
    }

    /**
     * A section of options under its header, two per row; nothing if the chart has none in the group.
     */
    private void addGroup(String title, List<JCheckBox> options) {
        if (options.isEmpty()) {
            return;
        }
        addHeader(title);
        // two equal columns, so the options line up from one group to the next
        JPanel grid = new JPanel(new MigLayout("wrap 2,fillx", "[grow,sg][grow,sg]"));
        options.forEach(grid::add);
        sidebar.add(grid, "span 3,growx");
    }

    /**
     * Intro choice for the selected chart; picking one plays it.
     */
    private <T> void addIntro(T[] intros, T selected, Consumer<T> setter) {
        JComboBox<T> intro = new JComboBox<>(intros);
        intro.setSelectedItem(selected);
        intro.addActionListener(e -> {
            setter.accept(intro.getItemAt(intro.getSelectedIndex()));
            selectedChart().playIntro();
        });
        sidebar.add(new JLabel("Intro"));
        sidebar.add(intro, "span 2");
    }

    /**
     * New random values for every series, animated from the current ones.
     */
    private void randomize(AnimatedChart chart) {
        if (chart instanceof CandlestickChart candles) {
            // the four values of a candle have to stay consistent with each other
            setCandles(candles, random);
            return;
        }
        if (chart instanceof GanttChart gantt) {
            // the tasks have to keep following the ones they wait for
            setTasks(gantt, random);
            return;
        }
        if (chart instanceof BubbleChart bubble) {
            // bubbles aren't values over categories
            setBubbles(bubble, random, true);
            return;
        }
        if (chart instanceof PolarChart polar) {
            // the curves have to stay closed
            setCurves(polar, random, true);
            return;
        }
        if (chart instanceof FlowChart flow) {
            // what flows into a page has to flow out of it
            setFlows(flow, random, true);
            return;
        }
        if (chart instanceof DeviationChart deviation) {
            // new bands along with the new values
            setBands(deviation, random, true);
            return;
        }
        for (int s = 0; s < chart.getSeriesCount(); s++) {
            double[] values = new double[chart.getValues(s).length];
            double value = 30 + random.nextInt(30);
            for (int i = 0; i < values.length; i++) {
                if (chart == timeChart) {
                    // noise with an occasional spike, on the 0 to 350 range
                    values[i] = 50 + random.nextInt(80) + (random.nextInt(14) == 0 ? 100 + random.nextInt(110) : 0);
                } else {
                    value = Math.clamp(value + random.nextInt(31) - 13, 10, 95);
                    values[i] = value;
                }
            }
            chart.setValues(s, values);
        }
    }

    private static JCheckBox option(String text, boolean selected, Consumer<Boolean> setter) {
        JCheckBox box = new JCheckBox(text, selected);
        box.addActionListener(e -> setter.accept(box.isSelected()));
        return box;
    }

    private static JCheckBox darkMode() {
        JCheckBox dark = new JCheckBox("Dark mode", FlatLaf.isLafDark());
        dark.addActionListener(e -> {
            FlatAnimatedLafChange.showSnapshot();
            if (dark.isSelected()) {
                FlatMacDarkLaf.setup();
            } else {
                FlatMacLightLaf.setup();
            }
            FlatLaf.updateUI();
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        });
        return dark;
    }

    /**
     * Panel holding one group of controls, framed by a titled border.
     */
    private static JPanel card(String title, String layout, String columns) {
        JPanel card = new JPanel(new MigLayout("insets 10," + layout, columns));
        card.setBorder(BorderFactory.createTitledBorder(title));
        return card;
    }

    /**
     * The colors of a palette side by side in a rounded strip; none are those of the chart style's own palette.
     */
    private record PaletteIcon(int[] colors) implements Icon {

        private static final int COUNT = 5;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                FlatUIUtils.setRenderingHints(g2);
                float arc = UIScale.scale(6f);
                RoundRectangle2D strip = new RoundRectangle2D.Float(x, y, getIconWidth(), getIconHeight(), arc, arc);
                ChartStyle style = new ChartStyle();
                float width = getIconWidth() / (float) COUNT;
                for (int i = 0; i < COUNT; i++) {
                    // each color from its place to the end of the strip, under the next one, so no seam shows between
                    // them; cut out of the strip as a shape and not with a clip, which wouldn't smooth the round ends
                    Area part = new Area(strip);
                    part.intersect(new Area(new Rectangle2D.Float(x + i * width, y, getIconWidth() - i * width, getIconHeight())));
                    g2.setColor(colors.length > 0 ? new Color(colors[i % colors.length]) : style.seriesColor(i));
                    g2.fill(part);
                }
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return UIScale.scale(50);
        }

        @Override
        public int getIconHeight() {
            return UIScale.scale(14);
        }
    }

    /**
     * Installs the demo's font/theme, then shows the window on the Swing event thread.
     */
    public static void main(String[] args) {
        FlatRobotoFont.install();
        UIManager.put("defaultFont", FontUtils.getCompositeFont(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
        FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", "#3d6bff"));
        FlatMacLightLaf.setup();

        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
