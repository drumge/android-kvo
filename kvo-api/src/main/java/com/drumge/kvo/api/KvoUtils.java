package com.drumge.kvo.api;

import android.support.annotation.RestrictTo;

import java.util.Arrays;
import java.util.Objects;

/**
 * Created by chenrenzhan on 2018/5/1.
 *
 * @hide
 */

@RestrictTo(RestrictTo.Scope.LIBRARY)
class KvoUtils {
    /**
     * Returns {@code true} if the arguments are deeply equal to each other
     * and {@code false} otherwise.
     *
     * Two {@code null} values are deeply equal.  If both arguments are
     * arrays, the algorithm in {@link Arrays#deepEquals(Object[],
     * Object[]) Arrays.deepEquals} is used to determine equality.
     * Otherwise, equality is determined by using the {@link
     * Object#equals equals} method of the first argument.
     *
     * @param a an object
     * @param b an object to be compared with {@code a} for deep equality
     * @return {@code true} if the arguments are deeply equal to each other
     * and {@code false} otherwise
     * @see Arrays#deepEquals(Object[], Object[])
     * @see Objects#equals(Object, Object)
     */
    public static boolean deepEquals(Object a, Object b) {
        if (a == b)
            return true;
        else if (a == null || b == null)
            return false;
        else
            return deepEquals0(a, b);
    }

    private static boolean deepEquals0(Object e1, Object e2) {
        // BEGIN Android-changed: getComponentType() is faster than instanceof()
        Class<?> cl1 = e1.getClass().getComponentType();
        Class<?> cl2 = e2.getClass().getComponentType();

        if (cl1 != cl2) {
            return false;
        }
        if (e1 instanceof Object[])
            return deepEquals ((Object[]) e1, (Object[]) e2);
        else if (cl1 == byte.class)
            return Arrays.equals((byte[]) e1, (byte[]) e2);
        else if (cl1 == short.class)
            return Arrays.equals((short[]) e1, (short[]) e2);
        else if (cl1 == int.class)
            return Arrays.equals((int[]) e1, (int[]) e2);
        else if (cl1 == long.class)
            return Arrays.equals((long[]) e1, (long[]) e2);
        else if (cl1 == char.class)
            return Arrays.equals((char[]) e1, (char[]) e2);
        else if (cl1 == float.class)
            return Arrays.equals((float[]) e1, (float[]) e2);
        else if (cl1 == double.class)
            return Arrays.equals((double[]) e1, (double[]) e2);
        else if (cl1 == boolean.class)
            return Arrays.equals((boolean[]) e1, (boolean[]) e2);
        else
            return e1.equals(e2);
        // END Android-changed: getComponentType() is faster than instanceof()
    }
}
