/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import farearth.landsat.util.Slice;
import java.io.IOException;

/**
 *
 * @author Chris
 */
public class CaduHandler implements PayloadHandler {

    private static final long MAX_FRAME_COUNT = 16777215L;
    private long _lastCaduNumber = -1;
    private final PayloadHandler _nextHandler;

    public CaduHandler(PayloadHandler nextHandler) {
        _nextHandler = nextHandler;
    }

    @Override
    public void payload(Slice e) throws IOException {
        e.skip(CaduHeader.ASM_SIZE + 2);
        long frameCount = e.getBytesAsLong(3);
        e.skip(1);
        long drops = getDrops(frameCount);
        if (drops > 0) {
            dataLost();
        }
        _nextHandler.payload(e);
    }

    @Override
    public void dataLost() {
        _nextHandler.dataLost();
    }

    @Override
    public void close() throws IOException {
        _nextHandler.close();
    }

    private long getDrops(long vcFrameCount) {
        long lastNumber = _lastCaduNumber;
        _lastCaduNumber = vcFrameCount;
        if (lastNumber < 0) {
            return 0;
        } else if (vcFrameCount < lastNumber) {
            //wrap
            return MAX_FRAME_COUNT - lastNumber + vcFrameCount;
        } else {
            //no wrap
            return vcFrameCount - lastNumber - 1;
        }
    }

}
