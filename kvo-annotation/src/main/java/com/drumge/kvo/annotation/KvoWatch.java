package com.drumge.kvo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by chenrenzhan on 2018/4/29.
 *
 * 修饰观察 @KvoSource 中的指定的一个属性
 */

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface KvoWatch {
    enum Thread {
        DEFAULT, // 当前线程
        MAIN, // UI 线程
        WORK // 异步线程
    }

    String tag() default ""; // 标识同对象不同实例
    String name() default ""; // 观察属性的标识
    Thread thread() default Thread.DEFAULT;
}
