package com.yuugu.screws.thread;

import java.util.concurrent.Callable;

/**
 * Author: yuugu
 * Date: 2018/11/29
 * Time: 12:03 AM
 * {@link ObservablePriorityExecutor} which provides both observable future and feature of priority task.
 */
public interface ObservablePriorityExecutor extends ObservableExecutor, PriorityExecutorService {

    @Override
    ObservableFuture<?> submit(Runnable task, Priority priority);

    @Override
    <T> ObservableFuture<T> submit(Runnable task, T result, Priority priority);

    @Override
    <T> ObservableFuture<T> submit(Callable<T> task, Priority priority);
}
