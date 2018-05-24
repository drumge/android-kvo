package com.drumge.kvo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by chenrenzhan on 2018/5/1.
 *
 * 绑定指定属性的set方法，没有此注解默认使用set+属性标识(首字母大写)
 */

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface KvoBind {
    String name(); // 绑定的属性的标识
}
