/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import farearth.landsat.util.Slice;
import java.io.EOFException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Chris
 */
public class SpacePacketHandler implements PayloadHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SpacePacketHandler.class);
    private final ApidPayloadHandler _handler;
    private final ApidFilter _filter;

    public SpacePacketHandler(ApidPayloadHandler handler) {
        this(handler, new ApidFilter.All());
    }

    public SpacePacketHandler(ApidPayloadHandler handler, ApidFilter filter) {
        _handler = handler;
        _filter = filter;
    }

    @Override
    public void payload(Slice spacePacket) throws IOException {
        try {
            long value = spacePacket.getBytesAsLong(SpacePacketHeader.SIZE);
            int dataLength = SpacePacketHeader.parseDataLength(value);
            APID apid = SpacePacketHeader.parseApid(value);
            if (apid != null) {
                int extra = spacePacket.remaining() - dataLength;
                if (extra == 0) {
                    //TODO we dont need to extract at this point
                    Slice payload = spacePacket.extract(dataLength);
                    handleSpacePacket(apid, payload);
                } else if (extra < 0) {
                    LOG.debug("Invalid space packet header length");
                } else {
                    Slice payload = spacePacket.extract(dataLength);
                    handleSpacePacket(apid, payload);
                    spacePacket.skip(dataLength);
                    payload(spacePacket);
                }
            } else if (LOG.isInfoEnabled()) {
                LOG.info("Invalid APID in space packet: {}, skipping {} bytes", SpacePacketHeader.parseApidNumber(value), spacePacket.remaining());
            }
        } catch (EOFException e) {
            LOG.warn("Received a space packet with a broken header: {}", e.getMessage());
        }
    }

    private void handleSpacePacket(APID apid, Slice slice) throws IOException {
        if (apid != APID.Fill && _filter.pass(apid)) {
            _handler.payload(apid, slice);
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

}
