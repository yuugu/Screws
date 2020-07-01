package com.yuugu.screws.thread;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * Author: yuugu
 * Date: 2018/11/29
 * Time: 9:03 PM
 * Future that can be observed.
 */
public interface ObservableFuture<T> extends Future<T> {

    /**
     * Observer for {@link ObservableFuture}.
     */
    interface Observer<T> {
        /**
         * Called when corresponding observable future is done.
         *
         * @param future the future to be observed.
         */
        void done(ObservableFuture<T> future);
    }

    /**
     * Add an observer to this future.
     *
     * @param observer future observer.
     * @param executor executor to callback observer.
     * @throws NullPointerException if observer or executor is null.
     */
    void observe(Observer<T> observer, Executor executor);
}
