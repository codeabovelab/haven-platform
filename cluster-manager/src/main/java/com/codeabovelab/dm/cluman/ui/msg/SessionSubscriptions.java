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

import com.codeabovelab.dm.cluman.model.EventWithTime;
import com.codeabovelab.dm.common.mb.SmartConsumer;
import com.codeabovelab.dm.common.mb.Subscriptions;
import com.codeabovelab.dm.common.utils.Closeables;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * Bean which hold subscriptions of session
 */
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
@Scope(value = "websocket", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SessionSubscriptions implements AutoCloseable {

    private final ConcurrentMap<String, AutoCloseable> subs = new ConcurrentHashMap<>();

    private final Stomp stomp;

    public Collection<String> getIds() {
        ArrayList<String> list = new ArrayList<>(subs.keySet());
        list.sort(null);
        return Collections.unmodifiableList(list);
    }

    @Override
    public void close() throws Exception {
        subs.values().forEach(Closeables::close);
    }

    /**
     * Subscribe current session to specified {@link Subscriptions}
     * @param uas it not ever same as {@link Subscriptions#getId()}
     * @param subscriptions
     */
    public <T> void subscribe(UiAddSubscription uas, Subscriptions<T> subscriptions) {
        subs.computeIfAbsent(uas.getSource(), (i) -> subscriptions.openSubscription(new ConsumerImpl<T>(uas)));
        fire();
    }

    /**
     * remove and close specified subscription
     * @param id
     */
    public void unsubscribe(String id) {
        AutoCloseable subs = this.subs.remove(id);
        Closeables.close(subs);
        fire();
    }

    private void fire() {
        //send into our session info about changes
        stomp.sendToSession(EventController.SUBSCRIPTIONS_GET, getIds());
    }

    private class ConsumerImpl<T> implements SmartConsumer<T>, AutoCloseable {
        private final String id;
        private final AtomicBoolean closed = new AtomicBoolean();
        private final long historySince;
        private final int historyCount;

        ConsumerImpl(UiAddSubscription uas) {
            this.id = uas.getSource();
            this.historyCount = uas.getHistoryCount();
            Date historySince = uas.getHistorySince();
            this.historySince = historySince == null? Long.MIN_VALUE : historySince.getTime();
        }

        @Override
        public void accept(T e) {
            stomp.sendToSession(id, e);
        }

        @Override
        public void close() throws Exception {
            //this flag prevent recursion
            if(!closed.compareAndSet(false, true)) {
                return;
            }
            unsubscribe(id);
        }

        @Override
        public Predicate<T> historyFilter() {
            return (event) -> {
                if(!(event instanceof EventWithTime)) {
                    return true;
                }
                return ((EventWithTime)event).getTimeInMilliseconds() >= historySince;
            };
        }

        @Override
        public int getHistoryCount() {
            return historyCount;
        }
    }
}
