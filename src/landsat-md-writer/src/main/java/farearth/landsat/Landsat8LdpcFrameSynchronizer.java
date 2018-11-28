/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import farearth.landsat.ldpc.Landsat8LdpcConstants;
import farearth.landsat.ldpc.decode.LdpcDecodeResult;
import farearth.landsat.ldpc.decode.LdpcDecoder;
import farearth.landsat.ldpc.decode.ThreadedLdpcDecoder;
import farearth.landsat.util.BatchUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author anton
 */
public class Landsat8LdpcFrameSynchronizer extends AbstractFrameSynchronizer {

    private static final int BATCH_SIZE = 100;
    private final boolean derandomize;
    private final FrameSynchronizer nextFrameSync;
    private final ThreadedLdpcDecoder ldpcDecoder;
    private final List<byte[]> cache = new ArrayList<>();

    public Landsat8LdpcFrameSynchronizer(LdpcDecoder ldpcDecoder, boolean derandomize, FrameSynchronizer nextFrameSync) {
        super(Landsat8LdpcConstants.ASM, ldpcDecoder.getFrameSizes().getEncodedByteCount());
        this.derandomize = derandomize;
        this.nextFrameSync = nextFrameSync;
        this.ldpcDecoder = new ThreadedLdpcDecoder(ldpcDecoder);
    }

    @Override
    protected void onAsmError() {
    }

    @Override
    protected void processFrame(ByteBuffer buffer) throws IOException {
        byte[] bytes = new byte[getContentSize()];
        buffer.get(bytes);
        cache.add(bytes);
        if (cache.size() >= BATCH_SIZE) {
            processCache();
        }
    }

    private void processCache() throws IOException {
        List<LdpcDecodeResult> results = ldpcDecoder.decode(cache, derandomize);
        cache.clear();

        for (LdpcDecodeResult result : results) {
            if (result.isSuccess()) {
                result.getErrorBitPositions().size();
                nextFrameSync.process(ByteBuffer.wrap(result.getDecodedBytes().get()));
            }
        }
    }

    @Override
    public void close() throws IOException {
        try {
            BatchUtils.doAndDontStopOnErrors(
                    () -> processCache(), // process last frames in cache
                    () -> nextFrameSync.close()
            );
        } catch (Exception ex) {
            throw new IOException("Error closing LDPC Frame Synchronizer", ex);
        }
    }
}
