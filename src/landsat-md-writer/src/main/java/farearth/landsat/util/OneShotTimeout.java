/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author eduan
 */
public class OneShotTimeout implements RestartableTimer {

    private static final int DEFAULT_TIMEOUT_PERIOD = 5_000;
    private static final Logger LOG = LoggerFactory.getLogger(OneShotTimeout.class);

    private final Timer _timeoutTimer;
    private final Consumer<Boolean> _triggered;
    private final long _period;

    private boolean _shouldRun = true;
    private TimerTask _timeoutTriggered;

    public OneShotTimeout(String threadName, Consumer<Boolean> triggered) {
        this(threadName, DEFAULT_TIMEOUT_PERIOD, triggered);
    }

    public OneShotTimeout(String threadName, long period, Consumer<Boolean> triggered) {
        _timeoutTimer = new Timer(threadName);
        _triggered = triggered;
        _timeoutTriggered = createNewTask(_triggered);
        _period = period;
    }

    @Override
    public void restart() {
        restart(_period);
    }

    @Override
    public void restart(long period) {
        _timeoutTriggered.cancel();
        _triggered.accept(false);
        synchronized (this) {
            if (_shouldRun) {
                _timeoutTriggered = createNewTask(_triggered);
                _timeoutTimer.schedule(_timeoutTriggered, period);
            }
        }
    }

    @Override
    public synchronized void close() throws Exception {
        _shouldRun = false;
        _timeoutTriggered.cancel();
        _timeoutTimer.cancel();
        _timeoutTimer.purge();
    }

    private static TimerTask createNewTask(Consumer<Boolean> triggered) {
        return new TimerTask() {
            @Override
            public void run() {
                try {
                    triggered.accept(true);
                } catch (Throwable t) {
                    LOG.debug("Error on dispatching timeout event: {}.", t.getMessage(), t);
                }
            }
        };
    }

}
