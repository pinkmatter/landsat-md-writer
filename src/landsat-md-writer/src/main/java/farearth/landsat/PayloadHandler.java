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
public interface PayloadHandler extends AutoCloseable {

    void payload(Slice e) throws IOException;

    void dataLost();

    @Override
    void close() throws IOException;
}
