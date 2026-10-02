package com.swingcraft4j.animatedchart;

import com.swingcraft4j.animatedchart.AnimatedChart.HoverRow;

import java.awt.event.MouseEvent;
import java.util.List;

/**
 * A click on what a chart shows: the index the chart hovers it by, with the title and rows of its hover popup,
 * which say what it is and its values, and the mouse event of the click.
 */
public record ChartClickEvent(AnimatedChart chart, int index, String title, List<HoverRow> rows, MouseEvent mouse) {
}
