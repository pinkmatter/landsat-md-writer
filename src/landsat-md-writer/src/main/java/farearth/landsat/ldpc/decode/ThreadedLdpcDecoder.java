/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.ldpc.decode;

import farearth.landsat.ldpc.LdpcFrameSizes;
import farearth.landsat.util.SequenceRandomizer;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author anton
 */
public class ThreadedLdpcDecoder {

    private final LdpcDecoder delegate;

    public ThreadedLdpcDecoder(LdpcDecoder delegate) {
        this.delegate = delegate;
    }

    public LdpcDecoder getDelegate() {
        return delegate;
    }

    public List<LdpcDecodeResult> decode(List<byte[]> encodedFrames, boolean derandomize) {
        LdpcFrameSizes frameSizes = delegate.getFrameSizes();
        int encodedByteCount = frameSizes.getEncodedByteCount();
        return encodedFrames.parallelStream().map(frame -> {
            if (derandomize) {
                SequenceRandomizer.encodeCcsdsInplace(frame, 0, encodedByteCount);
            }
            return delegate.decode(frame);
        }).collect(Collectors.toList()); // order is guaranteed even if parallel
    }
}
