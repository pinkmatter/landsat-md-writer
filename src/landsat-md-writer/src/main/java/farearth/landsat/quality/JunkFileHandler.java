/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.quality;

import farearth.landsat.APID;
import farearth.landsat.FileHandler;
import farearth.landsat.util.Slice;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.StandardOpenOption;

/**
 *
 * @author Chris
 */
public class JunkFileHandler implements FileHandler {

    private final File _outputFolder;
    private String _filename = "junk.data";
    private WritableByteChannel _out;

    public JunkFileHandler(File outputFolder) {
        _outputFolder = outputFolder;
    }

    @Override
    public void startFile(String name) {
    }

    @Override
    public void endFile(long filesize, int checksum) {
    }

    @Override
    public void fileData(Slice data, APID apid, int length, long offset) throws IOException {
        if (_out == null) {
            _out = FileChannel.open(new File(_outputFolder, _filename).toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }
        data.read(_out);
    }

    @Override
    public void close() throws IOException {
        if (_out != null) {
            _out.close();
        }
    }

    @Override
    public void dataLost() {
    }

    public String getFilename() {
        return _filename;
    }

    public void setFilename(String filename) {
        _filename = filename;
    }

}
