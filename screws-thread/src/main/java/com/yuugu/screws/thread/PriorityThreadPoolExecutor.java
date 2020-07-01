package com.yuugu.screws.thread;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Author: yuugu
 * Date: 2018/11/29
 * Time: 12:03 AM
 * <p>Thread pool with priority support.</p>
 */
public class PriorityThreadPoolExecutor extends ThreadPoolExecutor implements PriorityExecutorService {

    /**
     * Construct a PriorityThreadPoolExecutor with corresponding pool size.
     *
     * @param poolSize Pool size of this thread pool.
     */
    public PriorityThreadPoolExecutor(int poolSize) {
        super(poolSize, poolSize, Integer.MAX_VALUE, TimeUnit.NANOSECONDS, new PriorityBlockingQueue<>());
    }

    /**
     * <p>Support in API level 9 and above.</p>
     * Construct a PriorityThreadPoolExecutor with corresponding pool size and keep alive time.
     *
     * @param poolSize      Pool size of this thread pool.
     * @param keepAliveTime this is the maximum time that idle threads will wait for new
     *                      tasks before terminating.
     * @param unit          the time unit for the {@code keepAliveTime} argument
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public PriorityThreadPoolExecutor(int poolSize,
                                      long keepAliveTime,
                                      TimeUnit unit) {
        super(poolSize, poolSize, keepAliveTime, unit, new PriorityBlockingQueue<>());
        if (keepAliveTime > 0) {
            allowCoreThreadTimeOut(true);
        }
    }

    /**
     * Construct a PriorityThreadPoolExecutor with corresponding core size, max size, keep alive time and queue for jobs.
     *
     * @param coreSize      Core pool size of this thread pool.
     * @param maxPoolSize   Maximum pool size of this thread pool.
     * @param keepAliveTime when the number of threads is greater than
     *                      the core, this is the maximum time that excess idle threads
     *                      will wait for new tasks before terminating.
     * @param unit          the time unit for the {@code keepAliveTime} argument
     * @param workQueue     the queue to use for holding tasks before they are
     *                      executed.  This queue will hold only the {@code Runnable}
     *                      tasks submitted by the {@code execute} method.
     */
    public PriorityThreadPoolExecutor(int coreSize,
                                      int maxPoolSize,
                                      long keepAliveTime,
                                      TimeUnit unit,
                                      PriorityBlockingQueue<Runnable> workQueue) {
        super(coreSize, maxPoolSize, keepAliveTime, unit, workQueue);
    }

    /**
     * Construct a PriorityThreadPoolExecutor with corresponding core size, max size, keep alive time and queue for jobs.
     *
     * @param coreSize      Core pool size of this thread pool.
     * @param maxPoolSize   Maximum pool size of this thread pool.
     * @param keepAliveTime when the number of threads is greater than
     *                      the core, this is the maximum time that excess idle threads
     *                      will wait for new tasks before terminating.
     * @param unit          the time unit for the {@code keepAliveTime} argument
     * @param workQueue     the queue to use for holding tasks before they are
     *                      executed.  This queue will hold only the {@code Runnable}
     *                      tasks submitted by the {@code execute} method.
     * @param handler       the handler to use when execution is blocked
     *                      because the thread bounds and queue capacities are reached.
     */
    public PriorityThreadPoolExecutor(int coreSize,
                                      int maxPoolSize,
                                      long keepAliveTime,
                                      TimeUnit unit,
                                      PriorityBlockingQueue<Runnable> workQueue,
                                      RejectedExecutionHandler handler) {
        super(coreSize, maxPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    /**
     * Construct a PriorityThreadPoolExecutor with corresponding core size, max size, keep alive time and queue for jobs.
     *
     * @param coreSize      Core pool size of this thread pool.
     * @param maxPoolSize   Maximum pool size of this thread pool.
     * @param keepAliveTime when the number of threads is greater than
     *                      the core, this is the maximum time that excess idle threads
     *                      will wait for new tasks before terminating.
     * @param unit          the time unit for the {@code keepAliveTime} argument
     * @param workQueue     the queue to use for holding tasks before they are
     *                      executed.  This queue will hold only the {@code Runnable}
     *                      tasks submitted by the {@code execute} method.
     * @param threadFactory the factory to use when the executor
     *                      creates a new thread
     * @param handler       the handler to use when execution is blocked
     *                      because the thread bounds and queue capacities are reached.
     */
    public PriorityThreadPoolExecutor(int coreSize,
                                      int maxPoolSize,
                                      long keepAliveTime,
                                      TimeUnit unit,
                                      PriorityBlockingQueue<Runnable> workQueue,
                                      ThreadFactory threadFactory,
                                      RejectedExecutionHandler handler) {
        super(coreSize, maxPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    /**
     * @throws RejectedExecutionException at discretion of
     *                                    {@code RejectedExecutionHandler}, if the task
     *                                    cannot be accepted for execution {@inheritDoc}
     * @throws NullPointerException       if {@code command} is null {@inheritDoc}
     */
    @Override
    public void execute(Runnable command) {
        execute(command, null);
    }

    /**
     * Executes the given task sometime in the future.  The task
     * may execute in a new thread or in an existing pooled thread.
     * <p>
     * If the task cannot be submitted for execution, either because this
     * executor has been shutdown or because its capacity has been reached,
     * the task is handled by the current {@code RejectedExecutionHandler}.
     *
     * @param command  the task to execute
     * @param priority Priority of this job.
     * @throws RejectedExecutionException at discretion of
     *                                    {@code RejectedExecutionHandler}, if the task
     *                                    cannot be accepted for execution {@inheritDoc}
     * @throws NullPointerException       if {@code command} is null {@inheritDoc}
     */
    @Override
    public void execute(Runnable command, Priority priority) {
        if (priority == null) {
            priority = Priority.NORMAL;
        }
        PriorityRunnable priorityRunnable = new PriorityRunnable(command, priority);
        super.execute(priorityRunnable);
    }

    /**
     * Same as {@link #submit(Runnable)}, with corresponding priority.
     *
     * @throws RejectedExecutionException
     * @throws NullPointerException
     */
    @SuppressLint("NewApi")
    @Override
    public Future<?> submit(Runnable task, Priority priority) {
        if (task == null) throw new NullPointerException();
        if (aboveJava6()) {
            return submitInternal(newTaskFor(task, null), priority);
        } else {
            return submitInternal(new FutureTask<>(task, null), priority);
        }
    }

    /**
     * Same as {@link #submit(Runnable, T)}, with corresponding priority.
     *
     * @throws RejectedExecutionException
     * @throws NullPointerException
     */
    @SuppressLint("NewApi")
    @Override
    public <T> Future<T> submit(Runnable task, T result, Priority priority) {
        if (task == null) throw new NullPointerException();
        if (aboveJava6()) {
            return submitInternal(newTaskFor(task, result), priority);
        } else {
            return submitInternal(new FutureTask<T>(task, result), priority);
        }
    }

    /**
     * Same as {@link #submit(Callable)}, with corresponding priority.
     *
     * @throws RejectedExecutionException
     * @throws NullPointerException
     */
    @SuppressLint("NewApi")
    @Override
    public <T> Future<T> submit(Callable<T> task, Priority priority) {
        if (task == null) throw new NullPointerException();
        if (aboveJava6()) {
            return submitInternal(newTaskFor(task), priority);
        } else {
            return submitInternal(new FutureTask<T>(task), priority);
        }
    }

    private <T, V extends Runnable & Future<T>> Future<T> submitInternal(V task, Priority priority) {
        execute(task, priority);
        return task;
    }

    private static boolean aboveJava6() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    private static class PriorityRunnable<T> implements Runnable, Comparable<PriorityRunnable> {

        private static final AtomicLong SEQ = new AtomicLong(0);

        private final Runnable mRunnable;

        /**
         * the bigger, the prior.
         */
        private final int mPriority;

        /**
         * whether fifo(with same {@link #mPriority}).
         */
        private final boolean mFifo;

        /**
         * seq number.
         */
        private final long mSeqNum;

        PriorityRunnable(Runnable runnable, Priority p) {
            mRunnable = runnable;
            mPriority = p.priority;
            mFifo = p.fifo;
            mSeqNum = SEQ.getAndIncrement();
        }

        @Override
        public void run() {
            mRunnable.run();
        }

        @Override
        public int compareTo(PriorityRunnable another) {
            return mPriority > another.mPriority ? -1 : (mPriority < another.mPriority ? 1 : subCompareTo(another));
        }

        private int subCompareTo(PriorityRunnable another) {
            int result = mSeqNum < another.mSeqNum ? -1 : (mSeqNum > another.mSeqNum ? 1 : 0);
            return mFifo ? result : -result;
        }
    }
}
