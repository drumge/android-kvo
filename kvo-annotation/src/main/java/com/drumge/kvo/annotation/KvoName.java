package com.drumge.kvo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by chenrenzhan on 2018/5/2.
 *
 * 用于被观察的属性指定特定的字符串标识，没有此注解默认使用属性名字作为属性的标识
 */

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface KvoName {
    String name();
}
