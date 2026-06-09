package com.ultra.ucore.util;

import java.time.Duration;

/**
 * Time utility methods.
 */
public final class TimeUtil {

    private TimeUtil() {
    }

    public static String format(Duration duration) {
        long seconds = duration.toSeconds();

        long days = seconds / 86_400;
        seconds %= 86_400;

        long hours = seconds / 3_600;
        seconds %= 3_600;

        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder builder = new StringBuilder();

        if (days > 0) builder.append(days).append("d ");
        if (hours > 0) builder.append(hours).append("h ");
        if (minutes > 0) builder.append(minutes).append("m ");
        if (seconds > 0 || builder.isEmpty()) builder.append(seconds).append("s");

        return builder.toString().trim();
    }
}
