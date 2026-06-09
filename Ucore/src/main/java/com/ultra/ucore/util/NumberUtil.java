package com.ultra.ucore.util;

import java.text.DecimalFormat;

/**
 * Number formatting utilities.
 */
public final class NumberUtil {

    private static final DecimalFormat COMMA_FORMAT = new DecimalFormat("#,###");

    private NumberUtil() {
    }

    public static String comma(long number) {
        return COMMA_FORMAT.format(number);
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
