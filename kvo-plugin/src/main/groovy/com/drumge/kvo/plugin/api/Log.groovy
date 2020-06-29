package com.drumge.kvo.plugin.api

class Log {

    static void d(String tag, String msg, Object... p) {
        System.out.println(format("debug: [%s]	%s", tag, format(msg, p)))
    }

    static void i(String tag, String msg, Object... p) {
        System.out.println(format("info: [%s]	%s", tag, format(msg, p)))
    }

    static void w(String tag, String msg, Object... p) {
        System.out.println(format("warm: [%s]	%s", tag, format(msg, p)))
    }

    static void e(String tag, String msg, Object... p) {
        System.err.println(format("error: [%s]\t%s", tag, format(msg, p)))
    }

    private static String format(String msg, Object... format) {
        return String.format(Locale.getDefault(), "${System.currentTimeMillis()} $msg", format)
    }
}