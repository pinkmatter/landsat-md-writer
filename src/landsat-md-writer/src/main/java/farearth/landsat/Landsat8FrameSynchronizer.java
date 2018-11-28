/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import farearth.landsat.util.Slice;
import farearth.landsat.util.Slices;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Chris
 */
class Landsat8FrameSynchronizer implements FrameSynchronizer {

    private static final int LANDSAT8 = 0xFA;
    private static final int VERSION = 1;
    private static final Logger LOG = LoggerFactory.getLogger(Landsat8FrameSynchronizer.class);

    private final Map<Integer, PayloadHandler> _handlers;
    private ByteBuffer _cache;

    public Landsat8FrameSynchronizer(Map<Integer, PayloadHandler> handlers) {
        _handlers = handlers;
    }

    @Override
    public void process(ByteBuffer buffer) throws IOException {
        Slice slice;
        if (_cache != null) {
            slice = Slices.create(_cache, buffer);
        } else {
            slice = Slices.create(buffer);
        }
        while (slice.remaining() >= CaduHeader.CADU_SIZE + CaduHeader.ASM_SIZE) {
            process(slice);
        }
        if (slice.remaining() > 0) {
            _cache = ByteBuffer.allocate(slice.remaining());
            slice.read(_cache);
        } else {
            _cache = null;
        }
    }

    private void process(Slice slice) throws IOException {
        int vcID = alignToCadu(slice);
        if (vcID >= 0) {
            if (slice.remaining() >= CaduHeader.CADU_SIZE + CaduHeader.ASM_SIZE) {
                int position = slice.position();
                slice.skip(CaduHeader.CADU_SIZE);
                if (readAsm(slice)) {
                    slice.position(position);
                    PayloadHandler handler = _handlers.get(vcID);
                    if (handler != null) {
                        ByteBuffer copy = ByteBuffer.allocate(CaduHeader.CADU_SIZE);
                        slice.read(copy);
                        handler.payload(Slices.create(copy));
                    } else {
                        slice.skip(CaduHeader.CADU_SIZE);
                    }
                } else {
                    slice.position(position + CaduHeader.ASM_SIZE);
                }

            }
        }
    }

    private int alignToCadu(Slice slice) throws IOException {
        int vcID = readValidStart(slice);
        if (vcID < 0) {
            vcID = scanToValidStart(slice);
            if (vcID < 0) {
                return -1;
            }
        }
        slice.skip(-CaduHeader.ASM_SIZE - 2);
        return vcID;
    }

    private boolean readAsm(Slice slice) throws IOException {
        if (slice.remaining() >= CaduHeader.ASM_SIZE) {
            return slice.getBytesAsLong(CaduHeader.ASM_SIZE) == CaduHeader.ASM;
        }
        return false;
    }

    private int readValidStart(Slice slice) throws IOException {
        if (readAsm(slice)) {
            return readVc(slice);
        }
        return -1;
    }

    private int scanToValidStart(Slice slice) throws IOException {
        int vc = -1;
        while (vc < 0 && scanToASM(slice)) {
            vc = readVc(slice);
            if (vc < 0) {
                slice.skip(-2);
            }
        }
        return vc;
    }

    private boolean scanToASM(Slice slice) throws IOException {
        int scanValue = 0;
        int read = slice.getUnsignedByte();
        while (read >= 0) {
            scanValue <<= 8;
            scanValue += read;
            if (scanValue == CaduHeader.ASM) {
                return true;
            }
            read = slice.getUnsignedByte();
        }
        return false;
    }

    private int readVc(Slice slice) throws IOException {
        int byte1 = slice.getUnsignedByte();
        int byte2 = slice.getUnsignedByte();
        if (byte2 < 0) {
            return -1;
        }
        int vcID = CaduHeader.getVcAndCheck(byte1, byte2, VERSION, LANDSAT8);
        if (vcID < 0) {
            LOG.debug("Invalid version or spacecraft");
            return -1;
        }
        return vcID;
    }

    private void closeHandlers() throws IOException {
        IOException lastException = null;
        for (PayloadHandler handler : _handlers.values()) {
            lastException = closeQuietly(handler);
        }
        if (lastException != null) {
            throw lastException;
        }
    }

    private IOException closeQuietly(PayloadHandler closeable) {
        try {
            closeable.close();
            return null;
        } catch (IOException e) {
            return e;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (_cache != null && _cache.remaining() >= CaduHeader.CADU_SIZE) {
                Slice slice = Slices.create(_cache);
                int vcID = alignToCadu(slice);
                if (vcID >= 0) {
                    if (slice.remaining() >= CaduHeader.CADU_SIZE) {
                        PayloadHandler handler = _handlers.get(vcID);
                        if (handler != null) {
                            ByteBuffer copy = ByteBuffer.allocate(CaduHeader.CADU_SIZE);
                            slice.read(copy);
                            handler.payload(Slices.create(copy));
                        }
                    }
                }
            }
        } finally {
            closeHandlers();
        }
    }

}
