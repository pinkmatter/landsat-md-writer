/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc;

import farearth.landsat.ldpc.decode.Landsat8LdpcDecoderAccurate;
import farearth.landsat.ldpc.decode.Landsat8LdpcDecoderFast;
import farearth.landsat.ldpc.decode.Landsat8LdpcParityGraph;
import farearth.landsat.ldpc.decode.Landsat8LdpcParityMatrix;
import farearth.landsat.ldpc.decode.LdpcDecoder;
import farearth.landsat.ldpc.encode.Landsat8LdpcEncoder;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author anton
 */
public class TestDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(TestDecoder.class);
    private final Landsat8LdpcParityMatrix matrix = new Landsat8LdpcParityMatrix();
    private final Landsat8LdpcParityGraph graph = new Landsat8LdpcParityGraph(matrix);
    private final Landsat8LdpcFrameSizes frameSizes = new Landsat8LdpcFrameSizes();
    private final Landsat8LdpcEncoder encoder = new Landsat8LdpcEncoder();
    private final Landsat8LdpcDecoderFast fastDecoder = new Landsat8LdpcDecoderFast(graph);
    private final Landsat8LdpcDecoderAccurate accurateDecoder = new Landsat8LdpcDecoderAccurate(graph);

    @Test
    public void testDecoder() throws LdpcException {
        int outerLoops = 30;
        int outerSteps = 5;
        int innerLoops = 10;
        LOG.info("Fast Decoder:");
        testDecoder(fastDecoder, outerLoops, innerLoops, outerSteps);
        LOG.info("Accurate Decoder:");
        testDecoder(accurateDecoder, outerLoops, innerLoops, outerSteps);
    }

    private void testDecoder(LdpcDecoder decoder, int outerLoops, int innerLoops, int outerSteps) throws LdpcException {
        LOG.info("Frames per measurement: {}", innerLoops);
        for (int i = 0; i <= outerLoops; i += outerSteps) {
            int errorBits = i;
            testDecoder(decoder, innerLoops, errorBits);
        }
    }

    private void testDecoder(LdpcDecoder decoder, int iterations, int errorBits) throws LdpcException {
        byte[][] frames = LdpcTestUtils.generatePseudoRandomInfoFrames(iterations, frameSizes.getDecodedByteCount());
        byte[][] encoded = LdpcTestUtils.encode(encoder, frames, false);
        if (errorBits > 0) {
            LdpcTestUtils.flipBits(encoded, errorBits);
        }
        DecodeStats stats = new DecodeStats();
        for (int i = 0; i < iterations; i++) {
            testDecoder(decoder, frames[i], encoded[i], stats);
        }
        LOG.info("Errors per Frame: {}, Success: {}, Failed: {}, Incorrect: {}", errorBits, stats.success, stats.failed, stats.incorrect);
        if (decoder.equals(fastDecoder)) {
            if (errorBits <= 22) {
                Assert.assertEquals(iterations, stats.success);
            }
        } else if (errorBits <= 26) {
            Assert.assertEquals(iterations, stats.success);
        }
    }

    private void testDecoder(LdpcDecoder decoder, byte[] frame, byte[] encoded, DecodeStats stats) throws LdpcException {
        byte[] decoded = decoder.decode(encoded).getDecodedBytes().orElse(null);
        if (decoded == null) {
            stats.failed++;
        } else {
            decoded = Arrays.copyOf(decoded, frameSizes.getDecodedByteCount());
            if (Arrays.equals(frame, decoded)) {
                stats.success++;
            } else {
                stats.incorrect++;
            }
        }
    }

    private static class DecodeStats {

        int success;
        int failed;
        int incorrect;
    }
}
