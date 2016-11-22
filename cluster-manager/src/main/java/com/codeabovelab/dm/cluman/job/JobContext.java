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

package com.codeabovelab.dm.cluman.job;

import com.codeabovelab.dm.common.utils.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;

import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Context of job. Note tah context is hold in {@link ThreadLocal} but can be used simultaneously from different threads.
 */
public final class JobContext /* we cannot use AutoCloseable on this bean, so it cause recursion */{

    private static final ThreadLocal<JobContext> TL = new ThreadLocal<>();
    private static final AtomicLong COUNTER = new AtomicLong(0);
    /**
     * attr hold between iterations and cannot contains beans
     */
    private final ConcurrentMap<String, Object> attrs = new ConcurrentHashMap<>();
    private final Map<String, Object> beans = new HashMap<>();
    private final ConcurrentMap<String, Set<Runnable>> callbacks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> result = new ConcurrentHashMap<>();
    private final String id;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final JobParameters parameters;
    private final AbstractJobInstance instance;

    JobContext(AbstractJobInstance instance, JobParameters parameters) {
        this.instance = instance;
        this.parameters = parameters;
        this.attrs.putAll(this.parameters.getParameters());
        this.id = this.getParameters().getType() + ".context#" + COUNTER.getAndIncrement();
    }

    /**
     * Send message into job log which formatted as {@link MessageFormat#format(String, Object...)}
     * <p/> Use {@link JobInstance#send(String, Object...) } which replace this.
     * @param message message with
     * @param args
     */
    public void fire(String message, Object ... args) {
        this.instance.send(message, args);
    }

    public static JobContext getCurrent() {
        return TL.get();
    }

    Map<String, Object> getBeans() {
        return beans;
    }

    void registerDestructionCallback(String name, Runnable callback) {
        callbacks.computeIfAbsent(name, (n) -> Collections.synchronizedSet(new HashSet<>())).add(callback);
    }

    public String getId() {
        return id;
    }

    public static JobContext set(JobContext context) {
        TL.set(context);
        return context;
    }

    /**
     * Remove context from thread local. <p/>
     * Note that this method does not close context.
     */
    public static void remove() {
        TL.remove();
    }

    void close() {
        try {
            for(Map.Entry<String, Set<Runnable>> entry: callbacks.entrySet()) {
                String key = entry.getKey();
                for(Runnable runnable: entry.getValue()) {
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

    public JobParameters getParameters() {
        return parameters;
    }

    /**
     * Reference to internal result map.
     * @return
     */
    public ConcurrentMap<String, Object> getResult() {
        return result;
    }

    /**
     * Return result of job. Also {@link JobParam out parameters} for job beans.
     * @param name
     * @return
     */
    public Object getResult(String name) {
        return result.get(name);
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

    public Map<String, Object> getAttributes() {
        return attrs;
    }
}
