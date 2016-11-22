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

import com.codeabovelab.dm.cluman.security.TempAuth;
import com.codeabovelab.dm.common.utils.SafeCloseable;
import com.google.common.base.MoreObjects;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.SettableFuture;
import lombok.Data;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 */
public abstract class AbstractJobInstance implements JobInstance {

    @Data
    public static class Config {
        protected JobsManagerImpl jobsManager;
        protected JobParameters parameters;
        protected JobInfo info;
        protected JobWatcher watcher;
        protected Runnable job;
        protected Authentication authentication;
        /**
         * Flag from {@link JobBean#repeatable()}, when true then context between iterations will not cleared.
         */
        protected boolean repeatable;

        public Config jobsManager(JobsManagerImpl jobsManager) {
            setJobsManager(jobsManager);
            return this;
        }

        public Config parameters(JobParameters parameters) {
            setParameters(parameters);
            return this;
        }

        public Config info(JobInfo info) {
            setInfo(info);
            return this;
        }

        public Config job(Runnable job) {
            setJob(job);
            return this;
        }

        public Config authentication(Authentication authentication) {
            setAuthentication(authentication);
            return this;
        }

        /**
         * Flag from {@link JobBean#repeatable()}, when true then context between iterations will not cleared.
         * @param repeatable default false
         * @return
         */
        public Config repeatable(boolean repeatable) {
            setRepeatable(repeatable);
            return this;
        }
    }

    /**
     * We cannot save all events because it cause memory leak for scheduled jobs.
     */
    private static final int MAX_EVENTS = 1024;
    private static final Logger LOG = LoggerFactory.getLogger(JobInstanceImpl.class);
    protected final AtomicReference<JobInfo> infoRef;
    protected final ListenableFutureTask<Boolean> cancelFuture;
    protected final ListenableFutureTask<Boolean> startFuture;
    protected final JobsManagerImpl manager;
    protected final Runnable job;
    protected final SettableFuture<JobInstance> atEndFuture;
    protected final JobContext jobContext;
    protected final AtomicReference<JobStatus> statusRef = new AtomicReference<>(JobStatus.CREATED);
    protected volatile Future<?> executeHandle;
    private final Authentication authentication;
    private final JobWatcher watcher;
    private final List<JobEvent> events = new CopyOnWriteArrayList<>();

    public AbstractJobInstance(Config config) {
        Assert.notNull(config.parameters, "parameters is null");
        Assert.notNull(config.job, "job is null");
        Assert.notNull(config.jobsManager, "jobsManager is null");
        Assert.notNull(config.info, "info is null");
        this.jobContext = new JobContext(this, config.parameters);
        this.infoRef = new AtomicReference<>(config.info);
        this.manager = config.jobsManager;
        // create wait future with stub
        this.atEndFuture = SettableFuture.create();
        this.job = config.job;
        this.authentication = config.authentication;
        this.watcher = config.watcher;
        this.cancelFuture = ListenableFutureTask.create(this::innerCancel);
        this.startFuture = ListenableFutureTask.create(this::innerStart);
    }

    /**
     * Test that arg is not null and call {@link Future#cancel(boolean)} with true param.
     * @param future
     */
    protected static void cancel(Future<?> future) {
        if(future != null) {
            future.cancel(true);
        }
    }

    /**
     * Send message into job log which formatted as {@link MessageFormat#format(String, Object...)}
     * @param message message with
     * @param args objects, also first find throwable will be extracted and passed into event as {@link JobEvent#getException()}
     */
    @Override
    public void send(String message, Object ... args) {
        Throwable throwable = null;
        if(!ArrayUtils.isEmpty(args)) {
            //find and extract throwable
            for(int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if(arg instanceof Throwable) {
                    throwable = (Throwable) arg;
                    args = ArrayUtils.remove(args, i);
                    break;
                }
            }
            if(message != null) {
                try {
                    message = MessageFormat.format(message, args);
                } catch (Exception e) {
                    LOG.error("Cannot format message: \"{}\"", message, e);
                }
            }
        }
        sendEvent(new JobEvent(getInfo(), message, throwable));
    }

    private void sendEvent(JobEvent event) {
        this.events.add(event);
        this.manager.getBus().accept(event);
        while(this.events.size() > MAX_EVENTS) {
            this.events.remove(0);
        }
        // we use watcher instead of subscription on bus, because it binds with concrete instance
        //  and also receive instance reference (event does not have reference to instance)
        if(watcher != null) {
            watcher.onEvent(this, event);
        }
    }

    @Override
    public JobContext getJobContext() {
        return jobContext;
    }

    protected abstract boolean innerCancel() throws Exception;

    protected abstract boolean innerStart() throws Exception;

    protected abstract JobStatus completedStatus();
    protected abstract JobStatus failedStatus();

    protected void setStatus(JobStatus status) {
        this.statusRef.set(status);
        statusChanged(null, status, null).close();
    }

    protected <T> T compareAndSetStatus(JobStatus expected, JobStatus status, Callable<T> ifOk) throws Exception {
        boolean result = this.statusRef.compareAndSet(expected, status);
        T res = null;
        if(result) {
            SafeCloseable closeable = statusChanged(expected, status, null);
            if(ifOk != null) {
                res = ifOk.call();
            }
            closeable.close();
        }
        return res;
    }

    protected void fail(JobStatus status, Throwable e) {
        this.statusRef.set(status);
        statusChanged(null, status, e).close();
    }

    private SafeCloseable statusChanged(JobStatus expected, JobStatus status, Throwable e) {
        JobInfo old = this.infoRef.get();
        if(expected == null) {
            expected = old.getStatus();
        }
        while(true) {
            JobInfo.Builder jib = JobInfo.builder().from(old);
            if(jib.getStatus() == expected) {
                jib.status(status);
            }
            if(status.isEnd()) {
                jib.setEndTime(LocalDateTime.now());
            }
            if(status == JobStatus.STARTED) {
                jib.setStartTime(LocalDateTime.now());
            }
            JobInfo newInfo = jib.build();
            boolean change = setInfo(old, newInfo);
            if(change) {
                return () -> sendEvent(new JobEvent(newInfo, null, e));
            }
            old = this.infoRef.get();
        }
    }

    boolean setInfo(JobInfo expected, JobInfo value) {
        return this.infoRef.compareAndSet(expected, value);
    }

    @Override
    public JobInfo getInfo() {
        return infoRef.get();
    }

    @Override
    public List<JobEvent> getLog() {
        return Collections.unmodifiableList(this.events);
    }

    @Override
    public ListenableFuture<Boolean> cancel() {
        if(!this.cancelFuture.isDone()) {
            this.manager.execute(this.cancelFuture);
        }
        return this.cancelFuture;
    }

    @Override
    public ListenableFuture<Boolean> start() {
        if(!this.startFuture.isDone()) {
            this.manager.execute(this.startFuture);
        }
        return this.startFuture;
    }

    @Override
    public ListenableFuture<JobInstance> atEnd() {
        return atEndFuture;
    }

    protected class JobWrapper implements Runnable {

        private final Runnable job;

        JobWrapper(Runnable job) {
            this.job = job;
        }

        @Override
        public void run() {
            JobContext.set(jobContext);
            try (TempAuth auth = TempAuth.open(authentication)) {
                setStatus(JobStatus.STARTED);
                loadAttributesFromResult();
                job.run();
                Boolean res = compareAndSetStatus(JobStatus.STARTED, completedStatus(), () -> true);
                if(res != null && res) {
                    atEndFuture.set(AbstractJobInstance.this);
                }
            } catch (Throwable e) {
                fail(failedStatus(), e);
                LOG.error("On {} job.", getInfo().getId(), e);
                atEndFuture.setException(e);
            } finally {
                JobContext.remove();
                clearAfterIteration();
            }
        }

        private void loadAttributesFromResult() {
            jobContext.getAttributes().putAll(jobContext.getResult());
        }
    }

    protected abstract void clearAfterIteration();

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("infoRef", infoRef)
                .add("cancelFuture", cancelFuture)
                .add("startFuture", startFuture)
                .add("manager", manager)
                .add("job", job)
                .add("atEndFuture", atEndFuture)
                .add("jobContext", jobContext)
                .add("statusRef", statusRef)
                .add("executeHandle", executeHandle)
                .add("credentials", authentication)
                .omitNullValues()
                .toString();
    }
}
