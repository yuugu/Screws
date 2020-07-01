package com.yuugu.screws.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: yuugu
 * Date: 2018/11/29
 * Time: 12:03 AM
 */
public class PriorityThreadFactory implements ThreadFactory {

    private final String mName;
    private final int mPriority;
    private final AtomicInteger mNumber = new AtomicInteger();

    public PriorityThreadFactory(String name, int priority) {
        mName = name;
        mPriority = priority;
    }

    public Thread newThread(Runnable r) {
        return new Thread(r, mName + '-' + mNumber.getAndIncrement()) {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(mPriority);
                super.run();
            }
        };
    }
}
