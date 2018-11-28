/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import farearth.landsat.util.Slice;
import farearth.landsat.util.Slices;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Chris
 */
public class MpduPayloadAssembler implements HeaderAwarePayloadHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MpduHandler.class);
    private static final int BUFFER_SIZE = (int) Math.pow(2, 16) + CaduHeader.CADU_SIZE;
    private final PayloadHandler _handler;
    private ByteBuffer _buffer;

    public MpduPayloadAssembler(PayloadHandler handler) {
        _handler = handler;
        reset();
    }

    @Override
    public void headerPacket(Slice slice) throws IOException {
        if (!isEmpty()) {
            Slice compound = assemble();
            _handler.payload(compound);
            reset();
        }
        add(slice);
        checkHeader(slice);
    }

    private void checkHeader(Slice slice) throws IOException {
        if (slice.remaining() >= SpacePacketHeader.SIZE) {
            long value = slice.getBytesAsLong(SpacePacketHeader.SIZE);
            SpacePacketHeader header = SpacePacketHeader.parse(value);
            slice.skip(-SpacePacketHeader.SIZE);
            if (header.getVersion() != 0) {
                LOG.debug("invalid space packet: datalength={} apid={} version={}",
                        header.getDataLength(), header.getApidNumber(), header.getVersion());
            }
        }
    }

    @Override
    public void dataLost() {
        LOG.debug("data lost, clearing space packet assembly");
        reset();
        _handler.dataLost();
    }

    @Override
    public void payload(Slice e) throws IOException {
        if (!isEmpty()) {
            add(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (!isEmpty()) {
                Slice compound = assemble();
                _handler.payload(compound);
            }
        } finally {
            _handler.close();
        }
    }

    private void add(Slice slice) throws IOException {
        int pos = slice.position();
        int read = slice.read(_buffer);
        if (slice.hasRemaining()) {
            throw new IOException(String.format("didnt read complete slice: read=%d, slice remaining=%d", read, slice.remaining()));
        }
        slice.position(pos);
    }

    private Slice assemble() {
        _buffer.flip();
        return Slices.create(_buffer);
    }

    private boolean isEmpty() {
        return _buffer.position() == 0;
    }

    private void reset() {
        _buffer = ByteBuffer.allocate(BUFFER_SIZE);
    }

}
