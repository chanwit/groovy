/*
 * Copyright 2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.util;

import java.util.Arrays;

/**
 * A utility class to help calculate hashcode values
 * using an algorithm similar to that outlined in
 * "Effective Java, Joshua Bloch, 2nd Edition".
 */
public class HashCodeHelper {
    private static final int SEED = 127;
    private static final int MULT = 31;

    public static int initHash() {
        return SEED;
    }

    public static int updateHash(int current, boolean var) {
        return shift(current) + (var ? 1 : 0);
    }

    public static int updateHash(int current, char var) {
        return shift(current) + (int) var;
    }

    public static int updateHash(int current, int var) {
        return shift(current) + var;
    }

    public static int updateHash(int current, long var) {
        return shift(current) + (int) (var ^ (var >>> 32));
    }

    public static int updateHash(int current, float var) {
        return updateHash(current, Float.floatToIntBits(var));
    }

    public static int updateHash(int current, double var) {
        return updateHash(current, Double.doubleToLongBits(var));
    }

    public static int updateHash(int current, Object var) {
        if (var == null) return updateHash(current, 0);
        return updateHash(current, var.hashCode());
    }

    public static int updateHash(int current, boolean[] var) {
        if (var == null) return updateHash(current, 0);
        return shift(current) + Arrays.hashCode(var);
    }

    public static int updateHash(int current, char[] var) {
        if (var == null) return updateHash(current, 0);
        return shift(current) + Arrays.hashCode(var);
    }

    public static int updateHash(int current, byte[] var) {
        if (var == null) return updateHash(current, 0);
        return shift(current) + Arrays.hashCode(var);
    }

    public static int updateHash(int current, short[] var) {
        if (var == null) return updateHash(current, 0);
        return shift(current) + Arrays.hashCode(var);
    }

    public static int updateHash(int current, int[] var) {
        if (var == null) return updateHash(current, 0);
        return shift(current) + Arrays.hashCode(var);
    }

    public static int updateHash(int current, long[] var) {
        if (var == null) return updateHash(current, 0);
        return shift(current) + Arrays.hashCode(var);
    }

    public static int updateHash(int current, float[] var) {
        if (var == null) return updateHash(current, 0);
        return shift(current) + Arrays.hashCode(var);
    }

    public static int updateHash(int current, double[] var) {
        if (var == null) return updateHash(current, 0);
        return shift(current) + Arrays.hashCode(var);
    }

    public static int updateHash(int current, Object[] var) {
        if (var == null) return updateHash(current, 0);
        return shift(current) + Arrays.hashCode(var);
    }

    private static int shift(int current) {
        return MULT * current;
    }

}
