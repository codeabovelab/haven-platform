/*
 * Copyright 2016 Code Above Lab LLC
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

package com.codeabovelab.dm.common.utils;

import java.util.*;

/**
 */
public final class Booleans {

    private static final Map<String, Boolean> MAP;

    static{
        Map<String, Boolean> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        map.put("true", true);
        map.put("ok", true);
        map.put("yes", true);
        map.put("on", true);
        map.put("1", true);

        map.put("false", false);
        map.put("no", false);
        map.put("off", false);
        map.put("0", false);
        MAP = Collections.unmodifiableMap(map);
    }

    private Booleans() {
    }

    public static boolean parse(String s) {
        if(s == null) {
            return false;
        }
        Boolean res = MAP.get(s);
        return res != null && res;
    }

    public static boolean valueOf(Object o) {
        return o != null && (
          o instanceof Boolean && (Boolean)o ||
          o instanceof String && parse((String)o) ||
          o instanceof Number && ((Number)o).intValue() != 0
        );
    }
}
