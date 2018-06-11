package com.drumge.kvo.compiler;

import java.util.List;

import javax.lang.model.element.TypeElement;

/**
 * Created by chenrenzhan on 2018/6/11.
 */

public class KvoSourceInfo {
    String className;
    TypeElement clsElement;
    List<TypeElement> innerCls;
}
