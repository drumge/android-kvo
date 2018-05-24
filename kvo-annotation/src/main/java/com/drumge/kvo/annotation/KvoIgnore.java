package com.drumge.kvo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by chenrenzhan on 2018/4/29.
 *
 * 修饰属性，当使用了 @KvoSource(check=true) 时，类中的属性不能是 public ，可给 public 属性添加此注解忽略检查
 */

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface KvoIgnore {
}
