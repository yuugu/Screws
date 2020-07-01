package com.yuugu.screws.thread;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: yuugu
 * Date: 13-12-24
 * Time: 下午8:58
 * <p>WorkerTask enables execution and callback on a worker thread, and never block other threads.</p>
 * <p>Override {@link #onExecute} to do background work. Call {@link #scheduleExecute} wherever
 * you want to schedule next execution on a worker thread. Call {@link #scheduleFinish} wherever
 * you want to schedule finish of this task on a worker thread.</p>
 */
public abstract class WorkerTask<Result> implements Externalizable {

    private static final long serialVersionUID = 2457802695193039591L;

    /**
     * Callback which will be called when task finished.
     */
    public interface Callback<Result> {
        void onTaskDone(WorkerTask<Result> task, Result result);
    }

    /**
     * Task priority definition.
     */
    public final static class Priority extends PriorityExecutorService.Priority {

        public Priority(int priority, boolean fifo) {
            super(priority, fifo);
        }
    }

    public final static int STATUS_PENDING = 0;
    public final static int STATUS_RUNNING = 1;
    public final static int STATUS_SUCCEED = 2;
    public final static int STATUS_FAILED = 3;

    public final static int NO_ID = -1;

    // final is not permitted for Externalizable.
    private /*final*/ int mId;
    private /*final*/ boolean mOneShot;

    private transient final Runnable mJob;

    private transient PriorityExecutorService mThreadPool;
    private transient Priority mPriority;
    private transient Callback<Result> mCallback;

    private transient final AtomicInteger mStatus = new AtomicInteger(STATUS_PENDING);

    private transient final ThreadLocal<Boolean> mWorkerThread = new ThreadLocal<>();
    private transient final ThreadLocal<Throwable> mFinishingException = new ThreadLocal<>();

    /**
     * Construct a worker task with NO id.
     */
    public WorkerTask() {
        this(NO_ID);
    }

    /**
     * Construct a worker task.
     *
     * @param id Task id.
     */
    public WorkerTask(int id) {
        this(id, true);
    }

    /**
     * Construct a worker task.
     *
     * @param id      Task id.
     * @param oneshot Whether it's oneshot.
     */
    public WorkerTask(int id, boolean oneshot) {
        mId = id;
        mOneShot = oneshot;
        mJob = new Runnable() {
            @Override
            public void run() {
                // mark worker thread.
                mWorkerThread.set(true);
                try {
                    onExecute();
                } catch (Throwable e) {
                    if (mFinishingException.get() == e) {
                        // exception during finish, just throw it.
                        throw e;
                    } else {
                        // actual execute exception.
                        onExecuteException(e);
                    }
                }
            }
        };
    }

    /**
     * Execute this task.
     *
     * @return This instance of Task.
     * @throws IllegalStateException if this task's status is NOT {@link #STATUS_PENDING}.
     */
    public final WorkerTask<Result> execute() {
        if (!mStatus.compareAndSet(STATUS_PENDING, STATUS_RUNNING)) {
            switch (mStatus.get()) {
                case STATUS_RUNNING:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task is already running.");
                case STATUS_SUCCEED:
                case STATUS_FAILED:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task has already been executed "
                            + "(a task can be executed only once)");
            }
        }

        scheduleExecute();
        return this;
    }

    /**
     * Execute this task on corresponding thread pool.
     *
     * @param threadPool Thread pool to provide worker thread.
     * @return This instance of Task.
     * @throws IllegalStateException if this task's status is NOT {@link #STATUS_PENDING}.
     */
    public final WorkerTask<Result> executeOn(PriorityExecutorService threadPool) {
        setThreadPool(threadPool);
        return execute();
    }

    /**
     * Execute this task with {@link Callback}.
     *
     * @param callback Callback to be invoked when task finished.
     * @return This instance of Task.
     * @throws IllegalStateException if this task's status is NOT {@link #STATUS_PENDING}.
     */
    public final WorkerTask<Result> execute(Callback<Result> callback) {
        setCallback(callback);
        return execute();
    }

    /**
     * Execute this task with {@link Callback} on corresponding thread pool.
     *
     * @param threadPool Thread pool to provide worker thread.
     * @param callback   Callback to be invoked when task finished.
     * @return This instance of Task.
     * @throws IllegalStateException if this task's status is NOT {@link #STATUS_PENDING}.
     */
    public final WorkerTask<Result> executeOn(PriorityExecutorService threadPool, Callback<Result> callback) {
        setThreadPool(threadPool);
        setCallback(callback);
        return execute();
    }

    /**
     * Override this method to perform a computation on a <b>worker thread</b>.
     */
    protected abstract void onExecute();

    /**
     * Called when exception occurs during {@link #onExecute}, default implementation is just throw it.
     *
     * @param e Execute exception.
     */
    protected void onExecuteException(Throwable e) {
        throw new AssertionError(e);
    }

    /**
     * Schedule the next execution of this task, which means {@link #onExecute} will
     * be called later on a <b>worker thread</b>.
     */
    protected final void scheduleExecute() {
        performExecute(mJob);
    }

    private void performExecute(Runnable job) {
        PriorityExecutorService threadPool = mThreadPool;
        Priority priority = mPriority;
        if (threadPool == null) {
            threadPool = ThreadPools.defaultThreadPool();
        }
        if (priority == null) {
            throw new RuntimeException("priority should be specified before execution");
        }
        threadPool.execute(job, priority);
    }

    /**
     * Schedule finish this task, {@link Callback} will be invoked if specified on a <b>worker thread</b>.
     *
     * @param succeed whether this task succeed.
     * @param result  The final result of this task.
     * @throws IllegalStateException if this task is a oneshot task and has already been finished.
     */
    protected final void scheduleFinish(boolean succeed, Result result) {
        try {
            int newStatus = succeed ? STATUS_SUCCEED : STATUS_FAILED;
            if (!mStatus.compareAndSet(STATUS_RUNNING, newStatus)) {
                switch (mStatus.get()) {
                    case STATUS_PENDING:
                    /*throw new IllegalStateException("Cannot finish task:"
                        + " the task is not running.");*/
                        // allow finish pending task.
                        mStatus.set(newStatus);
                        break;
                    case STATUS_SUCCEED:
                    case STATUS_FAILED:
                        if (mOneShot) {
                            throw new IllegalStateException("Cannot finish task:"
                                    + " the task has already been finished "
                                    + "(a one shot task can be finished only once)");
                        } else {
                            mStatus.set(newStatus);
                        }
                        break;
                }
            }

            // perform finish.
            performFinish(succeed, result);
        } catch (Throwable e) {
            // mark the exception during finish.
            mFinishingException.set(e);
            throw e;
        }
    }

    private void performFinish(boolean succeed, final Result result) {
        final Callback<Result> callback = mCallback;
        if (callback == null) {
            return;
        }
        Boolean workerThread = mWorkerThread.get();
        if (workerThread != null && workerThread) {
            // avoid context switch.
            callback.onTaskDone(this, result);
        } else {
            performExecute(new Runnable() {
                @Override
                public void run() {
                    callback.onTaskDone(WorkerTask.this, result);
                }
            });
        }
    }

    /**
     * Return the task id.
     */
    public final int getId() {
        return mId;
    }

    /**
     * Return the task status, see {@link #STATUS_PENDING}, {@link #STATUS_RUNNING},
     * {@link #STATUS_FAILED}, {@link #STATUS_SUCCEED}.
     */
    public final int getStatus() {
        return mStatus.get();
    }

    /**
     * Return callback of this task.
     */
    public final Callback<Result> getCallback() {
        return mCallback;
    }

    /**
     * Specify which {@link PriorityExecutorService} is used to provide worker thread.
     * Default one is {@link ThreadPools#defaultThreadPool}.
     *
     * @param threadPool Thread pool
     * @return This instance of Task.
     * @throws IllegalStateException if this task has already been executed.
     */
    public final WorkerTask<Result> setThreadPool(PriorityExecutorService threadPool) {
        throwIfExecuted("setThreadPool");
        mThreadPool = threadPool;
        return this;
    }

    /**
     * Set whether this task is one shot, which means can only been finished once.
     *
     * @param oneShot Whether one shot.
     * @return This instance of Task.
     * @throws IllegalStateException if this task has already been executed.
     */
    public final WorkerTask<Result> setOneShot(boolean oneShot) {
        throwIfExecuted("setOneShot");
        mOneShot = oneShot;
        return this;
    }

    /**
     * Specify the {@link Priority} of this task.
     *
     * @param priority Priority.
     * @return This instance of Task.
     * @throws IllegalStateException if this task has already been executed.
     */
    public final WorkerTask<Result> setPriority(Priority priority) {
        throwIfExecuted("setPriority");
        mPriority = priority;
        return this;
    }

    /**
     * Specify the {@link Callback} of this task.
     *
     * @param callback task callback.
     * @return This instance of task.
     * @throws IllegalStateException if this task has already been executed.
     */
    public final WorkerTask<Result> setCallback(Callback<Result> callback) {
        throwIfExecuted("setCallback");
        mCallback = callback;
        return this;
    }

    /**
     * @throws IllegalStateException if this task has already been executed.
     */
    private void throwIfExecuted(String name) {
        if (mStatus.get() != STATUS_PENDING) {
            throw new IllegalStateException(name + " should be called before execution of this task.");
        }
    }

    @Override
    public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
        mId = input.readInt();
        mOneShot = input.readBoolean();
    }

    @Override
    public void writeExternal(ObjectOutput output) throws IOException {
        output.writeInt(mId);
        output.writeBoolean(mOneShot);
    }
}