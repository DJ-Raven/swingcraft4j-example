package com.swingcraft4j.animatedchart;

/**
 * Hears about clicks on what a chart shows: a category, slice, candle, task, bubble, flow or node.
 */
@FunctionalInterface
public interface ChartClickListener {

    void chartClicked(ChartClickEvent event);
}
