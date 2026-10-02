package com.swingcraft4j.animatedchart.chart;

import com.formdev.flatlaf.util.UIScale;
import com.swingcraft4j.animatedchart.AnimatedChart;
import com.swingcraft4j.animatedchart.ChartStyle;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.flow.FlowPlot;
import org.jfree.data.flow.DefaultFlowDataset;
import org.jfree.data.flow.FlowKey;
import org.jfree.data.flow.NodeKey;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JFreeChart flow (Sankey) chart: columns of named nodes joined by flows as wide as their values, with an animated
 * intro, animated value changes and hover highlight.
 */
public class FlowChart extends AnimatedChart {

    /**
     * How the chart first appears.
     */
    public enum Intro {
        /**
         * The flows are drawn in from left to right.
         */
        DRAW("Draw"),
        /**
         * Every node and flow grows from nothing at once.
         */
        GROW("Grow");

        private final String name;

        Intro(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * A node as painted.
     */
    private record Node(NodeKey<String> key, Rectangle2D bounds) {
    }

    /**
     * A flow as painted, from the right side of its source to the left side of its destination.
     */
    private record Ribbon(FlowKey<String> key, Path2D shape, double fromX, double toX) {
    }

    // gap between a node and its label, before UI scaling
    private static final float LABEL_GAP = 6;

    // the flows as shown now; it also keeps the nodes of each column in the order they were first used
    private final DefaultFlowDataset<String> dataset = new DefaultFlowDataset<>();
    private final FlowPlot plot = new NodePlot();
    // target value of every flow, in the order they were added
    private final Map<FlowKey<String>, Double> flows = new LinkedHashMap<>();
    // every node's name in the order they were first used, which gives each its color of the style's palette
    private final List<String> names = new ArrayList<>();
    private final Map<String, Color> nodeColors = new HashMap<>();
    // the nodes and flows as last painted
    private List<Node> nodes = List.of();
    private List<Ribbon> ribbons = List.of();
    private Intro intro = Intro.DRAW;
    private float nodeWidth = 14;
    private float nodeGap = 12;
    private float cornerRadius = 3;
    private float flowOpacity = 0.4f;
    private boolean labelsVisible = true;
    private boolean valuesVisible = true;
    private boolean hoverHighlight = true;
    // visible fraction of the width and scale of the flows; both 1 once the intro is done
    private double reveal = 1;
    private double grown = 1;

    public FlowChart() {
        setChart(new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, true));
    }

    /**
     * Adds a flow from a node of a column to a node of the next one; column 0 is the first, and the nodes are
     * created as they are named.
     */
    public void addFlow(int column, String from, String to, double value) {
        flows.put(new FlowKey<>(column, from, to), value);
        for (String name : List.of(from, to)) {
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        dataset.setFlow(column, from, to, value);
    }

    /**
     * Animates a flow to a new value; one the chart doesn't have yet grows from nothing.
     */
    public void setFlow(int column, String from, String to, double value) {
        if (!flows.containsKey(new FlowKey<>(column, from, to))) {
            addFlow(column, from, to, 0);
        }
        flows.put(new FlowKey<>(column, from, to), value);
        valuesChanged();
    }

    /**
     * Value of a flow, or 0 if the chart doesn't have it.
     */
    public double getFlow(int column, String from, String to) {
        return flows.getOrDefault(new FlowKey<>(column, from, to), 0.0);
    }

    /**
     * Number of columns of nodes.
     */
    public int getColumnCount() {
        return flows.isEmpty() ? 0 : dataset.getStageCount() + 1;
    }

    /**
     * Names of the nodes of a column, from top to bottom.
     */
    public List<String> getNodes(int column) {
        return List.copyOf(column == 0 ? dataset.getSources(0) : dataset.getDestinations(column - 1));
    }

    /**
     * Color of the nodes of a name, whatever their column; {@code null} uses the style's palette.
     */
    public void setNodeColor(String node, Color color) {
        nodeColors.put(node, color);
        getChart().fireChartChanged();
    }

    public Color getNodeColor(String node) {
        Color color = nodeColors.get(node);
        return color != null ? color : getStyle().seriesColor(Math.max(0, names.indexOf(node)));
    }

    @Override
    protected void seriesAdded(int series) {
    }

    @Override
    protected void valuesChanged() {
        reveal = 1;
        grown = 1;
        Map<FlowKey<String>, Double> from = new HashMap<>();
        flows.keySet().forEach(key -> from.put(key, shown(key)));
        Map<FlowKey<String>, Double> to = new LinkedHashMap<>(flows);
        // an overshooting easing can dip below 0, which a flow can't be
        animator.start(p -> batch(() -> to.forEach((key, value) -> dataset.setFlow(key.getStage(), key.getSource(),
                key.getDestination(), Math.max(0, lerp(from.get(key), value, p))))));
    }

    @Override
    protected void visibilityChanged(int series) {
    }

    @Override
    public void playIntro() {
        animator.stop();
        batch(() -> flows.forEach((key, value) -> dataset.setFlow(key.getStage(), key.getSource(), key.getDestination(), value)));
        if (intro == Intro.DRAW) {
            animator.start(p -> setIntro(Math.clamp(p, 0, 1), 1));
        } else {
            animator.start(p -> setIntro(1, Math.max(0, p)));
        }
    }

    private void setIntro(double reveal, double grown) {
        this.reveal = reveal;
        this.grown = grown;
        getChart().fireChartChanged();
    }

    public Intro getIntro() {
        return intro;
    }

    public void setIntro(Intro intro) {
        this.intro = intro;
    }

    /**
     * Width of the nodes, before UI scaling.
     */
    public float getNodeWidth() {
        return nodeWidth;
    }

    public void setNodeWidth(float nodeWidth) {
        this.nodeWidth = nodeWidth;
        getChart().fireChartChanged();
    }

    /**
     * Space between the nodes of a column, before UI scaling.
     */
    public float getNodeGap() {
        return nodeGap;
    }

    public void setNodeGap(float nodeGap) {
        this.nodeGap = nodeGap;
        getChart().fireChartChanged();
    }

    /**
     * Radius of the nodes' corners, before UI scaling; 0 is square.
     */
    public float getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
        getChart().fireChartChanged();
    }

    /**
     * Opacity, from 0 to 1, of the flows, which fade from the color of their source to that of their destination.
     */
    public float getFlowOpacity() {
        return flowOpacity;
    }

    public void setFlowOpacity(float flowOpacity) {
        this.flowOpacity = Math.clamp(flowOpacity, 0, 1);
        getChart().fireChartChanged();
    }

    /**
     * Whether each node has its name beside it.
     */
    public boolean isLabelsVisible() {
        return labelsVisible;
    }

    public void setLabelsVisible(boolean labelsVisible) {
        this.labelsVisible = labelsVisible;
        getChart().fireChartChanged();
    }

    /**
     * Whether each node's label ends with the total that flows through it.
     */
    public boolean isValuesVisible() {
        return valuesVisible;
    }

    public void setValuesVisible(boolean valuesVisible) {
        this.valuesVisible = valuesVisible;
        getChart().fireChartChanged();
    }

    /**
     * Whether hovering a flow or a node dims the flows and nodes that aren't part of it.
     */
    public boolean isHoverHighlight() {
        return hoverHighlight;
    }

    public void setHoverHighlight(boolean hoverHighlight) {
        this.hoverHighlight = hoverHighlight;
        getChart().fireChartChanged();
    }

    /**
     * Value of a flow as shown now, while it animates.
     */
    private double shown(FlowKey<String> key) {
        Number value = dataset.getFlow(key.getStage(), key.getSource(), key.getDestination());
        return value != null ? value.doubleValue() : 0;
    }

    /**
     * What flows through a node: the larger of what comes in and what goes out, shown now or as the target.
     */
    private double total(NodeKey<String> node, boolean target) {
        double in = 0;
        double out = 0;
        for (FlowKey<String> key : flows.keySet()) {
            double value = target ? flows.get(key) : shown(key);
            if (key.getStage() == node.getStage() && key.getSource().equals(node.getNode())) {
                out += value;
            } else if (key.getStage() == node.getStage() - 1 && key.getDestination().equals(node.getNode())) {
                in += value;
            }
        }
        return Math.max(in, out);
    }

    /**
     * Every node of the chart, column by column.
     */
    private List<NodeKey<String>> nodeKeys() {
        List<NodeKey<String>> keys = new ArrayList<>();
        for (int column = 0; column < getColumnCount(); column++) {
            for (String name : getNodes(column)) {
                keys.add(new NodeKey<>(column, name));
            }
        }
        return keys;
    }

    /**
     * Index that hovering goes by: the flows in the order they were added, then the nodes.
     */
    private int indexOf(FlowKey<String> flow) {
        return new ArrayList<>(flows.keySet()).indexOf(flow);
    }

    private int indexOf(NodeKey<String> node) {
        return flows.size() + nodeKeys().indexOf(node);
    }

    /**
     * Opacity of a flow now: dimmed, with hover highlight, unless it or one of its two nodes is hovered.
     */
    private float opacity(FlowKey<String> flow) {
        if (!hoverHighlight) {
            return 1;
        }
        return Math.max(getHoverOpacity(indexOf(flow)), Math.max(
                getHoverOpacity(indexOf(new NodeKey<>(flow.getStage(), flow.getSource()))),
                getHoverOpacity(indexOf(new NodeKey<>(flow.getStage() + 1, flow.getDestination())))));
    }

    /**
     * Opacity of a node now: dimmed, with hover highlight, unless it or one of its flows is in what is hovered.
     */
    private float opacity(NodeKey<String> node) {
        if (!hoverHighlight) {
            return 1;
        }
        float opacity = getHoverOpacity(indexOf(node));
        for (FlowKey<String> key : flows.keySet()) {
            boolean from = key.getStage() == node.getStage() && key.getSource().equals(node.getNode());
            boolean to = key.getStage() == node.getStage() - 1 && key.getDestination().equals(node.getNode());
            if (from || to) {
                opacity = Math.max(opacity, opacity(key));
            }
        }
        return opacity;
    }

    @Override
    protected NumberAxis getValueAxis() {
        return null;
    }

    /**
     * The nodes are named beside them, so the chart has no legend.
     */
    @Override
    protected void setLegendItems(LegendItemCollection items) {
        getChart().getLegend().setVisible(false);
    }

    /**
     * The node at the point, or else the flow.
     */
    @Override
    protected int hoverAt(Point2D point, Rectangle2D dataArea) {
        double reach = UIScale.scale(2f);
        for (Node node : nodes) {
            Rectangle2D bounds = node.bounds;
            if (new Rectangle2D.Double(bounds.getX() - reach, bounds.getY() - reach, bounds.getWidth() + reach * 2,
                    bounds.getHeight() + reach * 2).contains(point)) {
                return indexOf(node.key);
            }
        }
        for (Ribbon ribbon : ribbons.reversed()) {
            if (ribbon.shape.contains(point)) {
                return indexOf(ribbon.key);
            }
        }
        return -1;
    }

    /**
     * Not while the intro is still revealing the flows.
     */
    @Override
    protected boolean isHoverShown(int index, Rectangle2D dataArea) {
        return reveal >= 1 && grown == 1;
    }

    /**
     * The two nodes of a flow, or the name of a node.
     */
    @Override
    protected String hoverTitle(int index) {
        if (index < flows.size()) {
            FlowKey<String> flow = new ArrayList<>(flows.keySet()).get(index);
            return flow.getSource() + " → " + flow.getDestination();
        }
        List<NodeKey<String>> keys = nodeKeys();
        return index - flows.size() < keys.size() ? keys.get(index - flows.size()).getNode() : null;
    }

    /**
     * The value of a flow, or what flows into a node from each node before it and out of it to each node after it.
     */
    @Override
    protected List<HoverRow> hoverRows(int index) {
        if (index < flows.size()) {
            FlowKey<String> flow = new ArrayList<>(flows.keySet()).get(index);
            return List.of(new HoverRow(getNodeColor(flow.getSource()), "Value", formatValue(flows.get(flow))));
        }
        List<NodeKey<String>> keys = nodeKeys();
        if (index - flows.size() >= keys.size()) {
            return List.of();
        }
        NodeKey<String> node = keys.get(index - flows.size());
        List<HoverRow> rows = new ArrayList<>();
        flows.forEach((key, value) -> {
            if (key.getStage() == node.getStage() - 1 && key.getDestination().equals(node.getNode())) {
                rows.add(new HoverRow(getNodeColor(key.getSource()), "From " + key.getSource(), formatValue(value)));
            } else if (key.getStage() == node.getStage() && key.getSource().equals(node.getNode())) {
                rows.add(new HoverRow(getNodeColor(key.getDestination()), "To " + key.getDestination(), formatValue(value)));
            }
        });
        return rows;
    }

    /**
     * Places the nodes in their columns, each as tall as what flows through it, and the flows between them.
     */
    private void layout(Rectangle2D area) {
        List<Node> nodes = new ArrayList<>();
        List<Ribbon> ribbons = new ArrayList<>();
        this.nodes = nodes;
        this.ribbons = ribbons;
        int columns = getColumnCount();
        double width = UIScale.scale(nodeWidth);
        double gap = UIScale.scale(nodeGap);
        // pixels per unit of flow: as many as fit the column that needs the most room
        double unit = Double.MAX_VALUE;
        for (int column = 0; column < columns; column++) {
            double total = 0;
            for (String name : getNodes(column)) {
                total += total(new NodeKey<>(column, name), false);
            }
            if (total > 0) {
                unit = Math.min(unit, (area.getHeight() - gap * (getNodes(column).size() - 1)) / total);
            }
        }
        if (unit == Double.MAX_VALUE || unit <= 0 || area.getWidth() <= width) {
            return;
        }
        unit *= grown;

        Map<NodeKey<String>, Rectangle2D> bounds = new HashMap<>();
        for (int column = 0; column < columns; column++) {
            List<String> names = getNodes(column);
            double[] heights = new double[names.size()];
            double height = gap * (names.size() - 1);
            for (int i = 0; i < heights.length; i++) {
                heights[i] = total(new NodeKey<>(column, names.get(i)), false) * unit;
                height += heights[i];
            }
            double x = area.getX() + (area.getWidth() - width) * column / Math.max(1, columns - 1);
            // each column centered in the height
            double y = area.getCenterY() - height / 2;
            for (int i = 0; i < heights.length; i++) {
                NodeKey<String> key = new NodeKey<>(column, names.get(i));
                Rectangle2D node = new Rectangle2D.Double(x, y, width, heights[i]);
                bounds.put(key, node);
                nodes.add(new Node(key, node));
                y += heights[i] + gap;
            }
        }

        // how much of each node's right and left side the flows placed so far have taken
        Map<NodeKey<String>, Double> out = new HashMap<>();
        Map<NodeKey<String>, Double> in = new HashMap<>();
        for (int column = 0; column < columns - 1; column++) {
            // in the order of the nodes on both sides, so that the flows of a node don't cross each other
            for (String source : getNodes(column)) {
                for (String destination : getNodes(column + 1)) {
                    FlowKey<String> key = new FlowKey<>(column, source, destination);
                    double height = shown(key) * unit;
                    if (!flows.containsKey(key) || height <= 0) {
                        continue;
                    }
                    NodeKey<String> from = new NodeKey<>(column, source);
                    NodeKey<String> to = new NodeKey<>(column + 1, destination);
                    double fromX = bounds.get(from).getMaxX();
                    double toX = bounds.get(to).getX();
                    double fromY = bounds.get(from).getY() + out.merge(from, height, Double::sum) - height;
                    double toY = bounds.get(to).getY() + in.merge(to, height, Double::sum) - height;
                    double middle = (fromX + toX) / 2;
                    Path2D shape = new Path2D.Double();
                    shape.moveTo(fromX, fromY);
                    shape.curveTo(middle, fromY, middle, toY, toX, toY);
                    shape.lineTo(toX, toY + height);
                    shape.curveTo(middle, toY + height, middle, fromY + height, fromX, fromY + height);
                    shape.closePath();
                    ribbons.add(new Ribbon(key, shape, fromX, toX));
                }
            }
        }
    }

    /**
     * Paints the flows, then the nodes over their ends, then the nodes' labels.
     */
    private void paintPlot(Graphics2D g2, Rectangle2D area) {
        layout(area);
        ChartStyle style = getStyle();
        Graphics2D g = (Graphics2D) g2.create();
        try {
            if (reveal < 1) {
                g.clip(new Rectangle2D.Double(area.getX(), area.getY(), area.getWidth() * reveal, area.getHeight()));
            }
            for (Ribbon ribbon : ribbons) {
                Color from = getNodeColor(ribbon.key.getSource());
                Color to = getNodeColor(ribbon.key.getDestination());
                g.setComposite(AlphaComposite.SrcOver.derive(opacity(ribbon.key)));
                g.setPaint(new GradientPaint((float) ribbon.fromX, 0, ChartStyle.alpha(from, flowOpacity),
                        (float) ribbon.toX, 0, ChartStyle.alpha(to, flowOpacity)));
                g.fill(ribbon.shape);
            }
            g.setFont(style.font());
            FontMetrics metrics = g.getFontMetrics();
            double arc = UIScale.scale(cornerRadius) * 2;
            int last = getColumnCount() - 1;
            for (Node node : nodes) {
                Rectangle2D bounds = node.bounds;
                if (bounds.getHeight() <= 0) {
                    continue;
                }
                g.setComposite(AlphaComposite.SrcOver.derive(opacity(node.key)));
                g.setColor(getNodeColor(node.key.getNode()));
                double round = Math.min(arc, Math.min(bounds.getWidth(), bounds.getHeight()));
                g.fill(new RoundRectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), round, round));
                if (!labelsVisible) {
                    continue;
                }
                // to the right of the node, or to its left in the last column, where there is no room on the right
                String name = node.key.getNode();
                String value = valuesVisible ? "  " + formatValue(total(node.key, true)) : "";
                double labelGap = UIScale.scale(LABEL_GAP);
                double x = node.key.getStage() < last || last == 0 ? bounds.getMaxX() + labelGap
                        : bounds.getX() - labelGap - metrics.stringWidth(name + value);
                float baseline = (float) (bounds.getCenterY() + (metrics.getAscent() - metrics.getDescent()) / 2.0);
                g.setColor(style.foreground());
                g.drawString(name, (float) x, baseline);
                g.setColor(style.mutedForeground());
                g.drawString(value, (float) x + metrics.stringWidth(name), baseline);
            }
        } finally {
            g.dispose();
        }
    }

    /**
     * Flow plot that leaves all its painting to the chart.
     */
    private class NodePlot extends FlowPlot {

        NodePlot() {
            super(FlowChart.this.dataset);
        }

        @Override
        public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor, PlotState parentState, PlotRenderingInfo info) {
            getInsets().trim(area);
            if (info != null) {
                info.setPlotArea(area);
                info.setDataArea(area);
            }
            paintPlot(g2, area);
        }
    }
}
