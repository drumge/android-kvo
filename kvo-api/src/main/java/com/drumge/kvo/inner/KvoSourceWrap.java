package com.drumge.kvo.inner;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

/**
 * Created by chenrenzhan on 2018/5/2.
 *
 * @hide
 */

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class KvoSourceWrap<S> {
    final S source;
    final String tag;

    public KvoSourceWrap(@NonNull S source, String tag) {
        this.source = source;
        this.tag = tag == null ? "" : tag;
    }

    public S getSource() {
        return source;
    }

    public String getTag() {
        return tag;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof KvoSourceWrap) {
            return source == ((KvoSourceWrap) obj).source && tag.equals(((KvoSourceWrap) obj).tag);
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + source.hashCode();
        result = prime * result + tag.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "KvoSourceWrap{" +
                "source=" + source +
                ", tag='" + tag + '\'' +
                '}';
    }
}
