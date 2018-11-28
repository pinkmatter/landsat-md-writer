/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.util.queue;

import org.slf4j.Logger;

public interface SmartQueueEventHandler<T> {

    void onErrorOnItem(T item, Throwable t);

    void onErrorOnClose(Throwable t);

    void onSignal();

    public static class DefaultLoggingHandler<T> implements SmartQueueEventHandler<T> {

        private final Logger LOG;

        public DefaultLoggingHandler(Logger logger) {
            LOG = logger;
        }

        @Override
        public void onErrorOnItem(T item, Throwable t) {
            LOG.error("Failed to process SmartQueue item", t);
        }

        @Override
        public void onErrorOnClose(Throwable t) {
            LOG.error("Failed to close SmartQueue", t);
        }

        @Override
        public void onSignal() {
            LOG.warn("SmartQueue was signaled");
        }

    }

}
