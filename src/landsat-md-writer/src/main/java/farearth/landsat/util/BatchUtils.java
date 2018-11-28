/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author anton
 */
public class BatchUtils {

    public static void doAndDontStopOnErrors(CallableVoid... operations) throws Exception {
        doAndDontStopOnErrors(Arrays.asList(operations));
    }

    public static void doAndDontStopOnErrors(Collection<? extends CallableVoid> operations) throws Exception {
        Exception firstException = null;
        for (CallableVoid operation : operations) {
            try {
                operation.call();
            } catch (Exception ex) {
                if (firstException == null) {
                    firstException = ex;
                }
            }
        }
        if (firstException != null) {
            throw firstException;
        }
    }

    public static void closeAndDontStopOnErrors(AutoCloseable... closables) throws Exception {
        closeAndDontStopOnErrors(Arrays.asList(closables));
    }

    public static void closeAndDontStopOnErrors(Collection<? extends AutoCloseable> closables) throws Exception {
        List<CallableVoid> actions = closables.stream()
                .map(toCloseCallable())
                .collect(Collectors.toList());
        doAndDontStopOnErrors(actions);
    }

    private static Function<AutoCloseable, CallableVoid> toCloseCallable() {
        return closable -> () -> closable.close();
    }
}
