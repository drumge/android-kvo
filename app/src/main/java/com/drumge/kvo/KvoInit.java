package com.drumge.kvo;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.drumge.kvo.api.Kvo;
import com.drumge.kvo.api.log.IKvoLog;
import com.drumge.kvo.api.runtime.IKvoRuntime;
import com.drumge.kvo.api.thread.IKvoThread;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Created by chenrenzhan on 2018/5/23.
 */

public class KvoInit {

    public static void initKvo() {
        Kvo.getInstance().setLog(new KvoLog());
        Kvo.getInstance().setThread(new KvoThread());
        Kvo.getInstance().setRumtime(new KvoRuntime());
    }

    private static class KvoRuntime implements IKvoRuntime {

        @Override
        public boolean isRelease() {
            return false;
        }
    }

    private static class KvoThread implements IKvoThread {
        Handler mMainHandler = new Handler(Looper.getMainLooper());
        // 线程池可换成项目中统一的线程池，应用中只应存在一个全局统一的线程池，不建议创建多个线程池
        private ScheduledExecutorService mThreadPool = Executors.newSingleThreadScheduledExecutor();

        @Override
        public void mainThread(@NonNull Runnable runnable) {
            mMainHandler.post(runnable);
        }

        @Override
        public void mainThread(@NonNull Runnable runnable, long l) {
            mMainHandler.postDelayed(runnable, l);
        }

        @Override
        public void workThread(@NonNull Runnable runnable) {
            mThreadPool.schedule(runnable, 0, TimeUnit.MILLISECONDS);
        }

        @Override
        public void workThread(@NonNull Runnable runnable, long l) {
            mThreadPool.schedule(runnable, l, TimeUnit.MILLISECONDS);
        }
    }

    private static class KvoLog implements IKvoLog {

        @Override
        public void debug(Object o, String s, Object... objects) {
            Log.d(String.valueOf(o), String.format(s, objects));
        }

        @Override
        public void info(Object o, String s, Object... objects) {
            Log.i(String.valueOf(o), String.format(s, objects));
        }

        @Override
        public void warn(Object o, String s, Object... objects) {
            Log.w(String.valueOf(o), String.format(s, objects));
        }

        @Override
        public void error(Object o, String s, Object... objects) {
            Log.e(String.valueOf(o), String.format(s, objects));
        }

        @Override
        public void error(Object o, Throwable throwable) {
            Log.e(String.valueOf(o), Log.getStackTraceString(throwable));
        }
    }
}
