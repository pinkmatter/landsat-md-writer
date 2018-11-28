/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author anton
 */
public class ThreadUtils {

    private ThreadUtils() {
    }

    public static <T> List<T> forkAndWait(ExecutorService service, Callable<T>... callables) {
        return forkAndWait(service, Arrays.asList(callables));
    }

    public static <T> List<T> forkAndWait(ExecutorService service, List<Callable<T>> callables) {
        List<T> result = new ArrayList<>();
        List<Future<T>> futures = new ArrayList<>();
        callables.forEach(c -> futures.add(service.submit(c)));
        Throwable lastException = null;
        for (Future<T> f : futures) {
            try {
                result.add(f.get());
            } catch (Throwable t) {
                lastException = t;
            }
        }
        if (lastException != null) {
            throw new RuntimeException(lastException);
        }
        return result;
    }

    /**
     * Create a thread factory for named daemon threads.
     *
     * @param namePattern e.g. "epic-thread-%d"
     * @return
     */
    public static ThreadFactory createThreadFactory(String namePattern) {
        AtomicLong threadCount = new AtomicLong(0);
        ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
        return (Runnable r) -> {
            Thread newThread = defaultThreadFactory.newThread(r);
            newThread.setName(String.format(namePattern, threadCount.incrementAndGet()));
            newThread.setDaemon(true);
            return newThread;
        };
    }

}
