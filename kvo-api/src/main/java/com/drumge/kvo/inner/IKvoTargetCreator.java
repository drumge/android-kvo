package com.drumge.kvo.inner;

import android.support.annotation.RestrictTo;

/**
 * Created by chenrenzhan on 2018/5/7.
 *
 * 辅助实现功能，业务不要使用
 *
 * @hide
 */

@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface IKvoTargetCreator<P extends IKvoTargetProxy, T> {
    P createTarget(T target);
}
