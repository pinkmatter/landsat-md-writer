/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc;

/**
 *
 * @author anton
 */
public class LdpcArrayChecks {

    public static void checkLength(byte[] bytes, int expectedLength) throws LdpcException {
        checkLength(bytes.length, expectedLength);
    }

    public static void checkLength(int[] ints, int expectedLength) throws LdpcException {
        checkLength(ints.length, expectedLength);
    }

    private static void checkLength(int actualLength, int expectedLength) throws LdpcException {
        if (actualLength != expectedLength) {
            String msg = "Array length should be " + expectedLength + ", but is " + actualLength;
            throw new LdpcException(msg);
        }
    }
}
