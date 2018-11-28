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
public interface ApidPayloadHandler {

    void payload(APID apid, Slice e) throws IOException;

    void close() throws IOException;

    void dataLost();

}
