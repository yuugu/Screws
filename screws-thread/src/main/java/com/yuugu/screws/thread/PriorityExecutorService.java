package com.yuugu.screws.thread;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * Author: yuugu
 * Date: 2018/11/29
 * Time: 14:03 PM
 * An executor service which provide the feature of task with priority.
 */
public interface PriorityExecutorService extends ExecutorService {

    /**
     * Priority definition for {@link PriorityExecutorService}.
     * {@link #priority} determines the basic level, {@link #fifo} determines whether
     * fifo or filo is used within same {@link #priority}.
     */
    class Priority {
        /**
         * Default low priority with priority is -16, fifo is true.
         */
        public final static Priority LOW = new Priority(-16, true);

        /**
         * Default normal priority with priority is 0, fifo is true.
         */
        public final static Priority NORMAL = new Priority(0, true);

        /**
         * Default high priority with priority is 16, fifo is false.
         */
        public final static Priority HIGH = new Priority(16, false);

        public final int priority;
        public final boolean fifo;

        public Priority(int priority, boolean fifo) {
            this.priority = priority;
            this.fifo = fifo;
        }
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
    void execute(Runnable command, Priority priority);

    /**
     * Same as {@link #submit(Runnable)}, with corresponding priority.
     *
     * @throws RejectedExecutionException
     * @throws NullPointerException
     */
    Future<?> submit(Runnable task, Priority priority);

    /**
     * Same as {@link #submit(Runnable, T)}, with corresponding priority.
     *
     * @throws RejectedExecutionException
     * @throws NullPointerException
     */
    <T> Future<T> submit(Runnable task, T result, Priority priority);

    /**
     * Same as {@link #submit(Callable)}, with corresponding priority.
     *
     * @throws RejectedExecutionException
     * @throws NullPointerException
     */
    <T> Future<T> submit(Callable<T> task, Priority priority);
}
