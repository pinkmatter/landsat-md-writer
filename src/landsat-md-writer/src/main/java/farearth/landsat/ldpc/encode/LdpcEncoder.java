/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc.encode;

import farearth.landsat.ldpc.LdpcArrayChecks;
import farearth.landsat.ldpc.LdpcException;
import farearth.landsat.ldpc.LdpcFrameSizes;
import farearth.landsat.util.BitUtils;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author anton
 * @param <T> The generator matrix.
 */
public abstract class LdpcEncoder<T extends LdpcGeneratorMatrix> {

    private static final Logger LOG = LoggerFactory.getLogger(LdpcEncoder.class);
    private static final boolean debugLogging = LOG.isDebugEnabled(); // cache for performance
    private final T generatorMatrix;
    private final LdpcFrameSizes frameSizes;
    private final LdpcCompactGeneratorMatrix compactMatrix;

    public LdpcEncoder(T generatorMatrix, int parityMatrixRows, int parityMatrixColumns) {
        this(generatorMatrix, new LdpcFrameSizes(parityMatrixRows, parityMatrixColumns));
    }

    public LdpcEncoder(T generatorMatrix, int parityMatrixRows, int parityMatrixColumns, int encoderBitBoundary) {
        this(generatorMatrix, new LdpcFrameSizes(parityMatrixRows, parityMatrixColumns, encoderBitBoundary));
    }

    public LdpcEncoder(T generatorMatrix, LdpcFrameSizes frameSizes) {
        this.generatorMatrix = generatorMatrix;
        this.frameSizes = frameSizes;
        compactMatrix = new LdpcCompactGeneratorMatrix(generatorMatrix, frameSizes.getVirtualBitCount());
    }

    public LdpcFrameSizes getFrameSizes() {
        return frameSizes;
    }

    public byte[] encode(byte[] bytes) throws LdpcException {
        int encodedByteCount = frameSizes.getEncodedByteCount();
        int virtualBitCount = frameSizes.getVirtualBitCount();
        int infoBits = generatorMatrix.getHeight();
        byte[][] rawMatrix = compactMatrix.getRawMatrix();
        byte[] encoded = Arrays.copyOf(bytes, encodedByteCount);
        for (int i = infoBits; i < generatorMatrix.getWidth(); i++) {
            int value = 0;
            byte[] columnBytes = rawMatrix[i];
            for (int j = 0; j < bytes.length; j++) {
                // Fast byte parity calculation
                int v = bytes[j] & columnBytes[j];
                v ^= v >> 4;
                v &= 0xf;
                value ^= (0x6996 >> v) & 1;
            }
            if (value != 0) {
                BitUtils.setBit(encoded, i - virtualBitCount);
            }
        }
        return encoded;
    }

    public byte[] encodeSlow(byte[] bytes) throws LdpcException {
        int[] expanded = expand(bytes);
        int[] encoded = encodeExpanded(expanded);
        byte[] compacted = compact(encoded);
        if (debugLogging) {
            LOG.debug("Unencoded: {}", BitUtils.toString(bytes));
            LOG.debug("Expanded:  {}", BitUtils.toStringExpanded(expanded));
            LOG.debug("Encoded:   {}", BitUtils.toStringExpanded(encoded));
            LOG.debug("Compacted: {}", BitUtils.toString(compacted));
        }
        return compacted;
    }

    public int[] encodeExpanded(int[] bits) throws LdpcException {
        int infoBits = generatorMatrix.getHeight();
        int encodedBits = generatorMatrix.getWidth();
        LdpcArrayChecks.checkLength(bits, infoBits);
        int[] encoded = Arrays.copyOf(bits, encodedBits);
        byte[][] matrix = generatorMatrix.getRawMatrix();
        // Do binary/logic matrix multiplication
        for (int i = infoBits; i < generatorMatrix.getWidth(); i++) {
            int value = 0;
            for (int j = 0; j < generatorMatrix.getHeight(); j++) {
                value ^= bits[j] & matrix[j][i];
            }
            encoded[i] = value;
        }
        return encoded;
    }

    private int[] expand(byte[] bytes) throws LdpcException {
        int infoBitCount = frameSizes.getDecodedBitCount();
        int infoByteCount = frameSizes.getDecodedByteCount();
        int virtualBitCount = frameSizes.getVirtualBitCount();
        LdpcArrayChecks.checkLength(bytes, infoByteCount);
        int[] bits = BitUtils.expand(bytes, 0, infoBitCount, virtualBitCount);
        LdpcArrayChecks.checkLength(bits, virtualBitCount + infoBitCount);
        return bits;
    }

    private byte[] compact(int[] bits) throws LdpcException {
        int encodedBitCount = frameSizes.getEncodedBitCount();
        int encodedByteCount = frameSizes.getEncodedByteCount();
        int virtualBitCount = frameSizes.getVirtualBitCount();
        LdpcArrayChecks.checkLength(bits, virtualBitCount + encodedBitCount);
        byte[] bytes = BitUtils.compact(bits, virtualBitCount, encodedByteCount);
        LdpcArrayChecks.checkLength(bytes, encodedByteCount);
        return bytes;
    }
}
