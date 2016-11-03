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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 */
public class FindHandlerUtil {

    private FindHandlerUtil() {
    }

    /**
     * This method find appropriate handler by class hierarchy, if got none, then it find by interface hierarchy.
     * @param clazz
     * @param map
     * @param <T>
     * @param <H>
     * @return
     */
    public static <T, H> H findByClass(Class<T> clazz, Map<? extends Class<?>, ? extends H> map) {
        Class<?> c = clazz;
        List<Class<?>> ifaces = new ArrayList<>();
        while(c != null) {
            H handler = map.get(c);
            if(handler != null) {
                return handler;
            }
            ifaces.addAll(Arrays.asList(c.getInterfaces()));
            c = c.getSuperclass();
        }
        //note that ifaces can be duplicated in list
        for(Class<?> iface: ifaces) {
            while(iface != null) {
                H handler = map.get(iface);
                if(handler != null) {
                    return handler;
                }
                iface = iface.getSuperclass();
            }
        }
        return null;
    }
}
