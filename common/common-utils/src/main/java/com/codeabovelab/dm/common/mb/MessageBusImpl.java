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

import com.codeabovelab.dm.common.utils.Closeables;
import com.codeabovelab.dm.common.utils.Key;
import lombok.Data;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 */
public final class MessageBusImpl<M, S extends Subscriptions<M>> implements MessageBus<M> {

    @Data
    public static class Builder<M, S extends Subscriptions<M>> {
        protected Consumer<ExceptionInfo> exceptionInfoConsumer = ExceptionInfoLogger.getInstance();
        protected String id;
        protected final Class<M> type;
        protected final Function<Subscriptions<M>, S> subscriptionsFactory;
        protected SubscribeListener<M> onUnsubscribe;
        protected SubscribeListener<M> onSubscribe;

        Builder(Class<M> type, Function<Subscriptions<M>, S> subscriptionsFactory) {
            this.type = type;
            this.subscriptionsFactory = subscriptionsFactory;
        }

        public Builder<M, S> exceptionInfoConsumer(Consumer<ExceptionInfo> exceptionInfoConsumer) {
            setExceptionInfoConsumer(exceptionInfoConsumer);
            return this;
        }

        public Builder<M, S> id(String id) {
            setId(id);
            return this;
        }

        public Builder<M, S> onUnsubscribe(SubscribeListener<M> onUnsubscribe) {
            setOnUnsubscribe(onUnsubscribe);
            return this;
        }

        public Builder<M, S> onSubscribe(SubscribeListener<M> onSubscribe) {
            setOnSubscribe(onSubscribe);
            return this;
        }

        public MessageBusImpl<M, S> build() {
            return new MessageBusImpl<>(this);
        }
    }

    private final AtomicReference<List<Consumer<M>>> listenersRef = new AtomicReference<>(Collections.emptyList());
    private final Consumer<ExceptionInfo> exceptionInfoConsumer;
    private final String id;
    private final Class<M> type;
    private final S subscriptions;
    private final SubscribeListener<M> onUnsubscribe;
    private final SubscribeListener<M> onSubscribe;
    private final ConcurrentMap<Key<?>, Object> extensions = new ConcurrentHashMap<>();

    private MessageBusImpl(Builder<M, S> b) {
        Assert.hasText(b.id, "id is null or empty");
        this.id = b.id;
        Assert.notNull(b.type, "type is null");
        this.type = b.type;
        Assert.notNull(b.exceptionInfoConsumer, "exceptionInfoConsumer is null");
        this.exceptionInfoConsumer = b.exceptionInfoConsumer;
        Assert.notNull(b.subscriptionsFactory, "subscriptionsFactory is null");
        this.subscriptions = b.subscriptionsFactory.apply(this);
        this.onUnsubscribe = b.onUnsubscribe;
        this.onSubscribe = b.onSubscribe;
    }

    @SuppressWarnings("unchecked")
    public static <M, S extends Subscriptions<M>> Builder<M, S>
                builder(Class<M> messageType, Function<Subscriptions<M>, S> subscriptionsFactory) {
        return new Builder(messageType, subscriptionsFactory);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Class<M> getType() {
        return type;
    }

    @Override
    public void accept(M message) {
        //first we must check message for correct type
        type.cast(message);

        final List<Consumer<M>> list = listenersRef.get();
        int size = list.size();
        for(int i = 0; i < size; ++i) {
            Consumer<M> consumer = list.get(i);
            invoke(consumer, message);
        }
    }

    private void invoke(Consumer<M> consumer, M message) {
        try {
            consumer.accept(message);
        } catch (Throwable e) {
            ExceptionInfo ei = new ExceptionInfo(this, e, consumer, message);
            exceptionInfoConsumer.accept(ei);
        }
    }

    @Override
    public void subscribe(Consumer<M> listener) {
        Assert.notNull(listener, "listener is null");
        while(true) {
            final List<Consumer<M>> srcList = listenersRef.get();
            if(contains(srcList, listener)) {
                return;
            }
            List<Consumer<M>> tmp = new ArrayList<>(srcList.size() + 1);
            tmp.addAll(srcList);
            tmp.add(listener);
            List<Consumer<M>> dstList = Collections.unmodifiableList(tmp);
            if(listenersRef.compareAndSet(srcList, dstList)) {
                if(onSubscribe != null) {
                    onSubscribe.event(this, listener);
                }
                return;
            }
        }
    }

    private boolean contains(List<Consumer<M>> list, Consumer<M> key) {
        return indexOf(list, key) >= 0;
    }

    private int indexOf(List<Consumer<M>> list, Consumer<M> key) {
        Consumer<?> unwrappedKey = unwrap(key);
        int size = list.size();
        for(int i = 0; i < size; i++) {
            Consumer<?> element = unwrap(list.get(i));
            if(element == unwrappedKey) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Used when bus contains listener wrappers, note that must correct work if argument is not an wrapper.
     * @param src
     * @return
     */
    protected Consumer<M> unwrap(Consumer<M> src) {
        return WrappedConsumer.unwrap(src);
    }

    @Override
    public void unsubscribe(Consumer<M> listener) {
        Assert.notNull(listener, "listener is null");
        while(true) {
            final List<Consumer<M>> srcList = listenersRef.get();
            int i = indexOf(srcList, listener);
            if(i < 0) {
                return;
            }
            List<Consumer<M>> tmp = new ArrayList<>(srcList);
            tmp.remove(i);
            List<Consumer<M>> dstList = Collections.unmodifiableList(tmp);
            if(listenersRef.compareAndSet(srcList, dstList)) {
                if(onUnsubscribe != null) {
                    onUnsubscribe.event(this, listener);
                }
                return;
            }
        }
    }

    @Override
    public boolean isEmpty() {
        List<Consumer<M>> consumers = listenersRef.get();
        return consumers.isEmpty();
    }

    @Override
    public S asSubscriptions() {
        return subscriptions;
    }

    @Override
    public <T> T getOrCreateExtension(Key<T> key, ExtensionFactory<T, M> factory) {
        Assert.notNull(key, "key is null");
        Object old = this.extensions.computeIfAbsent(key, (k) -> factory.create(key, this));
        return key.cast(old);
    }

    @Override
    public <T> T getExtension(Key<T> key) {
        Assert.notNull(key, "key is null");
        return Key.get(this.extensions, key);
    }

    /**
     * Clean all listeners, and cloe if they implement {@link AutoCloseable }
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        List<Consumer<M>> old = listenersRef.getAndSet(Collections.emptyList());
        old.stream().forEach(Closeables::closeIfCloseable);
        this.extensions.values().forEach(Closeables::closeIfCloseable);
        this.extensions.clear();
    }
}
