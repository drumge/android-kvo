package com.drumge.kvo.inner;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.drumge.kvo.api.KvoEvent;
import com.drumge.kvo.inner.log.KLog;
import com.drumge.kvo.inner.weak.WeakSourceTargetSet;
import com.drumge.kvo.inner.weak.WeakSourceWrap;

import java.util.List;
import java.util.Map;

/**
 * Created by chenrenzhan on 2019/3/9.
 *
 * @hide
 */

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class KvoImp {
    private static final String TAG = "KvoImp";

    private final WeakSourceTargetSet mSourceTarget = new WeakSourceTargetSet();

    private static class InstanceHolder {
        static KvoImp instance = new KvoImp();
    }

    public static KvoImp getInstance() {
        return KvoImp.InstanceHolder.instance;
    }

    private KvoImp() {
    }

    /**
     * 代码注入使用，业务不要直接使用
     * 通知观察者
     * @param source 被观察实体类实例
     * @param name 被观察属性名字 field.name
     * @param oldValue 被观察属性更新前的值
     * @param newValue 被观察属性更新后的值
     *
     */
    @SuppressWarnings("unchecked")
    public <S, V> void notifyWatcher(@NonNull S source, @NonNull String name, V oldValue, V newValue) {
        if(!KvoUtils.deepEquals(oldValue, newValue)) {
            Map<WeakSourceWrap, List<IKvoTargetProxy>> entrySet = mSourceTarget.findWatcher(source);
            if (entrySet != null && !entrySet.isEmpty()) {
                for (Map.Entry<WeakSourceWrap, List<IKvoTargetProxy>> pair : entrySet.entrySet()) {
                    notifyWatcher(pair.getKey(), pair.getValue(), name, oldValue, newValue);
                }
            }
        }
    }

    private static <S, V> void notifyWatcher(@NonNull WeakSourceWrap<S> wrap,  List<IKvoTargetProxy> targets,
                                      @NonNull String name, V oldValue, V newValue) {
        KLog.debug(TAG, "notifyWatcher wrap: %s, targets.size: %d, name: %s, oldValue: %s, newValue: %s",
                wrap, targets == null ? 0 : targets.size(), name, oldValue, newValue);
        S source = null;
        if ((source = wrap.getSource()) == null) {
            KLog.error(TAG, "notifyWatcher source is null in WeakSourceWrap: %s, name: %s, oldValue: %s, newValue: %s", wrap, name, oldValue, newValue);
            return;
        }
        if (targets == null || targets.size() == 0) {
            KLog.error(TAG, "notifyWatcher target is empty with WeakSourceWrap: %s, name: %s, oldValue: %s, newValue: %s", wrap, name, oldValue, newValue);
            return;
        }
        KvoEvent<S, V> event = KvoEvent.newEvent(source, oldValue, newValue, wrap.getTag());
        for (IKvoTargetProxy kt : targets) {
            kt.notifyWatcher(name, event);
        }
    }

    /**
     * 绑定观察者
     * @param target @KvoWatch 修饰观察者所在实例
     * @param source @KvoSource 修饰的被观察对象的实例
     * @param tag 标识指定观察对象的实例
     * @param notifyWhenBind  绑定时是否通知观察者
     */
    public <S> void bind(@NonNull Object target, @NonNull S source, String tag, boolean notifyWhenBind) {
//        KLog.debug(TAG, "bind tag: %s, notifyWhenBind: %b, target: %s, source: %s", tag, notifyWhenBind, target, source);
        IKvoTargetProxy kt = KvoTargetProxyCreator.getInstance().createTargetProxy(target);
        if (kt == null) {
            KLog.error(TAG, "bind createTargetProxy is null");
            return;
        }
        mSourceTarget.add(kt, source, tag);

        if (notifyWhenBind) {
            // 绑定时通知观察者，初始值 oldValue = null 和 newValue 跟 source 中的值相等，预编译完成初始值赋值
            KvoEvent<S, Object> event = KvoEvent.newEvent(source, null, null, tag);
            kt.notifyWatcher(IKvoTargetProxy.INIT_METHOD_NAME, event);
        }
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
//        KLog.debug(TAG, "unbindAll target: %s", target);
        mSourceTarget.removeAll(target);
    }

    public  <S> void doUnbind(@NonNull Object target, @Nullable S source, String tag) {
//        KLog.debug(TAG, "doUnbind tag: %s, target: %s, source: %s", tag, target, source);
        mSourceTarget.removeAll(target, source, tag);
    }
}
