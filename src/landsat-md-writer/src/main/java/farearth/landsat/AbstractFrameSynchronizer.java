/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import farearth.landsat.util.ByteBufferUtils;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author anton
 */
public abstract class AbstractFrameSynchronizer implements FrameSynchronizer {

    private static final int ASM_SIZE = 4; // only support int sized (4-byte) ASMs for now
    private final int asm;
    private final int contentSize;
    private final int searchSize;
    private final byte[] asmBytes;
    private ByteBuffer cache;

    public AbstractFrameSynchronizer(int asm, int contentSize) {
        this.asm = asm;
        this.contentSize = contentSize;
        searchSize = 2 * ASM_SIZE + contentSize; // only frames between two ASMs are valid
        asmBytes = ByteBuffer.allocate(ASM_SIZE).putInt(asm).array();
    }

    protected abstract void processFrame(ByteBuffer buffer) throws IOException;

    public int getAsm() {
        return asm;
    }

    public int getContentSize() {
        return contentSize;
    }

    @Override
    public void process(ByteBuffer buffer) throws IOException {
        if (cache != null) {
            buffer = ByteBufferUtils.chain(cache, buffer);
        }
        processFrames(buffer);
        if (buffer.remaining() > 0) {
            cache = buffer; // buffer.duplicate(); no need to duplicate
        } else {
            cache = null;
        }
    }

    private void processFrames(ByteBuffer buffer) throws IOException {
        while (scanToASM(buffer)) {
            boolean foundNextAsm;
            do {
                int frameStart = buffer.position();
                int frameEnd = frameStart + contentSize;
                buffer.position(frameEnd);
                foundNextAsm = readAsm(buffer);
                if (foundNextAsm) {
                    buffer.position(frameStart);
                    processFrame(buffer);
                    if (buffer.remaining() < searchSize) {
                        buffer.position(frameEnd);
                        return;
                    } else {
                        buffer.position(frameEnd + ASM_SIZE);
                    }
                } else {
                    onAsmError();
                    buffer.position(frameStart + ASM_SIZE);
                }
            } while (foundNextAsm);
        }
    }

    protected void onAsmError() {
        // Override to e.g. accumulate stats
    }

    private boolean readAsm(ByteBuffer buffer) throws IOException {
        return buffer.getInt() == asm;
    }

    // this approach should be faster than Landsat8FrameSynchronizer.scanToASM(...)
    private boolean scanToASM(ByteBuffer buffer) {
        while (buffer.remaining() >= searchSize) {
            // if first byte matches
            if (asmBytes[0] == buffer.get()) {
                int position = buffer.position();
                // check if other 3 bytes match
                if (asmBytes[1] == buffer.get()
                        && asmBytes[2] == buffer.get()
                        && asmBytes[3] == buffer.get()) {
                    return true;
                }
                // else reset position to after first byte
                buffer.position(position);
            }
        }
        return false;
    }

    @Override
    public void close() throws IOException {
    }
}
