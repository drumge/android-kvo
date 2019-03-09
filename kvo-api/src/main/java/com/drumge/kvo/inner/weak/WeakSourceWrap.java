package com.drumge.kvo.inner.weak;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Created by chenrenzhan on 2019/2/2.
 *
 * @hide
 */

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class WeakSourceWrap<S> {
    final WeakReference<S> weakSource;
    final String tag;

    private int hashCode;

    public WeakSourceWrap(@NonNull S source, String tag, ReferenceQueue<? super S> queue) {
        this.weakSource = new WeakReference<>(source, queue);
        this.tag = tag;

        hashCode = hashCode(source, tag);
    }

    @Nullable
    public S getSource() {
        return weakSource.get();
    }

    public String getTag() {
        return tag;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WeakSourceWrap) {
            return weakSource.get() == ((WeakSourceWrap) obj).weakSource.get() && tag.equals(((WeakSourceWrap) obj).tag);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private int hashCode(@NonNull S source, String tag) {
        int result = 1;
        result = 17 * result + source.hashCode();
        result = 19 * result + tag.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "WeakSourceWrap{" +
                "source=" + weakSource.get() +
                ", tag='" + tag + '\'' +
                '}';
    }
}
