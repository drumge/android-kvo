package com.drumge.kvo.api.log;

import android.support.annotation.RestrictTo;

/**
 * Created by chenrenzhan on 2018/5/1.
 *
 * @hide
 */

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class KLog {
    private static IKvoLog kLog;

    public static void init(IKvoLog kLog) {
        KLog.kLog = kLog;
    }

    public static void debug(Object tag, String format, Object... args) {
        if (kLog != null) {
            kLog.debug(tag, format, args);
        }
    }

    public static void info(Object tag, String format, Object... args) {
        if (kLog != null) {
            kLog.info(tag, format, args);
        }
    }

    public static void warn(Object tag, String format, Object... args) {
        if (kLog != null) {
            kLog.warn(tag, format, args);
        }
    }

    public static void error(Object tag, String format, Object... args) {
        if (kLog != null) {
            kLog.error(tag, format, args);
        }
    }

    public static void error(Object tag, Throwable t) {
        if (kLog != null) {
            kLog.error(tag, t);
        }
    }
}
