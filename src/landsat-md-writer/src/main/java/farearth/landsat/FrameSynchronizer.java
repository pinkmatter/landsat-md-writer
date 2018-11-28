/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author Chris
 */
public interface FrameSynchronizer extends AutoCloseable {

    void process(ByteBuffer buffer) throws IOException;

    @Override
    void close() throws IOException;
}
