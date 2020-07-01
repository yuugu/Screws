package com.yuugu.screws.thread;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Process;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Author: yuugu
 * Date: 2018/11/29
 * Time: 12:03 AM
 */
public final class ThreadPools {

    /**
     * Default pool size.
     */
    public static final int DEFAULT_POOL_SIZE = 4;

    /**
     * Default thread priority. See constant defined in {@link Process}.
     */
    private static final int DEFAULT_THREAD_PRIORITY = Process.THREAD_PRIORITY_BACKGROUND;

    /**
     * Minor thread priority. See constant defined in {@link Process}.
     */
    private static final int MINOR_THREAD_PRIORITY = Process.THREAD_PRIORITY_LOWEST;

    private ThreadPools() {
        // static usage.
    }

    private final static class DefaultHolder {
        static final ObservablePriorityExecutor INSTANCE;

        static {
            PriorityThreadPoolExecutor executor = new PriorityThreadPoolExecutor(DEFAULT_POOL_SIZE);
            executor.setThreadFactory(new PriorityThreadFactory("default-thread-pool", DEFAULT_THREAD_PRIORITY));
            INSTANCE = observableExecutor(executor);
        }
    }

    private final static class MinorHolder {
        static final ScheduledThreadPoolExecutor INSTANCE
                = new ScheduledThreadPoolExecutor(1);

        static {
            INSTANCE.setThreadFactory(
                    new PriorityThreadFactory("minor-thread-pool", MINOR_THREAD_PRIORITY));
        }
    }

    /**
     * Returns the default thread pool ({@link ObservablePriorityExecutor}),
     * whose pool size is {@link #DEFAULT_POOL_SIZE}.
     *
     * @return Default thread pool.
     */
    public static ObservablePriorityExecutor defaultThreadPool() {
        return DefaultHolder.INSTANCE;
    }

    /**
     * Return the minor thread pool ({@link ScheduledExecutorService},
     * whose purpose is for minor and unimportant tasks.
     *
     * @return Minor thread pool.
     */
    public static ScheduledExecutorService minorThreadPool() {
        return MinorHolder.INSTANCE;
    }

    /**
     * Creates a thread pool that creates new threads as needed, but
     * will reuse previously constructed threads when they are
     * available.  These pools will typically improve the performance
     * of programs that execute many short-lived asynchronous tasks.
     * Calls to {@code execute} will reuse previously constructed
     * threads if available. If no existing thread is available, a new
     * thread will be created and added to the pool. Threads that have
     * not been used for sixty seconds are terminated and removed from
     * the cache. Thus, a pool that remains idle for long enough will
     * not consume any resources. Note that pools with similar
     * properties but different details (for example, timeout parameters)
     * may be created using {@link ThreadPoolExecutor} constructors.
     *
     * @param name the name of this thread pool.
     * @return the newly created thread pool
     */
    public static ExecutorService newCachedThreadPool(String name) {
        return newCachedThreadPool(name, 60L, TimeUnit.SECONDS);
    }

    /**
     * Creates a thread pool that creates new threads as needed, but
     * will reuse previously constructed threads when they are
     * available.  These pools will typically improve the performance
     * of programs that execute many short-lived asynchronous tasks.
     * Calls to {@code execute} will reuse previously constructed
     * threads if available. If no existing thread is available, a new
     * thread will be created and added to the pool. Threads that have
     * not been used for sixty seconds are terminated and removed from
     * the cache. Thus, a pool that remains idle for long enough will
     * not consume any resources. Note that pools with similar
     * properties but different details (for example, timeout parameters)
     * may be created using {@link ThreadPoolExecutor} constructors.
     *
     * @param name The name of thread pool.
     * @param timeout the maximum time that excess idle threads will
     *                wait for new tasks before terminating.
     * @param unit the time unit for the {@code timeout} argument.
     * @return the newly created thread pool
     */
    public static ExecutorService newCachedThreadPool(String name, long timeout, TimeUnit unit) {
        return newThreadPoolExecutor(name,
                0, Integer.MAX_VALUE, timeout, unit, new SynchronousQueue<>());
    }

    /**
     * <p>Support in API level 9 and above.</p>
     * Creates a thread pool that creates new threads as needed, but
     * will reuse previously constructed threads when they are
     * available.  These pools will typically improve the performance
     * of programs that execute many short-lived asynchronous tasks.
     * Calls to {@code execute} will reuse previously constructed
     * threads if available. If no existing thread is available, a new
     * thread will be created and added to the pool. Threads that have
     * not been used for sixty seconds are terminated and removed from
     * the cache. Thus, a pool that remains idle for long enough will
     * not consume any resources. Note that pools with similar
     * properties but different details (for example, timeout parameters)
     * may be created using {@link ThreadPoolExecutor} constructors.
     *
     * @param name The name of thread pool.
     * @param maxThreads the number of threads in the pool.
     * @return the newly created thread pool
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static ExecutorService newCachedThreadPool(String name, int maxThreads) {
        return newCachedThreadPool(name, maxThreads, 60L, TimeUnit.SECONDS);
    }

    /**
     * <p>Support in API level 9 and above.</p>
     * Creates a thread pool that creates new threads as needed, but
     * will reuse previously constructed threads when they are
     * available.  These pools will typically improve the performance
     * of programs that execute many short-lived asynchronous tasks.
     * Calls to {@code execute} will reuse previously constructed
     * threads if available. If no existing thread is available, a new
     * thread will be created and added to the pool. Threads that have
     * not been used for corresponding timeout are terminated and removed from
     * the cache. Thus, a pool that remains idle for long enough will
     * not consume any resources. Note that pools with similar
     * properties but different details (for example, timeout parameters)
     * may be created using {@link ThreadPoolExecutor} constructors.
     *
     * @param name The name of thread pool.
     * @param maxThreads the number of threads in the pool.
     * @param timeout the maximum time that excess idle threads will
     *                wait for new tasks before terminating.
     * @param unit the time unit for the {@code timeout} argument.
     * @return the newly created thread pool
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static ExecutorService newCachedThreadPool(String name, int maxThreads, long timeout, TimeUnit unit) {
        ThreadPoolExecutor executor = newThreadPoolExecutor(name,
                maxThreads, maxThreads, timeout, unit, new LinkedBlockingQueue<>());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    /**
     * Creates a thread pool that reuses a fixed number of threads
     * operating off a shared unbounded queue.  At any point, at most
     * {@code nThreads} threads will be active processing tasks.
     * If additional tasks are submitted when all threads are active,
     * they will wait in the queue until a thread is available.
     * If any thread terminates due to a failure during execution
     * prior to shutdown, a new one will take its place if needed to
     * execute subsequent tasks.  The threads in the pool will exist
     * until it is explicitly {@link ThreadPoolExecutor#shutdown shutdown}.
     *
     * @param nThreads the number of threads in the pool.
     * @return the newly created thread pool.
     * @throws IllegalArgumentException if {@code nThreads <= 0}
     */
    public static ExecutorService newFixedThreadPool(String name, int nThreads) {
        return newThreadPoolExecutor(name,
                nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    private static ThreadPoolExecutor newThreadPoolExecutor(String name,
                                                            int coreSize, int maxSize,
                                                            long timeout, TimeUnit timeunit,
                                                            BlockingQueue<Runnable> workQueue) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(coreSize, maxSize, timeout, timeunit, workQueue);
        executor.setThreadFactory(new PriorityThreadFactory(name, DEFAULT_THREAD_PRIORITY));
        return executor;
    }

    /**
     * Return the observable version of incoming executor.
     *
     * @param executor executor to be decorate.
     */
    public static ObservableExecutor observableExecutor(ExecutorService executor) {
        return (executor instanceof ObservableExecutor) ?
                (ObservableExecutor) executor : new ObservableExecutorDelegate(executor);
    }

    /**
     * Return the observable version of incoming priority executor.
     *
     * @param executor executor to be decorate.
     */
    public static ObservablePriorityExecutor observableExecutor(PriorityExecutorService executor) {
        return (executor instanceof ObservablePriorityExecutor) ?
                (ObservablePriorityExecutor) executor : new ObservablePriorityExecutorDelegate(executor);
    }

    private static class ObservableExecutorDelegate extends ObservableExecutor.AbstractObservableExecutor {

        private final ExecutorService mExecutor;

        ObservableExecutorDelegate(ExecutorService executor) {
            mExecutor = executor;
        }

        @Override
        public void shutdown() {
            mExecutor.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return mExecutor.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return mExecutor.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return mExecutor.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return mExecutor.awaitTermination(timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            mExecutor.execute(command);
        }
    }

    private static class ObservablePriorityExecutorDelegate extends ObservableExecutorDelegate
            implements ObservablePriorityExecutor {

        private final PriorityExecutorService mExecutor;

        ObservablePriorityExecutorDelegate(PriorityExecutorService executor) {
            super(executor);
            mExecutor = executor;
        }

        @Override
        public void execute(Runnable command, Priority priority) {
            mExecutor.execute(command, priority);
        }

        @Override
        public ObservableFuture<?> submit(Runnable task, Priority priority) {
            ObservableFuture<?> observable = aboveJava6() ? newTaskFor(task, null) :
                    new ObservableFutureTask<>(task, null);
            execute((Runnable) observable, priority);
            return observable;
        }

        @Override
        public <T> ObservableFuture<T> submit(Runnable task, T result, Priority priority) {
            ObservableFuture<T> observable = aboveJava6() ? newTaskFor(task, result) :
                    new ObservableFutureTask<>(task, result);
            execute((Runnable) observable, priority);
            return observable;
        }

        @Override
        public <T> ObservableFuture<T> submit(Callable<T> task, Priority priority) {
            ObservableFuture<T> observable = aboveJava6() ? newTaskFor(task) :
                    new ObservableFutureTask<>(task);
            execute((Runnable) observable, priority);
            return observable;
        }

        private static boolean aboveJava6() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
        }
    }
}
