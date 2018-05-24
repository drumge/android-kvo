package com.drumge.kvo.api.inner;

/**
 * Created by chenrenzhan on 2018/5/7.
 *
 * 辅助实现功能，业务不要使用
 */

public interface IKvoTargetCreator<P extends IKvoTargetProxy, T> {
    P createTarget(T target);
}
