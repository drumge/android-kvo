package com.drumge.kvo.inner;

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.drumge.kvo.inner.log.KLog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by chenrenzhan on 2019/3/8.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class KvoTargetProxyCreator {
    private static final String TAG = "KvoTargetProxyCreator";

    private final Map<Class<?>, IKvoTargetCreator> mTargetCreator = new ConcurrentHashMap<>();

    private static class InstanceHolder {
        static KvoTargetProxyCreator instance = new KvoTargetProxyCreator();
    }

    public static KvoTargetProxyCreator getInstance() {
        return InstanceHolder.instance;
    }

    private KvoTargetProxyCreator() {}

    /**
     * 代码注入使用，业务不要直接使用
     * @param cls 使用了 @KvoWatch 监听属性变化的对象
     * @param creator 编译期间生成的创建 Proxy 辅助类的创建
     *
     */
    public void registerTarget(Class<?> cls, IKvoTargetCreator creator) {
        if (cls != null && creator != null) {
            mTargetCreator.put(cls, creator);
        }
    }

    @Nullable
    public IKvoTargetProxy createTargetProxy(Object target) {
//        KLog.debug(TAG, "createTargetProxy mTargetCreator: %s", mTargetCreator);
        if (target == null) {
            KLog.error(TAG, "createTargetProxy target is null");
            return null;
        }

        Class cls = target.getClass();
        IKvoTargetCreator creator = mTargetCreator.get(cls);
        if (creator != null) {
            return creator.createTarget(target);
        }
        KLog.error(TAG, "createTargetProxy creator is null, cls: %s", cls);
        return null;
    }
}
