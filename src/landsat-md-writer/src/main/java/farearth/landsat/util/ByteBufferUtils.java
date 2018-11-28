/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.util;

import java.nio.ByteBuffer;

/**
 *
 * @author anton
 */
public class ByteBufferUtils {

    private ByteBufferUtils() {
    }

    public static ByteBuffer chain(ByteBuffer... buffers) {
        int size = 0;
        for (ByteBuffer buffer : buffers) {
            size += buffer.remaining();
        }
        ByteBuffer newBuffer = ByteBuffer.allocate(size);
        for (ByteBuffer buffer : buffers) {
            newBuffer.put(buffer);
        }
        newBuffer.flip();
        return newBuffer;
    }

    public static void skip(ByteBuffer buffer, int contentSize) {
        buffer.position(buffer.position() + contentSize);
    }

    public static ByteBuffer duplicate(ByteBuffer buffer, int frameStart, int frameEnd) {
        ByteBuffer duplicate = buffer.duplicate(); // shared byte[]
        duplicate.position(frameStart);
        duplicate.limit(frameEnd);
        return duplicate;
    }

    public static String toCompactHex(ByteBuffer buffer) {
        buffer.mark();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        buffer.reset();
        return ByteStringUtils.bytesToCompactHex(bytes);
    }
}
