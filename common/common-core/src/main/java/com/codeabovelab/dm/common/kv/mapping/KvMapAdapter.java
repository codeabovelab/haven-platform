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

package com.codeabovelab.dm.common.kv.mapping;

/**
 * Adapter which allow to obtain object for save from source, and verse versa.
 */
public interface KvMapAdapter<T> {

    /**
     * Default adapter implementation.
     */
    KvMapAdapter<Object> DIRECT = new KvMapAdapter<Object>() {
        @Override
        public Object get(Object source) {
            return source;
        }

        @Override
        public Object set(Object source, Object value) {
            return value;
        }
    };

    @SuppressWarnings("unchecked")
    static <T> KvMapAdapter<T> direct() {
        return (KvMapAdapter<T>) DIRECT;
    }

    /**
     * Retrieve object from source for saving.
     * @param source source object
     * @return value null is not allowed
     */
    Object get(T source);

    /**
     * Set loaded object to source object.
     * @param source old source object, may be null
     * @param value loaded value, null not allowed.
     * @return new source object, also you can return 'source'
     */
    T set(T source, Object value);

}
