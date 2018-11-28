/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc;

import farearth.landsat.ldpc.decode.Landsat8LdpcDecoderFast;
import farearth.landsat.ldpc.decode.LdpcDecodeResult;
import farearth.landsat.ldpc.decode.ThreadedLdpcDecoder;
import farearth.landsat.ldpc.encode.Landsat8LdpcEncoder;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author anton
 */
public class TestThreadedDecoder {

    private final Landsat8LdpcFrameSizes frameSizes = new Landsat8LdpcFrameSizes();
    private final Landsat8LdpcEncoder encoder = new Landsat8LdpcEncoder();
    private final Landsat8LdpcDecoderFast fastDecoder = new Landsat8LdpcDecoderFast();
    private final ThreadedLdpcDecoder threadedDecoder = new ThreadedLdpcDecoder(fastDecoder);

    @Test
    public void testThreadedDecoder() throws LdpcException {
        int innerLoop = 10;
        int outerLoop = 2;
        byte[][][] frames = new byte[outerLoop][][];
        byte[][][] encoded = new byte[outerLoop][][];
        for (int i = 0; i < outerLoop; i++) {
            System.out.println("Generating & encoding " + i + "x" + innerLoop);
            frames[i] = LdpcTestUtils.generatePseudoRandomInfoFrames(innerLoop, frameSizes.getDecodedByteCount());
            encoded[i] = LdpcTestUtils.encode(encoder, frames[i], false);
        }

        for (int i = 0; i < outerLoop; i++) {
            System.out.println("Decoding " + i + "x" + innerLoop);
            byte[][] enc = encoded[i];
            List<LdpcDecodeResult> results = threadedDecoder.decode(Arrays.asList(enc), false);
            for (int j = 0; j < innerLoop; j++) {
                Assert.assertArrayEquals(frames[i][j], results.get(j).getDecodedBytes().get());
            }
        }
    }
}
