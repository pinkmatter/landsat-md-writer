/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 *
 * @author Chris
 */
public interface Slice {

    public void skip(int delta);

    public int position();

    public void position(int position);

    /**
     * Extract a slice without affecting the original cut in any way
     *
     * @param start
     * @param length
     * @return
     */
    public Slice extract(int start, int length);

    public Slice extract(int length);

    public Slice duplicate();

    public int limit();

    public int getUnsignedByte();

    public long getBytesAsLong(int count) throws IOException;

    public byte[] getBytes(int count) throws IOException;

    /**
     * Returns the number of bytes read or -1 EOS
     *
     * @param dst
     * @return
     */
    public int read(ByteBuffer dst);

    public int read(WritableByteChannel dst) throws IOException;

    public int remaining();

    public boolean hasRemaining();
}
