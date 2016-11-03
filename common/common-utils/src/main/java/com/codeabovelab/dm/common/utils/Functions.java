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

import java.util.function.Function;

/**
 */
public class Functions {
    private static final Function<Object, Object> DIRECT = arg -> arg;

    private static final Function<Object, Object> NULL = arg -> null;

    /**
     * Function which return its argument.
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> Function<T, T> directFunc() {
        return (Function<T, T>) DIRECT;
    }

    /**
     * Function which return null.
     * @param <A>
     * @param <V>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <A, V> Function<A, V> nullFunc() {
        return (Function<A, V>) NULL;
    }

    /**
     * This is some hack which allow us to process any types of arguments with single generic function. Also it
     * frees us from check interceptor for null.
     * @param interceptor
     * @param arg
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T intercept(Function<Object, Object> interceptor, T arg) {
        if(interceptor == null) {
            return arg;
        }
        Object res = interceptor.apply(arg);
        return (T) res;
    }
}
