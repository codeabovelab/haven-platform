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

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 */
public final class MessageBuses {

    private MessageBuses() {
    }

    /**
     * Create new instance of message bus with default exception handler.
     * @param id
     * @param type
     * @param <M>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <M> MessageBus<M> create(String id, Class<M> type) {
        return MessageBusImpl.<M, Subscriptions<M>>builder(type, MessageSubscriptionsWrapper::new).id(id).build();
    }

    /**
     * Create new instance of message bus with specified exception handler.
     * @param id
     * @param type
     * @param eh
     * @param <M>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <M> MessageBus<M> create(String id, Class<M> type, Consumer<ExceptionInfo> eh) {
        return MessageBusImpl.<M, Subscriptions<M>>builder(type, MessageSubscriptionsWrapper::new)
          .id(id)
          .exceptionInfoConsumer(eh)
          .build();
    }

    /**
     * Create bust which can return {@link ConditionalSubscriptions} from {@link MessageBus#asSubscriptions()}
     * @param id name of bus
     * @param type type of message
     * @param keyExtractor function which is produce key from message, usual it return some message attribute, or simple
     *                     message, creation of new objects may reduce performance
     * @param predicate predicate which is accept pattern (given from {@link ConditionalSubscriptions#subscribeOnKey(Consumer, Object)} ) and
     *                  key from keyExtractor and return true if message must be transferred to subscriber. <b>Note that all parameters can be null.</b>
     * @param <M> type of message
     * @param <K> type of key
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <M, K> MessageBus<M> createConditional(String id, Class<M> type, Function<M, K> keyExtractor, BiPredicate<K, K> predicate) {
        return MessageBusImpl.<M, ConditionalSubscriptions<M, K>>builder(type,
          (s) -> new ConditionalMessageBusWrapper(s, keyExtractor, predicate)
        ).id(id).build();
    }
}
