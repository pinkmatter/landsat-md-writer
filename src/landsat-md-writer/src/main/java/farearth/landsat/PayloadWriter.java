/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

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
public class PayloadWriter implements PayloadHandler {

    private final WritableByteChannel _channel;

    public PayloadWriter(File output) throws IOException {
        _channel = FileChannel.open(output.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    @Override
    public void payload(Slice data) throws IOException {
        data.read(_channel);
    }

    @Override
    public void dataLost() {
    }

    @Override
    public void close() throws IOException {
        _channel.close();
    }

}
