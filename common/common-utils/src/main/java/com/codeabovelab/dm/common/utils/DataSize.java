/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.common.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/**
 */
public final class DataSize {

    public static final long KiB = 1L << 10L;
    public static final long MiB = 1L << 20L;
    public static final long GiB = 1L << 30L;
    public static final long TiB = 1L << 40L;

    private DataSize() {
    }

    public static long fromString(String str) {
        int end;
        for(end = 0; end < str.length(); ++end) {
            char c = str.charAt(end);
            // if char is not a part of number
            if(!(c >= '0' && c <= '9') && c != '-' && c != '+' && c != '.') {
                break;
            }
        }
        if(end == str.length()) {
            return Long.parseLong(str);
        }
        double rem = Double.parseDouble(str.substring(0, end));
        double mult = parseMult(str, end);
        return (long)(rem * mult);
    }

    private static long parseMult(String full, int from) {
        int length = full.length() - from;
        if(length == 0) {
            return 1;
        }
        if(length == 2) {
            char b = full.charAt(from + 1);
            if(b != 'b' && b != 'B') {
                throw new IllegalArgumentException("Invalid memory string: " + full);
            }
        }
        if(length == 3) {
            char i = full.charAt(from + 1);
            char b = full.charAt(from + 2);
            if(i != 'i' && i != 'I'|| b != 'b' && b != 'B') {
                throw new IllegalArgumentException("Invalid memory string: " + full);
            }
        }
        char m = full.charAt(from);
        switch (m) {
            case 'K':
            case 'k':
                return KiB;
            case 'M':
            case 'm':
                return MiB;
            case 'G':
            case 'g':
                return GiB;
            case 'T':
            case 't':
                return TiB;
            default:
                throw new IllegalArgumentException("Invalid memory string: " + full);
        }
    }

    /**
     * Parse literals like KiB or KB to its number representation 1024 (for KB & KiB)
     * @param mult str
     * @return long value
     */
    public static long parseMultiplier(String mult) {
        return parseMult(mult, 0);
    }

    public static String toString(long src) {
        if(src == 0) {
            return "0";
        }
        long i;
        {
            long tmp = src;
            if(tmp < 0) {
                tmp = ~tmp + 1;
            }
            i = Long.numberOfTrailingZeros(Long.highestOneBit(tmp));
        }
        NumberFormat formatter = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
        double rem = src;
        if(i < 10) {
            return formatter.format(rem);
        }
        String suff;
        if(i < 20) {
            rem /= KiB;
            suff = "KiB";
        } else if(i < 30) {
            rem /= MiB;
            suff = "MiB";
        } else if(i < 40) {
            rem /= GiB;
            suff = "GiB";
        } else {
            rem /= TiB;
            suff = "TiB";
        }
        return formatter.format(rem) + suff;
    }
}
