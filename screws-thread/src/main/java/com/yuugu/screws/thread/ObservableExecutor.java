package com.yuugu.screws.thread;

import android.annotation.TargetApi;
import android.os.Build;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * Author: yuugu
 * Date: 2018/11/29
 * Time: 8:35 PM
 * An {@link ObservableExecutor} which can provide observable future when executing task.
 */
public interface ObservableExecutor extends ExecutorService {

    @Override
    ObservableFuture<?> submit(Runnable task);

    @Override
    <T> ObservableFuture<T> submit(Runnable task, T result);

    @Override
    <T> ObservableFuture<T> submit(Callable<T> task);

    /**
     * The abstract observer executor implementation, which can be inherited to implement
     * the observable executor easily.
     */
    abstract class AbstractObservableExecutor extends AbstractExecutorService
            implements ObservableExecutor {

        @Override
        public ObservableFuture<?> submit(Runnable task) {
            if (aboveJava6()) {
                return (ObservableFuture<?>) super.submit(task);
            } else {
                ObservableFutureTask<?> observable = new ObservableFutureTask<>(task, null);
                execute(observable);
                return observable;
            }
        }

        @Override
        public <T> ObservableFuture<T> submit(Runnable task, T result) {
            if (aboveJava6()) {
                return (ObservableFuture<T>) super.submit(task, result);
            } else {
                ObservableFutureTask<T> observable = new ObservableFutureTask<>(task, result);
                execute(observable);
                return observable;
            }
        }

        @Override
        public <T> ObservableFuture<T> submit(Callable<T> task) {
            if (aboveJava6()) {
                return (ObservableFuture<T>) super.submit(task);
            } else {
                ObservableFutureTask<T> observable = new ObservableFutureTask<T>(task);
                execute(observable);
                return observable;
            }
        }

        @Override
        protected <T> ObservableRunnableFuture<T> newTaskFor(Runnable runnable, T value) {
            return new ObservableRunnableFutureTask<T>(runnable, value);
        }

        @Override
        protected <T> ObservableRunnableFuture<T> newTaskFor(Callable<T> callable) {
            return new ObservableRunnableFutureTask<>(callable);
        }

        private static boolean aboveJava6() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
        }

        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        private static class ObservableRunnableFutureTask<T> extends ObservableFutureTask<T>
                implements ObservableRunnableFuture<T> {

            ObservableRunnableFutureTask(Callable<T> callable) {
                super(callable);
            }

            ObservableRunnableFutureTask(Runnable runnable, T result) {
                super(runnable, result);
            }
        }
    }
}
