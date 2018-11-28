/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.util;

/**
 *
 * @author anton
 */
public class ByteStringUtils {

    private ByteStringUtils() {
    }

    public static String bytesToCompactHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        int zeros = 0;
        for (byte b : in) {
            if (b == 0) {
                zeros++;
            } else {
                if (zeros != 0) {
                    appendZeros(zeros, builder);
                    zeros = 0;
                }
                builder.append(String.format("%02x", b));
            }
        }
        if (zeros != 0) {
            appendZeros(zeros, builder);
        }
        return builder.toString();
    }

    private static void appendZeros(int zeros, final StringBuilder builder) {
        if (zeros > 5) {
            builder.append("...0{").append(zeros * 2).append("}...");
        } else {
            for (int i = 0; i < zeros; i++) {
                builder.append("00");
            }
        }
    }
}
