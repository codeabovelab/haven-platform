/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.cluman.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Beans for scope
 */
@Slf4j
class ScopeBeans {
    private final Map<String, Object> beans = new HashMap<>();
    private final ConcurrentMap<String, Set<Runnable>> callbacks = new ConcurrentHashMap<>();
    final JobContext context;
    final String id;

    protected ScopeBeans(JobContext context, String id) {
        this.context = context;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    void registerDestructionCallback(String name, Runnable callback) {
        callbacks.computeIfAbsent(name, (n) -> Collections.synchronizedSet(new HashSet<>())).add(callback);
    }

    void close() {
        try {
            for (Map.Entry<String, Set<Runnable>> entry : callbacks.entrySet()) {
                String key = entry.getKey();
                for (Runnable runnable : entry.getValue()) {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        log.error("On bean {} callback: {}", key, runnable, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("On close ", e);
        } finally {
            callbacks.clear();
        }
        //we clear beans but not attributes, because it can be need on next iteration
        synchronized (beans) {
            beans.clear();
        }
    }

    Object removeBean(String name) {
        synchronized (this.beans) {
            return this.beans.remove(name);
        }
    }


    Object getBean(String name, ObjectFactory<?> objectFactory) {
        // we cannot use computeIfAbsent because it does not support recursion
        Object bean;
        synchronized (this.beans) {
            bean = this.beans.computeIfAbsent(name, k -> objectFactory.getObject());
        }
        return bean;
    }

}
