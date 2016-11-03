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
 * Simple wrapper which is hide message bus method like {@link MessageBus#accept(Object)} from interface users.
 */
public class MessageSubscriptionsWrapper<M> implements Subscriptions<M> {
    private final Subscriptions<M> orig;

    public MessageSubscriptionsWrapper(Subscriptions<M> orig) {
        this.orig = orig;
    }

    @Override
    public void subscribe(Consumer<M> listener) {
        orig.subscribe(listener);
    }

    @Override
    public Subscription openSubscription(Consumer<M> listener) {
        return orig.openSubscription(listener);
    }

    @Override
    public void unsubscribe(Consumer<M> listener) {
        orig.unsubscribe(listener);
    }

    @Override
    public String getId() {
        return orig.getId();
    }

    @Override
    public Class<M> getType() {
        return orig.getType();
    }

    @Override
    public <T> T getExtension(Key<T> key) {
        return orig.getExtension(key);
    }

    @Override
    public <T> T getOrCreateExtension(Key<T> key, ExtensionFactory<T, M> factory) {
        return orig.getOrCreateExtension(key, factory);
    }
}
