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

/**
 * Utilities for callbacks
 */
public final class Callbacks {

    /**
     * Callback which do 'No OPerations'. It usable for cases when null callback is not acceptable.
     */
    private static final Callback<Object> NOP_CALLBACK = arg -> { };

    /**
     * Invoke callback if it is not a null.
     * @param callback
     * @param value
     */
    public static <T> void call(Callback<T> callback, T value) {
        if(callback == null) {
            return;
        }
        callback.call(value);
    }

    /**
     * Callback which do 'No OPerations'. It usable for cases when null callback is not acceptable.
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> Callback<T> nop() {
        return (Callback<T>) NOP_CALLBACK;
    }
}
