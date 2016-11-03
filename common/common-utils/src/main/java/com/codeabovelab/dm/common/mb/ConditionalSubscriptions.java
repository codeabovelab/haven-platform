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
public interface ConditionalSubscriptions<M, K> extends Subscriptions<M> {

    /**
     * Subscribe on specified keys
     * @param listener
     * @param key event key, what part of event is key depend from key extractor function
     */
    void subscribeOnKey(Consumer<M> listener, K key);

    /**
     * Act like {@link #subscribe(Consumer)}, but return closeable which is doing unsubcription.
     * @param listener
     * @param key event key, what part of event is key depend from key extractor function
     * @return
     */
    default Subscription openSubscriptionOnKey(Consumer<M> listener, K key) {
        subscribe(listener);
        return new SubscriptionImpl<>(this, listener);
    }
}
