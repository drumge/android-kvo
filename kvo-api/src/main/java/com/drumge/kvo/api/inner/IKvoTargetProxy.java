package com.drumge.kvo.api.inner;

import com.drumge.kvo.api.KvoEvent;

/**
 * Created by chenrenzhan on 2018/5/2.
 *
 * 代码插入使用，不要使用该接口
 *
 * @hide
 */

public interface IKvoTargetProxy<T> {
    void notifyWatcher(final String name, KvoEvent event);
}
