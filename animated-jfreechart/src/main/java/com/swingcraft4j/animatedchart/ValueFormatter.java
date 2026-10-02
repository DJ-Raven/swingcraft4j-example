package com.swingcraft4j.animatedchart;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * Text shown for a value on the value axis and in the hover popup, e.g. with a unit or currency.
 */
@FunctionalInterface
public interface ValueFormatter {

    String format(double value);

    /**
     * This formatter as a {@link NumberFormat}, which is what a JFreeChart axis takes.
     */
    default NumberFormat toNumberFormat() {
        return new NumberFormat() {
            @Override
            public StringBuffer format(double number, StringBuffer text, FieldPosition position) {
                return text.append(ValueFormatter.this.format(number));
            }

            @Override
            public StringBuffer format(long number, StringBuffer text, FieldPosition position) {
                return text.append(ValueFormatter.this.format(number));
            }

            @Override
            public Number parse(String source, ParsePosition position) {
                return null;
            }
        };
    }
}
