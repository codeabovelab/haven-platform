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

package com.codeabovelab.dm.cluman.persistent;

import com.codeabovelab.dm.common.fc.FbJacksonAdapter;
import com.codeabovelab.dm.common.fc.FbQueue;
import com.codeabovelab.dm.common.fc.FbStorage;
import com.codeabovelab.dm.common.mb.*;
import com.codeabovelab.dm.common.utils.Closeables;
import com.codeabovelab.dm.common.utils.Key;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 */
@Slf4j
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class PersistentBusFactory implements InitializingBean, DisposableBean {
    /**
     * Key of bus extension
     * @see MessageBus#getExtension(Key)
     */
    @SuppressWarnings("unchecked")
    public static final Key<PersistentBus<?>> EXT_KEY = new Key<>((Class)PersistentBus.class);

    public class PersistentBus<T> implements AutoCloseable {

        private final Consumer<T> queueListener;
        private boolean closed;
        private final FbQueue<T> queue;
        private final MessageBusImpl<T, MessageSubscriptionsWrapper<T>> bus;

        public PersistentBus(Class<T> type, String id, int size) {
            this.queue = FbQueue.builder(new FbJacksonAdapter<>(objectMapper, type))
              .id(id)
              .storage(fbStorage)
              .maxSize(size)
              .build();
            this.queueListener = queue::push;
            this.bus = MessageBusImpl
              .builder(type, MessageSubscriptionsWrapper::new)
              .id(id)
              .onSubscribe(this::flusher)
              .build();
            this.bus.getOrCreateExtension(EXT_KEY, (k, b) -> this);
            this.bus.subscribe(queueListener);
        }

        private void flusher(MessageBus<T> mb, Consumer<T> l) {
            if(WrappedConsumer.unwrap(l) == queueListener) {
                return;
            }
            SmartConsumer<T> sc = SmartConsumer.of(l);
            int historyCount = sc.getHistoryCount();
            if(historyCount == 0) {
                return;
            }
            Predicate<T> filter = sc.historyFilter();
            try {
                Iterator<T> iter = queue.iterator(historyCount);
                while (iter.hasNext()) {
                    T next = iter.next();
                    if(filter.test(next)) {
                        l.accept(next);
                    }
                }
            } catch (Exception e) {
                log.error("Can not up history to new consumer, due to error.", e);
            }
        }

        @Override
        public void close() throws Exception {
            if(closed) {
                // prevent SOE, because this method may be invoked from bus
                return;
            }
            closed = true;
            this.bus.close();
            this.queue.close();
        }

        public MessageBus<T> getBus() {
            return bus;
        }

        public FbQueue<T> getQueue() {
            return queue;
        }
    }

    private final ObjectMapper objectMapper;
    private final FbStorage fbStorage;
    private final ConcurrentMap<String, PersistentBus<?>> map = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> MessageBus<T> create(Class<T> type, String id, int size) {
        PersistentBus<T> entry = (PersistentBus<T>) map.computeIfAbsent(id, (i) -> new PersistentBus<>(type, id, size));
        return entry.getBus();
    }

    public PersistentBus<?> get(String id) {
        return map.get(id);
    }

    @Override
    public void destroy() throws Exception {
        map.values().forEach(Closeables::close);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
