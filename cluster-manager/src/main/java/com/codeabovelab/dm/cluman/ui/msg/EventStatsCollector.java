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

package com.codeabovelab.dm.cluman.ui.msg;

import com.codeabovelab.dm.common.mb.MessageBus;
import com.codeabovelab.dm.common.mb.MessageBusImpl;
import com.codeabovelab.dm.common.mb.MessageSubscriptionsWrapper;
import com.codeabovelab.dm.common.mb.Subscriptions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 */
public class EventStatsCollector<E> implements Consumer<E>, AutoCloseable {

    private class Bag {
        private final Object lock = new Object();
        private final Object key;
        private int count = 0;
        private E last;

        Bag(Object key) {
            this.key = key;
        }

        void accept(E e) {
            EventStats<E> stats;
            synchronized (lock) {
                count++;
                last = e;
                stats = makeEvent();
            }
            bus.accept(stats);
        }

        private EventStats<E> makeEvent() {
            synchronized (lock) {
                return new EventStats<>(key, last, count);
            }
        }
    }

    private final ConcurrentMap<Object, Bag> bags = new ConcurrentHashMap<>();
    private final MessageBus<EventStats<E>> bus;
    private final Function<E, Object> keyFactory;

    @SuppressWarnings("unchecked")
    public EventStatsCollector(String busId, Function<E, Object> keyFactory) {
        this.keyFactory = keyFactory;
        Class<EventStats<E>> type = (Class) EventStats.class;
        this.bus = MessageBusImpl.builder(type, MessageSubscriptionsWrapper::new)
          .id(busId)
          .onSubscribe(this::onSubscribe)
          .build();
    }

    private void onSubscribe(MessageBus<EventStats<E>> messageBus, Consumer<EventStats<E>> consumer) {
        bags.forEach((k, b) -> {
            consumer.accept(b.makeEvent());
        });
    }

    @Override
    public void accept(E e) {
        Object key = keyFactory.apply(e);
        if(key == null) {
            return;
        }
        Bag bag = bags.computeIfAbsent(key, Bag::new);
        bag.accept(e);
    }

    public Subscriptions<EventStats<E>> getSubscriptions() {
        return bus.asSubscriptions();
    }

    public String getBusId() {
        return this.bus.getId();
    }


    @Override
    public void close() throws Exception {
        this.bus.close();
    }
}
