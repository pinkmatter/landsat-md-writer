/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.util;

/**
 *
 * @author Chris
 */
public class ByteSizeFormatter {

    private ByteSizeFormatter() {
    }

    public static String format(long v) {
        return format(v, 1);
    }

    public static String format(long v, int forcedDecimals) {
        return format(v, "B", forcedDecimals);
    }

    public static String formatBits(long v) {
        return format(v, "b", 1);
    }

    private static String format(long v, String suffix, int forcedDecimals) {
        if (v < 1024) {
            return v + suffix;
        }
        int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
        double number = (double) v / (1L << (z * 10));
        Object unit = z == 0 ? "" : "KMGTPE".charAt(z - 1);
        return String.format("%." + forcedDecimals + "f%s%s", number, unit, suffix);
    }
}
