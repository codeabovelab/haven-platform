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
import java.util.function.Function;

/**
 * Message bus.
 */
public interface MessageBus<M> extends Consumer<M>, Subscriptions<M>, AutoCloseable {
    /**
     * Provide part of api which allow only subscription. Can return {@link ConditionalSubscriptions}.
     * @see Subscriptions
     * @see ConditionalSubscriptions
     * @return
     */
    Subscriptions<M> asSubscriptions();

    /**
     * Mean that does not have any subscriber.
     * @return
     */
    boolean isEmpty();
}
