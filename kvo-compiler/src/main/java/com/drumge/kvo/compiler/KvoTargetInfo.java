package com.drumge.kvo.compiler;

import android.support.annotation.NonNull;


import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Element;

/**
 * Created by chenrenzhan on 2018/5/1.
 * @hide
 */

public class KvoTargetInfo {
    public final String packageName;
    public final String simpleName;
    public Element target;
    public Set<ExecutableElement> methods;

    public KvoTargetInfo(@NonNull String pn, @NonNull String sn) {
        packageName = pn;
        simpleName = sn;
    }

    public void setTarget(Element target) {
        this.target = target;
    }

    public void addMethod(ExecutableElement element) {
        if (methods == null) {
            methods = new HashSet<>();
        }
        methods.add(element);
    }

    public boolean equals(String packageName, String simpleName) {
        return this.packageName.equals(packageName) && this.simpleName.equals(simpleName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof KvoTargetInfo) {
            KvoTargetInfo info = (KvoTargetInfo) o;
            return equals(info.packageName, info.simpleName);
        }
        return false;
    }
}
