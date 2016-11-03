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

package com.codeabovelab.dm.common.mb;

import java.util.function.Consumer;

/**
 */
public interface WrappedConsumer<T> extends Consumer<T>, AutoCloseable {
    /**
     * Return internal object, which is wrapped by this.
     * @return
     */
    Consumer<T> unwrap();

    /**
     * Used when bus contains listener wrappers, note that must correct work if argument is not an wrapper.
     * @param src
     * @param <M>
     * @return
     */
    static <M> Consumer<M> unwrap(Consumer<M> src) {
        Consumer<M> tmp = src;
        while(tmp instanceof WrappedConsumer) {
            tmp = ((WrappedConsumer<M>) tmp).unwrap();
            if(tmp == null) {
                throw new IllegalArgumentException("Null consumer in " + src);
            }
        }
        return tmp;
    }
}
