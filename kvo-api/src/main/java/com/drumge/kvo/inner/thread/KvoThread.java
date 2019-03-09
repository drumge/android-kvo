package com.drumge.kvo.inner.thread;



import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.drumge.kvo.inner.log.KLog;
import com.drumge.kvo.api.thread.IKvoThread;


/**
 * Created by chenrenzhan on 2018/5/1.
 *
 * @hide
 */

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class KvoThread {
    private static final String TAG = "KvoThread";
    private static final KvoThread ourInstance = new KvoThread();
    private IKvoThread kThread;

//    private Handler mainHandler;
//    private Handler threadHandler;

    public static KvoThread getInstance() {
        return ourInstance;
    }

    private KvoThread() {
        initThread();
    }

    public void init(IKvoThread thread) {
        kThread = thread;
        if (thread != null) {
//            mainHandler = null;
//            threadHandler = null;
        } else {
            KLog.error(TAG, "you must implements IKvoThread and call Kvo.getInstance().setThread(IKvoThread) to init the thread");
            initThread();
            throw new IllegalArgumentException("you did not init IKvoThread, you must implements IKvoThread and call Kvo.getInstance().setThread(IKvoThread) to init the thread");

        }
    }

    private void initThread() {
//        mainHandler = new Handler(Looper.getMainLooper());
//        HandlerThread hd = new HandlerThread("kvo-thread");
//        hd.start();
//        threadHandler = new Handler(hd.getLooper());
    }

    public void mainThread(@NonNull Runnable task) {
        if (kThread != null) {
            kThread.mainThread(task);
        } else {
//            KLog.error(TAG, "you did not init IKvoThread, you must implements IKvoThread and call Kvo.getInstance().setThread(IKvoThread) to init the thread");
//            mainHandler.post(task);
            throw new IllegalArgumentException("you did not init IKvoThread, you must implements IKvoThread and call Kvo.getInstance().setThread(IKvoThread) to init the thread");

        }
    }

    public void mainThread(@NonNull Runnable task, long delay) {
        if (kThread != null) {
            kThread.mainThread(task, delay);
        } else {
//            KLog.error(TAG, "you did not init IKvoThread, you must implements IKvoThread and call Kvo.getInstance().setThread(IKvoThread) to init the thread");
//            mainHandler.postDelayed(task, delay);
            throw new IllegalArgumentException("you did not init IKvoThread, you must implements IKvoThread and call Kvo.getInstance().setThread(IKvoThread) to init the thread");

        }
    }

    public void workThread(@NonNull Runnable task) {
        if (kThread != null) {
            kThread.workThread(task);
        } else {
//            KLog.error(TAG, "you did not init IKvoThread, you must implements IKvoThread and call Kvo.getInstance().setThread(IKvoThread) to init the thread");
//            threadHandler.post(task);
            throw new IllegalArgumentException("you did not init IKvoThread, you must implements IKvoThread and call Kvo.getInstance().setThread(IKvoThread) to init the thread");

        }
    }

    public void workThread(@NonNull Runnable task, long delay) {
        if (kThread != null) {
            kThread.workThread(task, delay);
        } else {
//            KLog.error(TAG, "you did not init IKvoThread, you must implements IKvoThread and call Kvo.getInstance().setThread(IKvoThread) to init the thread");
//            threadHandler.postDelayed(task, delay);
            throw new IllegalArgumentException("you did not init IKvoThread, you must implements IKvoThread and call Kvo.getInstance().setThread(IKvoThread) to init the thread");
        }
    }
}
