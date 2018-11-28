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
abstract class QueuedPayloadHandler implements PayloadHandler {

    private final Logger LOG = LoggerFactory.getLogger(QueuedPayloadHandler.class);

    private final SmartQueue<Slice> _queue;

    protected QueuedPayloadHandler(PayloadHandler delegate, int maxSize, SmartQueueMode mode) {
        this(delegate, maxSize, mode, "Queued Payload Handler");
    }

    protected QueuedPayloadHandler(PayloadHandler delegate, int maxSize, SmartQueueMode mode, String name) {
        _queue = new SmartQueue<>(maxSize, mode, new MessageHandler(delegate), new SmartQueueEventHandler<Slice>() {
            @Override
            public void onErrorOnItem(Slice item, Throwable t) {
                LOG.error(String.format("Error during Queued Payload Handler operation for %s: %s", item, t.getMessage()), t);
            }

            @Override
            public void onErrorOnClose(Throwable t) {
                LOG.error("Error closing Queued Payload Handler: " + t.getMessage(), t);
            }

            @Override
            public void onSignal() {
            }
        }, name);
        _queue.start();
    }

    @Override
    public void payload(Slice e) throws IOException {
        _queue.push(e);
    }

    @Override
    public void dataLost() {
        _queue.push(null);
    }

    @Override
    public void close() throws IOException {
        _queue.close();
    }

    public static class Drop extends QueuedPayloadHandler {

        public Drop(PayloadHandler delegate, int maxSize) {
            this(delegate, maxSize, "Queued Payload Handler (Drop)");
        }

        public Drop(PayloadHandler delegate, int maxSize, String name) {
            super(delegate, maxSize, SmartQueueMode.Drop, name);
        }

    }

    public static class Block extends QueuedPayloadHandler {

        public Block(PayloadHandler delegate, int maxSize) {
            this(delegate, maxSize, "Queued Payload Handler (Block)");
        }

        public Block(PayloadHandler delegate, int maxSize, String name) {
            super(delegate, maxSize, SmartQueueMode.Block, name);
        }

    }

    private class MessageHandler implements CloseAwareItemHandler<Slice> {

        private final PayloadHandler _delegate;

        public MessageHandler(PayloadHandler delegate) {
            _delegate = delegate;
        }

        @Override
        public void handle(Slice item) throws IOException {
            if (item == null) {
                _delegate.dataLost();
            } else {
                _delegate.payload(item);
            }
        }

        @Override
        public void onClose() throws IOException {
            _delegate.close();
        }

    }

}
