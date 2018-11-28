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
public class CfdpHandler implements ApidPayloadHandler {

    private final FileHandler _handler;

    public CfdpHandler(FileHandler handler) {
        _handler = handler;
    }

    @Override
    public void payload(APID apid, Slice packet) throws IOException {
        CfdpHeader header = CfdpHeader.parse(packet);
        if (header != null) { //stream closed or really bad data
            switch (header.getType()) {
                case Metadata:
                    CfdpHeader.Metadata metadata = (CfdpHeader.Metadata) header;
                    _handler.startFile(metadata.getDestinationFilename());
                    break;
                case EOF:
                    CfdpHeader.EOF eof = (CfdpHeader.EOF) header;
                    _handler.endFile(eof.getFilesize(), eof.getChecksum());
                    break;
                case Data:
                    CfdpHeader.Data data = (CfdpHeader.Data) header;
                    _handler.fileData(packet, apid, data.getPayloadLength(), data.getOffset());
                    break;
                default:
                    throw new IOException("Unknown packet type: " + header.getType());
            }
        }
    }

    @Override
    public void close() throws IOException {
        _handler.close();
    }

    @Override
    public void dataLost() {
        _handler.dataLost();
    }

}
