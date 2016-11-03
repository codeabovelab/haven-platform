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

package com.codeabovelab.dm.cluman.cluster.registry;

/**
 */
public final class RegistryUtils {
    private RegistryUtils() {
    }

    /**
     * Extract repository name from url.
     * @param url
     * @return
     */
    public static String getNameByUrl(String url) {
        int begin = url.indexOf("://");
        if(begin == -1) {
            return null;
        }
        begin += 3 /* len of '://' */;
        int end = url.indexOf("/", begin);
        return end == -1 ? url.substring(begin) : url.substring(begin, end);
    }
}
