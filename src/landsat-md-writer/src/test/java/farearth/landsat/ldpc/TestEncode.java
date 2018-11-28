/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc;

import farearth.landsat.ldpc.encode.Landsat8LdpcEncoder;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author anton
 */
public class TestEncode {

    @Test
    public void testEncode() throws LdpcException {
        Landsat8LdpcEncoder encoder = new Landsat8LdpcEncoder();
        int decodedByteCount = encoder.getFrameSizes().getDecodedByteCount();
        byte[] decoded = new byte[decodedByteCount];
        for (int n = 0; n < 10; n++) {
            for (int i = 0; i < decoded.length; i++) {
                decoded[i] = (byte) (i + n);
            }
            byte[] encoded1 = encoder.encode(decoded);
            byte[] encoded2 = encoder.encodeSlow(decoded);
            Assert.assertArrayEquals(encoded2, encoded1);
        }
    }
}
