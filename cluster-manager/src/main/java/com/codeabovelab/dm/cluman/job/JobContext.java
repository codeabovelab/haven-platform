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

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Context of job. Note that context is hold in {@link ThreadLocal} but can be used simultaneously from different threads.
 */
public final class JobContext /* we cannot use AutoCloseable on this bean, so it cause recursion */{

    private static final ThreadLocal<JobContext> TL = new ThreadLocal<>();
    private static final AtomicLong COUNTER = new AtomicLong(0);
    /**
     * attr hold between iterations and cannot contains beans
     */
    private final ConcurrentMap<String, Object> attrs = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> result = new ConcurrentHashMap<>();
    private final String id;
    private final JobParameters parameters;
    private final AbstractJobInstance instance;
    private volatile RollbackHandle rollbackHandle;
    private final ScopeBeans scopeBeans;
    private volatile ScopeBeans scopeIterationBeans;
    private final AtomicInteger iteration = new AtomicInteger(0);

    JobContext(AbstractJobInstance instance, JobParameters parameters) {
        this.instance = instance;
        this.parameters = parameters;
        this.attrs.putAll(this.parameters.getParameters());
        this.id = this.getParameters().getType() + ".context#" + COUNTER.getAndIncrement();
        this.scopeBeans = new ScopeBeans(this, this.id);
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

    public Map<String, Object> getAttributes() {
        return attrs;
    }

    /**
     * Set rollback handle. When job support schedule on single context (see {@link JobBean#repeatable()} )
     * it MUST be reset to null before job start by {@link JobInstance } implementation.
     * @param rollbackHandle any value include null is accepted.
     */
    public void setRollback(RollbackHandle rollbackHandle) {
        // Yes we can user results, but its a transfer data to job user, and conceptually must be set only at job success
        // but rollback also need when job is fail, and must part of job api
        this.rollbackHandle = rollbackHandle;
    }

    /**
     * Gives job rollback handle. Return null when job does not support rollback.
     * @return handle or null
     */
    public RollbackHandle getRollback() {
        return rollbackHandle;
    }

    public int getIteration() {
        return iteration.get();
    }

    int nextIteration() {
        return iteration.incrementAndGet();
    }

    ScopeBeans getScopeBeans() {
        return this.scopeBeans;
    }

    ScopeBeans getScopeIterationBeans() {
        return this.scopeIterationBeans;
    }

    void setScopeIterationBeans(ScopeBeans scopeBeans) {
        this.scopeIterationBeans = scopeBeans;
    }
}
