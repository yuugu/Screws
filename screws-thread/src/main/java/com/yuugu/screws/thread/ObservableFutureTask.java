package com.yuugu.screws.thread;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

/**
 * Author: yuugu
 * Date: 2018/11/29
 * Time: 11:03 AM
 * Observable future task which can be observed.
 */
public class ObservableFutureTask<T> extends FutureTask<T> implements ObservableFuture<T> {

    private List<Pair<Observer<T>, Executor>> mObservers;
    private final Object mLock = new Object();

    public ObservableFutureTask(Callable<T> callable) {
        super(callable);
    }

    public ObservableFutureTask(Runnable runnable, T result) {
        super(runnable, result);
    }

    @Override
    public void observe(Observer<T> observer, Executor executor) {
        synchronized (mLock) {
            if (!isDone()) {
                if (mObservers == null) {
                    mObservers = new ArrayList<>();
                }
                mObservers.add(new Pair<>(observer, executor));
                return;
            }
        }
        executor.execute(() -> observer.done(this));
    }

    @Override
    protected void done() {
        List<Pair<Observer<T>, Executor>> observers;
        synchronized (mLock) {
            observers = mObservers;
            mObservers = null;
        }
        if (observers != null) {
            for (Pair<Observer<T>, Executor> observer : observers) {
                observer.second.execute(() -> observer.first.done(this));
            }
        }
    }
}
