/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc;

import farearth.landsat.ldpc.encode.Landsat8LdpcEncoder;
import farearth.landsat.util.BitUtils;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author anton
 */
public class LdpcTestUtils {

    private static final Logger LOG = LoggerFactory.getLogger(LdpcTestUtils.class);
    private static final Random rand = new Random(1); // fixed seed

    public static byte[][] generatePseudoRandomInfoFrames(int count, int size) {
        byte[][] frames = new byte[count][size];
        for (int frame = 0; frame < count; frame++) {
            for (int i = 0; i < size; i++) {
                frames[frame][i] = (byte) (i + frame % 256);
            }
        }
        return frames;
    }

    public static void flipBits(byte[][] encoded, int errorBits) {
        for (int f = 0; f < encoded.length; f++) {
            byte[] frame = encoded[f];
            for (int i = 0; i < errorBits; i++) {
                int bitIndex = rand.nextInt(frame.length * 8);
                BitUtils.flipBit(frame, bitIndex);
            }
        }
    }

    public static byte[][] encode(Landsat8LdpcEncoder encoder, byte[][] frames, boolean log) {
        byte[][] encoded = new byte[frames.length][];
        AtomicInteger num = new AtomicInteger();
        IntStream.range(0, frames.length).parallel().forEach(i -> {
            byte[] frame = frames[i];
            try {
                int frameNum = num.incrementAndGet();
                if (log && frameNum % 100 == 0) {
                    LOG.info("Encoding {}/{}", frameNum, frames.length);
                }
                encoded[i] = encoder.encode(frame);
            } catch (LdpcException ex) {
                LOG.error("Error while encoding frame", ex);
            }
        });
        return encoded;
    }
}
