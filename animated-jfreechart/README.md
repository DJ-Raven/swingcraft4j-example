# Animated JFreeChart

Animated charts built on [JFreeChart](https://github.com/jfree/jfreechart), styled to match the FlatLaf theme. JFreeChart has no animation of its own, so a small animator drives each chart frame by frame: it changes the dataset, or how much of the plot is revealed, on each tick, and JFreeChart repaints.

## Charts

| Class | Chart |
|---|---|
| `LineChart` | Lines over named categories: smooth, monotone, straight or stepped, with an area fill, points and dashed series. Handles dense data, such as a value per day. |
| `BarChart` | Bars side by side or stacked, upright or horizontal, with rounded corners. |
| `PieChart` | A pie, donut, half circle or rose, with gaps, rounded corners and labels. |
| `CandlestickChart` | Candles, hollow candles, OHLC bars or Heikin-Ashi, with volume bars, moving averages, a line at the last close and a crosshair. |
| `GanttChart` | Tasks over dates, in groups, with progress, milestones, dependency arrows and a today line. |
| `SpiderWebChart` | Series over named axes that meet in a center: a radar chart. |
| `DifferenceChart` | Two lines with the area between them filled, colored by which one is higher. |
| `DeviationChart` | Lines with a band around each, from a low to a high value. |
| `BubbleChart` | Bubbles placed by two values and sized by a third. |
| `PolarChart` | Closed curves of radii at evenly spaced angles. |
| `FlowChart` | Columns of nodes joined by flows as wide as their values: a Sankey chart. |

They all share these features:

- **Animated intro**, played the first time the chart is shown. Each chart has its own intros.
- **Animated value changes**: new values don't jump. Everything moves from what is shown now to its new value, even if a previous change is still running.
- **Six easing curves**: linear, ease out, ease in-out, back, elastic and bounce.
- **Hover popup**: a card with a drop shadow that lists the values under the mouse. It slides after the mouse and fades in and out. Charts that highlight what is hovered ease the highlight in and out too.
- **Clickable legend**: clicking a series' legend item hides or shows it, with an animation.
- **Styling**: fonts, colors, palette, grid and legend come from the FlatLaf theme, update when it changes, and can each be overridden, per chart or for several charts at once.
- **Title, click events and image export** on every chart.
- **Sharp at any UI scale**: the chart is painted straight to the screen, not through a scaled image.

## Requirements

- Java 25.
- [FlatLaf](https://github.com/JFormDesigner/FlatLaf/).
- [JFreeChart](https://github.com/jfree/jfreechart) 1.5.6.

## Usage

### 1. Make a chart

Most charts are made over named categories, and each series has one value per category:

```java
LineChart chart = new LineChart("Jan", "Feb", "Mar", "Apr", "May", "Jun");
chart.addSeries("Revenue", null, 42, 55, 48, 63, 58, 72);                 // a color of the palette
chart.addSeries("Expenses", new Color(0x14b8a6), 30, 38, 35, 44, 51, 46);
panel.add(chart);
```

A series added with a `null` color takes the next color of the style's palette. The value axis is fixed, 0 to 100 by default, so it doesn't rescale while the values move:

```java
chart.setValueRange(0, 500);
chart.setValueFormatter(value -> "$" + Math.round(value) + "k");   // on the axis and in the popup
```

Changing the values animates to them:

```java
chart.setValues(0, 50, 61, 57, 70, 66, 81);   // series 0 moves to the new values
chart.setSeriesVisible(1, false);             // fades series 1 out, as clicking its legend item does
```

### 2. The charts

**Line**

```java
LineChart chart = new LineChart("Jan", "Feb", "Mar", "Apr");
chart.addSeries("Revenue", null, 42, 55, 48, 63);
chart.setLineStyle(LineChart.LineStyle.MONOTONE);   // SMOOTH by default; also STRAIGHT and STEP
chart.setAreaFilled(false);                         // no gradient under the lines
chart.setPointsVisible(false);
chart.setSeriesDashed(0, true);
chart.setLineWidth(3);
```

`SMOOTH` is a round curve that may overshoot the points on spiky data; `MONOTONE` is a curve that never does. With many categories the axis labels only as many as fit, so a value per day works:

```java
LineChart chart = new LineChart(days);          // a name per day
chart.setValueRange(0, 350);
chart.setLineStyle(LineChart.LineStyle.STRAIGHT);
chart.setPointsVisible(false);
chart.setValueGradient(true);                   // the line is colored by value
chart.addSeries("Visits", null, visits);        // a value per day
```

**Bar**

```java
BarChart chart = new BarChart("Mon", "Tue", "Wed");
chart.addSeries("Desktop", null, 62, 75, 58);
chart.addSeries("Mobile", null, 48, 52, 66);
chart.setStacked(true);          // one bar per category
chart.setValueRange(0, 200);     // which needs room for both series
chart.setHorizontal(true);
chart.setCornerRadius(6);        // 0 is square
```

**Pie**

```java
PieChart chart = new PieChart();
chart.addSlice("Direct", null, 38);
chart.addSlice("Search", null, 27);
chart.addSlice("Social", null, 18);
chart.setDonut(true);            // the default: a ring with the total in the middle
chart.setHalf(true);             // the upper half of a circle
chart.setRose(true);             // equal angles, the value shown by the radius
chart.setLabelsVisible(true);
chart.setValues(0, 50);          // animates the first slice to a new value
```

**Candlestick**

```java
CandlestickChart chart = new CandlestickChart("Mar 1", "Mar 2", "Mar 3");
chart.setValueRange(60, 180);
chart.setCandles(open, high, low, close, volume);   // a value of each per category
chart.addMovingAverage(5, null);
chart.setCandleStyle(CandlestickChart.CandleStyle.HOLLOW);   // SOLID by default; also BARS and HEIKIN_ASHI
chart.setColors(Color.BLUE, Color.ORANGE);          // candles that closed up, and down

chart.setCandles(newOpen, newHigh, newLow, newClose, newVolume);   // animates to them
```

**Gantt**

```java
GanttChart chart = new GanttChart("Research", "Design", "Launch");
chart.addGroup("Planning", null, 0);       // a color and a legend item for tasks 0
chart.addGroup("Build", null, 1, 2);       // and for tasks 1 and 2
chart.addDependency(0, 1);                 // an arrow: Design waits for Research
chart.setToday(LocalDate.now());
chart.setTasks(start, end, progress);      // a start, an end and a progress of 0 to 1 per task
```

A task that ends where it starts is a milestone, drawn as a diamond. Calling `setTasks` again animates to the new dates.

**Spider web**

```java
SpiderWebChart chart = new SpiderWebChart("Speed", "Power", "Range", "Defense");
chart.addSeries("Striker", null, 88, 72, 55, 40);
chart.addSeries("Guardian", null, 48, 60, 70, 90);
chart.setMaxValue(100);          // the value at the rim
chart.setCircular(true);         // rings as circles, not polygons
```

**Difference**

A `LineChart` that compares its first series with its second:

```java
DifferenceChart chart = new DifferenceChart("Jan", "Feb", "Mar", "Apr");
chart.addSeries("Actual", null, 38, 52, 47, 66);
chart.addSeries("Target", null, 45, 49, 53, 57);
chart.setColors(Color.GREEN, Color.RED);   // where the first is above, and below
```

**Deviation**

A `LineChart` whose series each have a band:

```java
DeviationChart chart = new DeviationChart("Jan", "Feb", "Mar", "Apr");
chart.addSeries("North", null, values, low, high);   // one of each per category
chart.setValues(0, newValues, newLow, newHigh);      // animates the line and its band
```

The band is kept as a distance from its line, so setting only the values moves the band along.

**Bubble**

A bubble chart has no categories; each series has bubbles of its own:

```java
BubbleChart chart = new BubbleChart();
chart.setXRange(0, 100);
chart.setYRange(0, 100);
chart.setNames("Price", "Rating", "Sales");       // in the popup, and on the axes if shown
chart.addSeries("Hardware", null, x, y, size);    // a bubble per index
chart.setLabels(0, "H1", "H2", "H3");             // inside the bubbles, where they fit
chart.setBubbles(0, newX, newY, newSize);         // moves and resizes; new bubbles grow in
```

Bubbles are sized by area, relative to the largest.

**Polar**

```java
PolarChart chart = new PolarChart(36);     // a radius every 10 degrees, clockwise from the top
chart.addSeries("Signal", null, radii);    // 36 values
chart.setSmooth(false);                    // straight segments
chart.setAngleStep(45);                    // degrees between the labeled angles
```

**Flow**

```java
FlowChart chart = new FlowChart();
chart.addFlow(0, "Search", "Landing", 44);     // from a node of column 0 to one of column 1
chart.addFlow(0, "Social", "Landing", 12);
chart.addFlow(1, "Landing", "Purchase", 27);   // from column 1 to column 2
chart.setFlow(0, "Search", "Landing", 90);     // animates; a flow it doesn't have yet grows in
chart.setNodeColor("Landing", Color.ORANGE);
```

Nodes are created as they are named, and labeled beside them, so the chart has no legend.

### 3. Animation

```java
chart.setIntro(LineChart.Intro.RISE);   // each chart has its own intros
chart.setEasing(Easing.ELASTIC);        // EASE_OUT by default
chart.setDuration(1200);                // milliseconds, 1000 by default
chart.playIntro();                      // play it again

chart.setAnimated(false);               // jump straight to the values instead
chart.setIntroOnShow(false);            // don't play the intro by itself when first shown
chart.setIntroDelay(300);               // or wait before it, e.g. to stagger several charts
```

| Chart | Intros (the default first) |
|---|---|
| `LineChart`, `DifferenceChart`, `DeviationChart` | `DRAW`: drawn in from left to right. `RISE`: rise from the bottom. |
| `BarChart` | `CASCADE`: bars grow one category after another. `GROW`: all at once. |
| `PieChart` | `SWEEP`: swept in clockwise. `EXPAND`: grows from the center. |
| `CandlestickChart` | `DRAW`: drawn in from left to right. `GROW`: every candle grows from its middle. |
| `GanttChart` | `CASCADE`: tasks grow one after another. `GROW`: all at once. `DRAW`: drawn in from left to right. |
| `SpiderWebChart` | `EXPAND`: grows out of the center. `SWEEP`: swept in clockwise. |
| `BubbleChart` | `CASCADE`: bubbles grow one after another, from left to right. `GROW`: all at once. |
| `PolarChart` | `SWEEP`: swept in clockwise. `EXPAND`: grows out of the center. |
| `FlowChart` | `DRAW`: drawn in from left to right. `GROW`: every node and flow grows at once. |

Back and elastic overshoot the target before settling. Where an overshoot can't be drawn, such as a reveal past the edge or a negative size, it is clamped.

### 4. Style

Every chart has a `ChartStyle`. Whatever isn't set, or is set to `null`, follows the FlatLaf theme:

```java
chart.getStyle()
        .setFont(new Font("Inter", Font.PLAIN, 13))
        .setPalette(new Color(0xf97316), new Color(0xec4899), new Color(0xf59e0b))
        .setBackground(new Color(0x14181f))
        .setForeground(new Color(0xe6e8eb))
        .setLegendPosition(RectangleEdge.BOTTOM)
        .setGridVisible(false);
```

One style can be shared, so several charts are restyled at once:

```java
ChartStyle style = new ChartStyle();
lineChart.setStyle(style);
barChart.setStyle(style);
style.setAccent(Color.MAGENTA);   // both charts follow
```

| Setting | What it is |
|---|---|
| `setFont`, `setTitleFont`, `setAxisFont`, `setLegendFont` | Fonts; the last three follow the first unless set. |
| `setPalette` | Colors of the series that have none of their own. The default starts with the accent color. |
| `setAccent`, `setPositive`, `setNegative` | The accent color, and the colors of up and down, as in candles and the difference chart. |
| `setBackground`, `setForeground`, `setMutedForeground` | Fill of the chart, transparent by default, and the colors of its text. |
| `setGridColor`, `setGridVisible` | The grid lines. |
| `setLegendVisible`, `setLegendPosition` | The legend, at the top by default. |
| `setTitleAlignment` | Side the title and subtitle are on, the center by default. |
| `setPopupBackground`, `setPopupBorder` | The hover popup card. |
| `setDimmedOpacity` | How far hovering dims what isn't hovered. |

A series can also be recolored at any time with `chart.setSeriesColor(series, color)`.

### 5. Title, hover and clicks

```java
chart.setTitle("Weekly visits");
chart.setSubtitle("Desktop only");
chart.setHoverPopup(false);        // no popup card on hover

chart.addChartClickListener(event ->
        System.out.println(event.index() + " " + event.title() + " " + event.rows()));
```

A click event is for what hovering has a popup for: a category, slice, candle, task, bubble, flow or node. It carries that index with the popup's title and rows, which say what was clicked and its values.

Line, spider web and polar charts mark the hovered category with a guide line and an enlarged point on each series. Bar, pie, Gantt, bubble and flow charts dim what isn't hovered; `setHoverHighlight(false)` turns that off. A candlestick chart has a crosshair instead, and dims the other candles only if `setHoverHighlight(true)` is set.

### 6. Export

```java
BufferedImage image = chart.toImage(2);             // at twice its size on screen
chart.saveAsImage(new File("chart.png"), 2);        // or .jpg
chart.copyToClipboard();
```

The image is the chart as it looks now, without the hover popup, on the background it is seen on. For anything else, `chart.getChart()` is the JFreeChart.

## How it works

`ChartAnimator` runs a Swing `Timer` at about 60 frames a second. Each tick, it works out how far through the duration it is, runs that through the easing curve, and passes the result (0 to 1) to a callback. The animations are built on it:

- **Value changes, and intros that grow or rise**: each frame sets every value to `from + (to - from) × progress`. The updates are made with `chart.setNotify(false)`, then notification is turned back on, so each frame repaints once instead of once per value.
- **Intros that draw in**: the data stays at its final values. The plot, a small `XYPlot` subclass, clips its data rendering to the left `progress` fraction of the plot area. The axes, grid lines and legend aren't clipped. Sweeps clip to a wedge the same way.
- **Cascades**: the animator passes linear time instead of eased progress. Each item starts a little later than the one before and applies the easing curve to its own share of the time, so every item still finishes its full curve.
- **Hiding a series** is a value change too: lines, curves, bubbles and task groups fade out, bars shrink to the base, and slices close up.

Most charts paint their data themselves, in a renderer or plot built on JFreeChart's: the lines as one path per series, the bars with a `BarPainter`, and the pie, spider web, polar and flow charts in a plot that overrides `draw`. JFreeChart still lays out the chart, and draws the axes, grid lines, title and legend.

The hover popup is a JFreeChart `Overlay` added to the `ChartPanel`, so it's painted with Java2D on top of the chart instead of in a separate window. `ChartHover` listens to the mouse, asks the chart what is at that point, and repaints. The shadow is eight translucent rounded rectangles, each a little larger than the last and shifted down, which stack into a soft edge. A timer moves the card a fraction of the way to its place on every frame, fades it, and eases the highlight from one hovered index to the next. Spider web and polar charts ask for their hovered axis to be kept clear, so the card is put where it doesn't cover the points it is about.

`AnimatedChart` is the base of every chart: it holds the series, the `ChartPanel`, the animator, the hover popup, the title and the intro-on-first-show logic. It applies its `ChartStyle` when the style or the theme changes.

## Key classes

| Package | Class | Role |
|---|---|---|
| `animatedchart` | `AnimatedChart` | Base component of the charts: series, chart panel, animator, title, click events and export. |
| `animatedchart` | `ChartStyle` | Fonts, colors, palette, grid and legend of a chart, following the FlatLaf theme. |
| `animatedchart` | `ChartAnimator` | Timer that passes each frame's eased progress to a callback. |
| `animatedchart` | `Easing` | The easing curves. |
| `animatedchart` | `ChartHover` | Overlay that tracks what is hovered and paints the popup card. |
| `animatedchart` | `ChartClickListener`, `ChartClickEvent` | Clicks on what a chart shows. |
| `animatedchart` | `ValueFormatter` | Text of the values on the value axis and in the popup. |
| `animatedchart.chart` | `LineChart`, `DifferenceChart`, `DeviationChart` | The line chart and the two charts built on it. |
| `animatedchart.chart` | `BarChart`, `PieChart`, `CandlestickChart`, `GanttChart` | The bar, pie, candlestick and Gantt charts. |
| `animatedchart.chart` | `SpiderWebChart`, `PolarChart` | The two charts around a center. |
| `animatedchart.chart` | `BubbleChart`, `FlowChart` | The bubble and flow charts, which have no categories. |

All packages are under `com.swingcraft4j`.
