/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import farearth.landsat.util.Slice;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Chris
 */
public class MpduHandler implements PayloadHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MpduHandler.class);

    private final HeaderAwarePayloadHandler _handler;

    public MpduHandler(HeaderAwarePayloadHandler handler) {
        _handler = handler;
    }

    @Override
    public void payload(Slice caduPayload) throws IOException {
        int offset = readMpduOffset(caduPayload);
        if (offset < 0) {
            //TODO what if there is less data to extract? Extract should return null
            _handler.payload(caduPayload);
        } else if (offset == 0) {
            _handler.headerPacket(caduPayload);
        } else {
            Slice dataPacket = caduPayload.extract(offset);
            _handler.payload(dataPacket);
            caduPayload.skip(offset);
            int length = caduPayload.remaining();
            Slice headerPacket = caduPayload.extract(length);
            _handler.headerPacket(headerPacket);
        }
    }

    @Override
    public void dataLost() {
        _handler.dataLost();
    }

    @Override
    public void close() throws IOException {
        _handler.close();
    }

    private int readMpduOffset(Slice caduPayload) throws IOException {
        int mpdu = (int) caduPayload.getBytesAsLong(2);
        int spare = mpdu >>> 11;
        if (spare != 0) {
            LOG.debug("Invalid MPDU");
            return -1;
        }
        int offset = mpdu & 0x07FF;
        if (offset < 0x07FE) {
            if (offset >= CaduHeader.PAYLOAD_SIZE) {
                LOG.debug("Invalid MPDU offset {}", offset);
                return -1;
            }
            return offset;
        }
        return -1;
    }

}
