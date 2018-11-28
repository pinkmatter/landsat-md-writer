/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.util;

/**
 * Utilities for bit manipulation.
 *
 * @author anton
 */
public class BitUtils {

    private static final int BITS_IN_BYTE = 8;

    public static int getByteIndex(int bitNum) {
        return bitNum / BITS_IN_BYTE;
    }

    public static int getPositionInByte(int bitNum) {
        return BITS_IN_BYTE - (bitNum % BITS_IN_BYTE) - 1;
    }

    public static int getBit(byte[] bytes, int bitNum) {
        int index = bitNum / BITS_IN_BYTE;
        int pos = BITS_IN_BYTE - (bitNum % BITS_IN_BYTE) - 1;
        return bytes[index] >> pos & 1;
    }

    public static void setBit(byte[] bytes, int bitNum) {
        int index = bitNum / BITS_IN_BYTE;
        int pos = BITS_IN_BYTE - (bitNum % BITS_IN_BYTE) - 1;
        bytes[index] |= 1 << pos;
    }

    public static void flipBit(byte[] bytes, int bitNum) {
        int index = bitNum / BITS_IN_BYTE;
        int pos = BITS_IN_BYTE - (bitNum % BITS_IN_BYTE) - 1;
        bytes[index] ^= 1 << pos;
    }

    public static int getBit(byte[] bytes, int bitNum, int initialVirtualZeroBits) {
        return bitNum < initialVirtualZeroBits ? 0 : getBit(bytes, bitNum - initialVirtualZeroBits);
    }

    public static int getRequiredBytes(int bits) {
        return (int) Math.ceil(1.0 * bits / BITS_IN_BYTE);
    }

    public static int[] expand(byte[] bytes, int startBit, int bitCount) {
        return expand(bytes, startBit, bitCount, 0);
    }

    public static int[] expand(byte[] bytes, int startBit, int bitCount, int prependZeroCount) {
        int[] bits = new int[bitCount + prependZeroCount];
        for (int i = 0; i < bitCount && i + startBit < 8 * bytes.length; i++) {
            // Don't put these operations in additional methods for performance reasons
            int bitNum = i + startBit;
            int index = bitNum / BITS_IN_BYTE;
            int pos = BITS_IN_BYTE - (bitNum % BITS_IN_BYTE) - 1;
            bits[i + prependZeroCount] = bytes[index] >> pos & 1;
        }
        return bits;
    }

    public static byte[] compact(int[] bits, int startBit, int byteCount) {
        byte[] bytes = new byte[byteCount];
        int bitCount = byteCount * BITS_IN_BYTE;
        for (int i = 0; i < bitCount && i + startBit < bits.length; i++) {
            int bit = bits[i + startBit];
            if (bit != 0) {
                setBit(bytes, i);
            }
        }
        return bytes;
    }

    public static String toString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(toString(b));
        }
        return sb.toString();
    }

    public static String toString(byte b) {
        return String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0').replace('0', '.');
    }

    public static String toStringExpanded(int[] bits) {
        StringBuilder sb = new StringBuilder();
        for (int b : bits) {
            sb.append(b == 0 ? "." : "1");
        }
        return sb.toString();
    }

    public static int[] expandFromHexString(String hex, int startBit, int bitCount) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) ((parseHex(hex, i) << 4) + parseHex(hex, i + 1));
        }
        return BitUtils.expand(bytes, startBit, bitCount);
    }

    private static int parseHex(String str, int index) {
        return Character.digit(str.charAt(index), 16);
    }
}
