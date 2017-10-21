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

package com.codeabovelab.dm.cluman.cluster.docker.management;

import com.codeabovelab.dm.cluman.model.ContainerBase;
import com.codeabovelab.dm.cluman.model.DockerLogEvent;
import com.codeabovelab.dm.common.cache.CacheInvalidator;
import com.codeabovelab.dm.common.mb.MessageBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 */
@Component
class DockerCacheInvalidator implements CacheInvalidator {

    private final Map<String, Consumer<DockerLogEvent>> listeners = new ConcurrentHashMap<>();

    @Autowired
    @Qualifier(DockerLogEvent.BUS)
    private MessageBus<DockerLogEvent> logEvents;

    @SuppressWarnings("unchecked")
    @Override
    public void init(Cache cache, Map<String, String> args) {
        String name = cache.getName();
        if(!DockerService.CACHE_CONTAINER_DETAILS.equals(name)) {
            return;
        }
        Consumer<DockerLogEvent> listener = this.listeners.computeIfAbsent(name, (n) -> this.makeInvalidator(cache));
        logEvents.subscribe(listener);
    }

    private Consumer<DockerLogEvent> makeInvalidator(Cache cache) {
        return (e) -> {
            ContainerBase container = e.getContainer();
            String name = container.getName();
            String id = container.getId();
            if(name != null && id != null) {
                cache.evict(name);
                cache.evict(id);
                // docker also can return containers by its 12 symbols id
                cache.evict(id.substring(0, 12));
            }
        };
    }

    @PreDestroy
    void preDestroy() {
        listeners.values().forEach((l) -> this.logEvents.asSubscriptions().unsubscribe(l));
    }
}
