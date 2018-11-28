/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.util.queue;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Chris
 * @param <T>
 */
public class SmartQueue<T> implements AutoCloseable {

    private final int _maxSize;
    private final Queue<T> _items = new LinkedList<>();
    private final Object _producerLock = new Object();
    private final Object _consumerLock = new Object();
    private final SmartQueueMode _mode;
    private final SmartQueueItemHandler<T> _handler;
    private final SmartQueueEventHandler<T> _eventHandler;

    private boolean _stopRequested = false;
    private String _name;
    private boolean _running = false;
    private long _totalAdded;
    private long _blockCount;
    private long _waitCount;
    private long _maxCount;
    private CountDownLatch _closeLock;
    private Exception _error;
    private ExecutorService _errorNotifier;
    private boolean _needSignal = false;

    public SmartQueue(int maxSize, SmartQueueMode mode, SmartQueueItemHandler<T> handler, SmartQueueEventHandler<T> errorHandler) {
        this(maxSize, mode, handler, errorHandler, "Smart Queue");
    }

    public SmartQueue(int maxSize, SmartQueueMode mode, SmartQueueItemHandler<T> handler, SmartQueueEventHandler<T> errorHandler, String name) {
        _handler = handler;
        _maxSize = maxSize;
        _name = name;
        _mode = mode;
        _eventHandler = errorHandler;
    }

    public boolean push(T item) {
        synchronized (this) {
            if (!_running) {
                throw new IllegalStateException("Queue must be started before it can be used");
            }
        }
        synchronized (_producerLock) {
            _totalAdded++;
            if (_items.size() >= _maxSize) {
                if (_mode == SmartQueueMode.Block || _mode == SmartQueueMode.Signal) {
                    try {
                        _blockCount++;
                        _producerLock.wait();
                        if (_error != null) {
                            Exception e = _error;
                            _error = null;
                            throw new RuntimeException("Error during queue operation: " + e.getMessage(), e);
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                } else if (_mode == SmartQueueMode.Drop) {
                    return false;
                }
            }
        }
        synchronized (_consumerLock) {
            _items.offer(item); // might still be possible to go beyond _maxSize due to the possibility of 2x producers entering push(..) at precisely the same time (albeit with producers running on different threads)
            int size = _items.size();
            if (size > _maxCount) {
                _maxCount = size;
            }
            _consumerLock.notify();
        }
        if (_mode == SmartQueueMode.Signal) {
            synchronized (_producerLock) {
                if (_items.size() >= _maxSize) {
                    _needSignal = true;
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    @SuppressWarnings("NestedSynchronizedStatement")
    public synchronized void close() {
        if (_running && !_stopRequested) {
            synchronized (_consumerLock) {
                _stopRequested = true;
                _consumerLock.notify();
            }
            try {
                _closeLock.await();
                if (_error != null) {
                    Exception e = _error;
                    _error = null;
                    throw new RuntimeException("Error during queue close operation: " + e.getMessage(), e);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                _running = false;
                _stopRequested = false;
                if (_errorNotifier != null) {
                    _errorNotifier.shutdown();
                }
                synchronized (_producerLock) {
                    _producerLock.notify();
                }

            }
        }
    }

    public synchronized boolean isRunning() {
        return _running;
    }

    public synchronized void start() {
        if (!_running) {
            _closeLock = new CountDownLatch(1);
            Thread consumerThread = new Thread(() -> {
                try {
                    while (!(_stopRequested && _items.isEmpty())) {
                        T item;
                        synchronized (_consumerLock) {
                            item = _items.poll();
                            if (item == null) {
                                _waitCount++;
                                _consumerLock.wait();
                            }
                        }
                        if (item != null) {
                            synchronized (_producerLock) {
                                _producerLock.notify();
                                if (_needSignal) {
                                    _needSignal = false;
                                    _eventHandler.onSignal();
                                }
                            }
                            try {
                                _handler.handle(item);
                            } catch (Exception e) {
                                notifyError(item, e);
                            }
                        }
                    }

                    try {
                        if (_handler instanceof CloseAwareItemHandler) {
                            ((CloseAwareItemHandler) _handler).onClose();
                        }
                    } catch (Exception e) {
                        notifyError(null, e);
                    } finally {
                        _closeLock.countDown();
                    }
                } catch (Exception e) {
                    _error = e;
                    synchronized (_producerLock) {
                        _producerLock.notify();
                    }
                    _closeLock.countDown();
                }
            }, _name);
            _error = null;
            _totalAdded = 0;
            _waitCount = 0;
            _blockCount = 0;
            _maxCount = 0;
            consumerThread.setDaemon(true);
            consumerThread.start();
            _running = true;
        }
    }

    private void notifyError(final T item, final Exception e) {
        if (_errorNotifier == null) {
            _errorNotifier = Executors.newSingleThreadExecutor();
        }
        _errorNotifier.submit(() -> {
            if (item == null) {
                _eventHandler.onErrorOnClose(e);
            } else {
                _eventHandler.onErrorOnItem(item, e);
            }
        });
    }

    public int size() {
        return _items.size();
    }

    public long getTotalAdded() {
        return _totalAdded;
    }

    public long getBlockCount() {
        return _blockCount;
    }

    public long getWaitCount() {
        return _waitCount;
    }

    public long getMaxCount() {
        return _maxCount;
    }

}
