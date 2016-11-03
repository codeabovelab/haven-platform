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

import com.codeabovelab.dm.common.utils.Key;

import java.util.function.Consumer;

/**
 * Part of bus which allow subscription.
 */
public interface Subscriptions<M> extends SubscriptionsIface<M>, MessageBusInfo<M>  {

    /**
     * Make empty subscription which do nothing.
     * @param id
     * @param type
     * @param <T>
     * @return
     */
    static <T> Subscriptions<T> empty(String id, Class<T> type) {
        return new Subscriptions<T>() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public Class<T> getType() {
                return type;
            }

            @Override
            public void subscribe(Consumer<T> listener) {

            }

            @Override
            public void unsubscribe(Consumer<T> listener) {

            }

            @Override
            public <E> E getOrCreateExtension(Key<E> key, ExtensionFactory<E, T> factory) {
                return null;
            }

            @Override
            public <E> E getExtension(Key<E> key) {
                return null;
            }
        };
    }

    /**
     * Register specified extension in this bus. <p/>
     * Note that if extension is instance of {@link AutoCloseable } then it closed with bus.
     * @see #getExtension(Key)
     * @param key
     * @param factory
     * @param <T>
     * @return
     */
    <T> T getOrCreateExtension(Key<T> key, ExtensionFactory<T, M> factory);

    /**
     * Get specified extension from this bus.
     * @see #getOrCreateExtension(Key, ExtensionFactory)
     * @param key
     * @param <T>
     * @return
     */
    <T> T getExtension(Key<T> key);
}
