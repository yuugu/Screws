package com.yuugu.screws.thread;

import android.annotation.TargetApi;
import android.os.Build;

import java.util.concurrent.RunnableFuture;

/**
 * Author: yuugu
 * Date: 2018/11/29
 * Time: 12:03 AM
 * Observable runnable future.
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public interface ObservableRunnableFuture<T> extends RunnableFuture<T>, ObservableFuture<T> {
}
