package com.drumge.kvo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by chenrenzhan on 2018/4/29.
 *
 * 修饰需要被观察的属性所在的类
 */

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface KvoSource {

    boolean check() default true; // 检查 public 属性定义
}
