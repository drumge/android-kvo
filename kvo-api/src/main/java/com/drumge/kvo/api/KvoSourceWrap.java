package com.drumge.kvo.api;

import android.support.annotation.NonNull;

/**
 * Created by chenrenzhan on 2018/5/2.
 *
 * @hide
 */

class KvoSourceWrap<S> {
    final S source;
    final String tag;

    public KvoSourceWrap(@NonNull S source, @NonNull String tag) {
        this.source = source;
        this.tag = tag == null ? "" : tag;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof KvoSourceWrap) {
            return source == ((KvoSourceWrap) obj).source && tag.equals(((KvoSourceWrap) obj).tag);
        }
        return false;
    }

    @Override
    public String toString() {
        return "KvoSourceWrap{" +
                "source=" + source +
                ", tag='" + tag + '\'' +
                '}';
    }
}
