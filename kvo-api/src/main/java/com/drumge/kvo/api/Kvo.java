package com.drumge.kvo.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.drumge.kvo.api.log.IKvoLog;
import com.drumge.kvo.api.runtime.IKvoRuntime;
import com.drumge.kvo.api.runtime.KvoRuntimeSimple;
import com.drumge.kvo.api.thread.IKvoThread;
import com.drumge.kvo.inner.IKvoSource;
import com.drumge.kvo.inner.IKvoTarget;
import com.drumge.kvo.inner.KvoImp;
import com.drumge.kvo.inner.log.KLog;
import com.drumge.kvo.inner.thread.KvoThread;

/**
 * Created by chenrenzhan on 2018/5/1.
 */

public class Kvo {
    private static final String TAG = "Kvo";

    private IKvoRuntime mKvoRuntime;

    private static class InstanceHolder {
        static Kvo instance = new Kvo();
    }

    public static Kvo getInstance() {
        return Kvo.InstanceHolder.instance;
    }

    private Kvo() {
    }

    public void setLog(IKvoLog log) {
        KLog.init(log);
    }

    public void setThread(IKvoThread thread) {
        KvoThread.getInstance().init(thread);
    }

    public void setRumtime(IKvoRuntime runtime) {
        mKvoRuntime = runtime == null ? new KvoRuntimeSimple() : runtime;
    }

    /**
     * 绑定观察者
     * @param target 使用了 @KvoWatch 监听属性变化的对象实例
     * @param source 使用了 @KvoSource 修饰的对象，有可被监听的属性
     */
    public <S> void bind(@NonNull Object target, @NonNull S source) {
        bind(target, source, "", true);
    }

    /**
     * 绑定观察者
     * @param target 使用了 @KvoWatch 监听属性变化的对象实例
     * @param source 使用了 @KvoSource 修饰的对象，有可被监听的属性
     * @param tag 不同的tag区分相同KvoSource对象不同的实例
     */
    public <S> void bind(@NonNull Object target, @NonNull S source, String tag) {
        bind(target, source, tag, true);
    }

    /**
     * 绑定观察者
     * @param target 使用了 @KvoWatch 监听属性变化的对象实例
     * @param source 使用了 @KvoSource 修饰的对象，有可被监听的属性
     * @param notifyWhenBind  绑定时是否通知观察者
     */
    public <S> void bind(@NonNull Object target, @NonNull S source, boolean notifyWhenBind) {
        bind(target, source, "", notifyWhenBind);
    }

    /**
     * 绑定观察者
     * @param target @KvoWatch 修饰观察者所在实例
     * @param source @KvoSource 修饰的被观察对象的实例
     * @param tag 标识指定观察对象的实例
     * @param notifyWhenBind  绑定时是否通知观察者
     */
    public <S> void bind(@NonNull Object target, @NonNull S source, String tag, boolean notifyWhenBind) {
        KLog.debug(TAG, "bind tag: %s, notifyWhenBind: %b, target: %s, source: %s", tag, notifyWhenBind, target, source);
        checkTargetIllegal(target);
        checkSourceIllegal(source);

        KvoImp.getInstance().bind(target, source, tag, notifyWhenBind);
    }

    /**
     * 解绑观察者
     * @param target
     * @param source
     */
    public <S> void unbind(@NonNull Object target, @NonNull S source) {
        doUnbind(target, source, "");
    }

    /**
     * 解绑观察者
     * @param target @KvoWatch 修饰观察者所在实例
     * @param tag 标识指定观察对象的实例
     *
     */
    public <S> void unbind(@NonNull Object target, @NonNull S source, String tag) {
        doUnbind(target, source, tag);
    }

    /**
     * 解绑 target 下的所有观察者
     * @param target @KvoWatch 修饰观察者所在实例
     *
     */
    public void unbindAll(@NonNull Object target) {
        KLog.debug(TAG, "unbindAll target: %s", target);
        checkTargetIllegal(target);

        KvoImp.getInstance().unbindAll(target);
    }

    private  <S> void doUnbind(@NonNull Object target, @Nullable S source, String tag) {
        KLog.debug(TAG, "doUnbind tag: %s, target: %s, source: %s", tag, target, source);
        checkTargetIllegal(target);
        checkSourceIllegal(source);

        KvoImp.getInstance().doUnbind(target, source, tag);
    }

    private <S> void checkSourceIllegal(S source) {
        if (source != null && !(source instanceof IKvoSource)) {
            String msg = String.format("source(%s) must be Object with @KvoSource annotation", source.getClass());
            IllegalArgumentException e = new IllegalArgumentException(msg);
            if (mKvoRuntime.isRelease()) {
                KLog.error(TAG, e);
            } else {
                throw e;
            }
        }
    }

    private <T> void checkTargetIllegal(T target) {
        if (target != null && !(target instanceof IKvoTarget)) {
            String msg = String.format("target(%s) must be Object with @KvoWatch annotation to annotate method", target.getClass());
            IllegalArgumentException e = new IllegalArgumentException(msg);
            if (mKvoRuntime.isRelease()) {
                KLog.error(TAG, e);
            } else {
                throw e;
            }
        }
    }
}
