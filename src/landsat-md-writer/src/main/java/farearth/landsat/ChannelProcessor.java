/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.StandardOpenOption;

/**
 *
 * @author Chris
 */
public class ChannelProcessor {

    private final FrameSynchronizer _sync;

    public ChannelProcessor(FrameSynchronizer sync) {
        _sync = sync;
    }

    public void readAll(File file) throws IOException {
        try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            readAll(channel);
        }
    }

    public void readAll(ReadableByteChannel channel) throws IOException {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(100000);
            while (channel.read(buffer) >= 0) {
                buffer.flip();
                _sync.process(buffer);
                buffer.clear();
            }
        } finally {
            _sync.close();
        }
    }

}
