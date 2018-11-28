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
public class FileMultiplexer implements FileHandler {

    private final FileHandler[] _handlers;

    public FileMultiplexer(FileHandler... handlers) {
        _handlers = handlers;
    }

    @Override
    public void startFile(String name) throws IOException {
        for (FileHandler handler : _handlers) {
            handler.startFile(name);
        }
    }

    @Override
    public void endFile(long filesize, int checksum) throws IOException {
        for (FileHandler handler : _handlers) {
            handler.endFile(filesize, checksum);
        }
    }

    @Override
    public void fileData(Slice data, APID apid, int length, long offset) throws IOException {
        for (FileHandler handler : _handlers) {
            handler.fileData(data.duplicate(), apid, length, offset);
        }
    }

    @Override
    public void close() throws IOException {
        for (FileHandler handler : _handlers) {
            handler.close();
        }
    }

    @Override
    public void dataLost() {
        for (FileHandler handler : _handlers) {
            handler.dataLost();
        }
    }

}
