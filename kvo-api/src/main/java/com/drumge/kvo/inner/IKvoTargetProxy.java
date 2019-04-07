package com.drumge.kvo.inner;

import android.support.annotation.RestrictTo;

import com.drumge.kvo.api.KvoEvent;

/**
 * Created by chenrenzhan on 2018/5/2.
 *
 * 代码插入使用，不要使用该接口
 *
 * @hide
 */

@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface IKvoTargetProxy<T> {
    String INIT_METHOD_NAME = "kvo_init_method_name";

    void notifyWatcher(final String name, KvoEvent event);
    boolean equalsTarget(Object obj);
    boolean isTargetValid();
}
