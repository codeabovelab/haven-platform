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
class SubscriptionImpl<M> implements Subscription {
    private final SubscriptionsIface<M> subscriptions;
    private final Consumer<M> consumer;

    SubscriptionImpl(SubscriptionsIface<M> subscriptions, Consumer<M> consumer) {
        this.subscriptions = subscriptions;
        this.consumer = consumer;
    }

    @Override
    public Consumer<?> getConsumer() {
        return this.consumer;
    }

    @Override
    public void close() {
        this.subscriptions.unsubscribe(this.consumer);
    }
}
