/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import farearth.landsat.util.Slice;
import farearth.landsat.util.queue.CloseAwareItemHandler;
import farearth.landsat.util.queue.SmartQueue;
import farearth.landsat.util.queue.SmartQueueEventHandler;
import farearth.landsat.util.queue.SmartQueueMode;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Chris
 */
abstract class QueuedFileHandler implements FileHandler {

    private final Logger LOG = LoggerFactory.getLogger(QueuedFileHandler.class);

    private final SmartQueue<Message> _queue;

    protected QueuedFileHandler(FileHandler delegate, int maxSize, SmartQueueMode mode) {
        this(delegate, maxSize, mode, "Queued File Handler");
    }

    protected QueuedFileHandler(FileHandler delegate, int maxSize, SmartQueueMode mode, String name) {
        LOG.info("Creating queued handler '{}' with max size {} and mode {}.", name, maxSize, mode);
        _queue = new SmartQueue<>(maxSize, mode, new MessageHandler(delegate), new SmartQueueEventHandler<Message>() {
            @Override
            public void onErrorOnItem(Message item, Throwable t) {
                LOG.error(String.format("Error during Queued File Handler operation for %s: %s", item, t.getMessage()), t);
            }

            @Override
            public void onErrorOnClose(Throwable t) {
                LOG.error("Error closing Queued File Handler: " + t.getMessage(), t);
            }

            @Override
            public void onSignal() {
            }
        }, name);
        _queue.start();
    }

    @Override
    public void startFile(String name) throws IOException {
        _queue.push(new StartMessage(name));
    }

    @Override
    public void endFile(long filesize, int checksum) throws IOException {
        _queue.push(new EndMessage(filesize, checksum));
    }

    @Override
    public void fileData(Slice data, APID apid, int length, long offset) throws IOException {
        _queue.push(new DataMessage(data, apid, length, offset));
    }

    @Override
    public void dataLost() {
        _queue.push(new DataLostMessage());
    }

    @Override
    public void close() throws IOException {
        _queue.close();
    }

    public static class Drop extends QueuedFileHandler {

        public Drop(FileHandler delegate, int maxSize) {
            this(delegate, maxSize, "Queued File Handler (Drop)");
        }

        public Drop(FileHandler delegate, int maxSize, String name) {
            super(delegate, maxSize, SmartQueueMode.Drop, name);
        }

    }

    public static class Block extends QueuedFileHandler {

        public Block(FileHandler delegate, int maxSize) {
            this(delegate, maxSize, "Queued File Handler (Block)");
        }

        public Block(FileHandler delegate, int maxSize, String name) {
            super(delegate, maxSize, SmartQueueMode.Block, name);
        }

    }

    private class MessageHandler implements CloseAwareItemHandler<Message> {

        private final FileHandler _delegate;

        public MessageHandler(FileHandler delegate) {
            _delegate = delegate;
        }

        @Override
        public void handle(Message item) throws IOException {
            if (item instanceof StartMessage) {
                StartMessage message = (StartMessage) item;
                _delegate.startFile(message.getName());
            } else if (item instanceof EndMessage) {
                EndMessage message = (EndMessage) item;
                _delegate.endFile(message.getFilesize(), message.getChecksum());
            } else if (item instanceof DataMessage) {
                DataMessage message = (DataMessage) item;
                _delegate.fileData(message.getData(), message.getApid(), message.getLength(), message.getOffset());
            } else if (item instanceof DataLostMessage) {
                _delegate.dataLost();
            }
        }

        @Override
        public void onClose() throws IOException {
            _delegate.close();
        }

    }

    private static abstract class Message {

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }

    }

    private static class StartMessage extends Message {

        private final String _name;

        public StartMessage(String name) {
            _name = name;
        }

        public String getName() {
            return _name;
        }

    }

    private static class EndMessage extends Message {

        private final long _filesize;
        private final int _checksum;

        public EndMessage(long filesize, int checksum) {
            _filesize = filesize;
            _checksum = checksum;
        }

        public long getFilesize() {
            return _filesize;
        }

        public int getChecksum() {
            return _checksum;
        }

    }

    private static class DataMessage extends Message {

        private final long _offset;
        private final int _length;
        private final APID _apid;
        private final Slice _data;

        public DataMessage(Slice data, APID apid, int length, long offset) {
            _data = data;
            _apid = apid;
            _length = length;
            _offset = offset;
        }

        public long getOffset() {
            return _offset;
        }

        public int getLength() {
            return _length;
        }

        public APID getApid() {
            return _apid;
        }

        public Slice getData() {
            return _data;
        }

    }

    private static class DataLostMessage extends Message {

    }

}
