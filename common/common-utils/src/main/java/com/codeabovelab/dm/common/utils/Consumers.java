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

import java.util.function.Consumer;

/**
 * Some utilities for {@link java.util.function.Consumer }
 */
public class Consumers {

    /**
     * Holder for consumed value. It threadsafe.
     * @param <T>
     * @return
     */
    public static <T> HolderConsumer<T> holder() {
        return new HolderConsumer<>();
    }

    /**
     * Holder for consumed value. It threadsafe.
     * @param <T>
     */
    public static class HolderConsumer<T> implements Consumer<T> {
        private volatile T value;


        @Override
        public void accept(T t) {
            this.value = t;
        }

        /**
         * Last accepted value.
         * @return
         */
        public T getValue() {
            return value;
        }
    }
    
    public static final Consumer<Object> NO_OP = o -> {
        //nothing
    };

    /**
     * Consumer which do nothing 'No OPerations'
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> Consumer<T> nop() {
        return (Consumer<T>) NO_OP;
    }
}
