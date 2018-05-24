package com.drumge.kvo.plugin

import javassist.CtField
import javassist.CtMethod

/**
 * Created by chenrenzhan on 2018/5/3.
 */

class SourceBindInfo {
    final String bindName
    final CtField field
    final CtMethod bindMethod

    SourceBindInfo(CtField field, String bindName, CtMethod bindMethod) {
        this.field = field
        this.bindName = bindName
        this.bindMethod = bindMethod
    }

    @Override
    boolean equals(Object obj) {
        if (this == obj) {
            return true
        }
        if (obj instanceof SourceBindInfo) {
            SourceBindInfo info = (SourceBindInfo) obj
            return field.name == info.field.name
        }
        return false
    }

    @Override
    String toString() {
        return "SourceBindInfo{" +
                "field='" + field + '\'' +
                ", bindName='" + bindName + '\'' +
                ", bindMethod=" + bindMethod +
                '}'
    }
}
