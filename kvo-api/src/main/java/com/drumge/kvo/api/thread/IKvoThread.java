package com.drumge.kvo.api.thread;

import android.support.annotation.NonNull;

/**
 * Created by chenrenzhan on 2018/5/1.
 */

public interface IKvoThread {

    // main 线程执行
    void mainThread(@NonNull Runnable task);
    void mainThread(@NonNull Runnable task, long delay);

    // 非 main 线程的子线程执行
    void workThread(@NonNull Runnable task);
    void workThread(@NonNull Runnable task, long delay);
}
