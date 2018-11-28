/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.util;

/**
 *
 * @author eduan
 */
public interface RestartableTimer extends AutoCloseable {

    public void restart();

    public void restart(long period);

    public static final RestartableTimer NULL = new RestartableTimer() {
        @Override
        public void restart() {

        }

        @Override
        public void restart(long period) {

        }

        @Override
        public void close() throws Exception {

        }
    };
}
