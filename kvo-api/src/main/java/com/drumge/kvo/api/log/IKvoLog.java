package com.drumge.kvo.api.log;

/**
 * Created by chenrenzhan on 2018/5/1.
 */

public interface IKvoLog {
    void debug(Object tag, String format, Object... args);
    void info(Object tag, String format, Object... args);
    void warn(Object tag, String format, Object... args);
    void error(Object tag, String format, Object... args);
    void error(Object tag, Throwable t);
}
