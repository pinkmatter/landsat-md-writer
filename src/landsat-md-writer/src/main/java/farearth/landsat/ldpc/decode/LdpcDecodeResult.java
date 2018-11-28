/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc.decode;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author anton
 */
public class LdpcDecodeResult {

    private final byte[] encodedBytes;
    private final byte[] decodedBytes;
    private final int decodingLoops;
    private final List<Integer> errorBits;

    public LdpcDecodeResult(byte[] encodedBytes, byte[] decodedBytes, int decodingLoops, List<Integer> errorBits) {
        this.encodedBytes = encodedBytes;
        this.decodedBytes = decodedBytes;
        this.decodingLoops = decodingLoops;
        this.errorBits = errorBits;
    }

    public byte[] getEncodedBytes() {
        return encodedBytes;
    }

    public Optional<byte[]> getDecodedBytes() {
        return Optional.ofNullable(decodedBytes);
    }

    public boolean isSuccess() {
        return decodedBytes != null;
    }

    public int getDecodingLoops() {
        return decodingLoops;
    }

    public List<Integer> getErrorBitPositions() {
        return errorBits;
    }
}
