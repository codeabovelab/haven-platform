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

package com.codeabovelab.dm.common.kv;

import java.util.Arrays;

/**
 */
public class KvUtils {

    public static boolean predicate(String pattern, String key) {
        int last = pattern.length();
        final int keyLen = key.length();
        if(pattern.charAt(last - 1) == '*') {
            //we check that key start with pattern, but without '*'
            last--;
        }
        if(keyLen == last - 1 && pattern.charAt(last - 1) == '/') {
            //when operation act on current node then key does not contain a end slash
            last--;
        }
        return key.regionMatches(0, pattern, 0, last);
    }

    /**
     * Utility which correct join path components. <p/>
     * Accept any '/component/' with or without '/' at ends and join they in correct '/component1/component2/.../componentN/' path.
     * @param components
     * @return
     */
    public static String join(String ... components) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < components.length; ++i) {
            String component = components[i];
            if(component == null) {
                throw new IllegalArgumentException("Null component at " + i + " in " + Arrays.toString(components));
            }
            final int lastChar = sb.length() - 1;
            if(component.charAt(0) != '/' && (lastChar < 0 || sb.charAt(lastChar) != '/')) {
                sb.append('/');
            }
            sb.append(component);
        }
        if(sb.length() > 0 && sb.charAt(sb.length() - 1) != '/') {
            sb.append('/');
        }
        return sb.toString();
    }


    /**
     * Return path relative to prefix (its suffix, but without leading slash).
     * @param prefix
     * @param path
     * @return
     */
    public static String suffix(String prefix, String path) {
        if(!path.startsWith(prefix)) {
            return null;
        }

        int end = prefix.length();
        if(path.length() > end && path.charAt(end) == '/') {
            end++;
        }
        return path.substring(end);
    }

    /**
     * Name of first path element after prefix.
     * @param prefix
     * @param path
     * @return name or null
     */
    public static String name(String prefix, String path) {
        String tmp = KvUtils.suffix(prefix, path);
        if(tmp == null) {
            return null;
        }
        int i = tmp.indexOf('/');
        if(i == 0) {
            throw new RuntimeException("Incorrect path: " + path + " and prefix:" + prefix);
        }
        if(i > 0) {
            tmp = tmp.substring(0, i);
        }
        return tmp;
    }
}
