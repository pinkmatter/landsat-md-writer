/*
 *  Copyright Pinkmatter Solutions
 *  www.pinkmatter.com
 */
package farearth.landsat.util.queue;

@FunctionalInterface
public interface SmartQueueItemHandler<T> {

    void handle(T item) throws Exception;

}
