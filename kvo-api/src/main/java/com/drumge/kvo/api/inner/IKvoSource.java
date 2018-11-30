package com.drumge.kvo.api.inner;

/**
 * Created by chenrenzhan on 2018/5/3.
 *
 * 代码插入使用，不要使用该接口
 *
 * @hide
 */

public interface IKvoSource {
    // TODO 同一个实例多地方绑定，会重置 tag
    void _setKvoSourceTag(String tag);
    String _getKvoSourceTag();
}
