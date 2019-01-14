package com.drumge.kvo.api.inner;

/**
 * Created by chenrenzhan on 2018/5/3.
 *
 * 代码插入使用，不要使用该接口
 *
 * @hide
 */

public interface IKvoSource {
    boolean _addKvoSourceTag(String tag);
    boolean _removeKvoSourceTag(String tag);
    boolean _containKvoSourceTag(String tag);
}
