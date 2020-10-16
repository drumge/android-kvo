package com.drumge.kvo.plugin.api

class Log {
    static int logLevel = -1

    static void d(String tag, String msg, Object... p) {
        if (logLevel >= 1) {
            System.out.println(format("debug: [%s]	%s", tag, format(msg, p)))
        }
    }

    static void i(String tag, String msg, Object... p) {
        if (logLevel >= 2) {
            System.out.println(format("info: [%s]	%s", tag, format(msg, p)))
        }
    }

    static void w(String tag, String msg, Object... p) {
        if (logLevel >= 3) {
            System.out.println(format("warm: [%s]	%s", tag, format(msg, p)))
        }
    }

    static void e(String tag, String msg, Object... p) {
        if (logLevel >= 4) {
            System.err.println(format("error: [%s]\t%s", tag, format(msg, p)))
        }
    }

    private static String format(String msg, Object... format) {
        return String.format(Locale.getDefault(), "$msg", format)
    }
}