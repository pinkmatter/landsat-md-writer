/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat.util.queue;

/**
 *
 * @author Chris
 */
public interface CloseAwareItemHandler<T> extends SmartQueueItemHandler<T> {

    void onClose() throws Exception;
}
