package com.drumge.kvo.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by chenrenzhan on 2018/5/2.
 *
 * 观察特定属性的事件， <S, V> 是必须的，分别对应 <class, field> 被观察的类，被观察的属性类型, 否则会通知不到
 */

public class KvoEvent<S, V> {
    String tag = "";

    @NonNull S source;

    @Nullable V oldValue;
    @Nullable V newValue;

    public static  <S, V> KvoEvent<S, V> newEvent(@NonNull S source, @Nullable V oldValue, @Nullable V newValue, String tag) {
        KvoEvent<S, V> event = new KvoEvent<>();
        event.tag = tag == null ? "" : tag;
        event.source = source;
        event.oldValue = oldValue;
        event.newValue = newValue;
        return event;
    }

    public String getTag() {
        return tag;
    }

    @NonNull
    public S getSource() {
        return source;
    }

    @Nullable
    public V getOldValue() {
        return oldValue;
    }

    @Nullable
    public V getNewValue() {
        return newValue;
    }
}
