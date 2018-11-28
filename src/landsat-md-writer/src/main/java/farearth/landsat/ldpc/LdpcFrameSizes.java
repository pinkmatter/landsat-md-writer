/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc;

import farearth.landsat.util.BitUtils;

/**
 *
 * @author anton
 */
public class LdpcFrameSizes {

    private static final int DEFAULT_ENCODER_BIT_BOUNDARY = 32;
    private final int virtualBits; // prepended virtual unset/zero bits required to make info bits divisible by the bit boundary
    private final int decodedBits; // info bits (without virtual bits and/or padding)
    private final int decodedBytes; // info byts (without virtual bits and/or padding)
    private final int encodedBits; // amount of bits after encoding and after stripping off virtual bits again, but without padding
    private final int encodedBytes; // encoded bit with added padding to match byte boundary (should this not match encoder bit boundary?)

    public LdpcFrameSizes(int parityMatrixRows, int parityMatrixColumns) {
        this(parityMatrixRows, parityMatrixColumns, DEFAULT_ENCODER_BIT_BOUNDARY);
    }

    public LdpcFrameSizes(int parityMatrixRows, int parityMatrixColumns, int encoderBitBoundary) {
        virtualBits = (parityMatrixColumns - parityMatrixRows) % encoderBitBoundary;
        decodedBits = parityMatrixColumns - parityMatrixRows - virtualBits;
        encodedBits = parityMatrixColumns - virtualBits;
        decodedBytes = BitUtils.getRequiredBytes(decodedBits);
        encodedBytes = BitUtils.getRequiredBytes(encodedBits);
    }

    public int getVirtualBitCount() {
        return virtualBits;
    }

    public int getDecodedBitCount() {
        return decodedBits;
    }

    public int getDecodedByteCount() {
        return decodedBytes;
    }

    public int getEncodedBitCount() {
        return encodedBits;
    }

    public int getEncodedByteCount() {
        return encodedBytes;
    }
}
