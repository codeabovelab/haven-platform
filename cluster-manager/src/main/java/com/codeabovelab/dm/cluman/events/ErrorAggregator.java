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

package com.codeabovelab.dm.cluman.events;

import com.codeabovelab.dm.cluman.model.Severity;
import com.codeabovelab.dm.cluman.model.WithSeverity;
import com.codeabovelab.dm.cluman.persistent.PersistentBusFactory;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.codeabovelab.dm.common.mb.Subscription;
import com.codeabovelab.dm.common.mb.Subscriptions;
import com.codeabovelab.dm.common.utils.Closeables;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 */
@Component
public class ErrorAggregator implements AutoCloseable, InitializingBean {
    // we must store events for last 24 hours, but our queue does not support limit by time
    private static final int MAX_SIZE = 1024 * 10;
    private final MessageBus<WithSeverity> bus;
    private final List<AutoCloseable> closeables = new ArrayList<>();
    private final ListableBeanFactory beanFactory;

    public ErrorAggregator(PersistentBusFactory pbf, ListableBeanFactory beanFactory) {
        this.bus = pbf.create(WithSeverity.class, EventsUtils.BUS_ERRORS, MAX_SIZE);
        this.beanFactory = beanFactory;
    }

    public Subscriptions<WithSeverity> getSubscriptions() {
        return bus.asSubscriptions();
    }

    @Override
    public void close() throws Exception {
        this.closeables.forEach(Closeables::close);
        this.closeables.clear();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String[] beanNames = beanFactory.getBeanNamesForType(Subscriptions.class, false, false);
        for(String beanName: beanNames) {
            if(EventsUtils.BUS_ERRORS.equals(beanName)) {
                continue;
            }
            Subscriptions<?> bean = beanFactory.getBean(beanName, Subscriptions.class);
            Subscription subs = bean.openSubscription(this::onEvent);
            this.closeables.add(subs);
        }
    }

    private void onEvent(Object o) {
        if(!(o instanceof WithSeverity)) {
            return;
        }
        WithSeverity ws = (WithSeverity) o;
        Severity severity = ws.getSeverity();
        if(severity != Severity.ERROR && severity != Severity.WARNING) {
            return;
        }
        this.bus.accept(ws);
    }
}
