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

package com.codeabovelab.dm.common.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Data;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Common utilities for {@link java.util.concurrent.Executor }
 */
public final class ExecutorUtils {
    public static final Executor DIRECT = command -> command.run();

    /**
     * Executor deffer tasks into internal storage and run its only at {@link #flush()} .
     */
    public static class DeferredExecutor implements Executor {
        private final Object lock = new Object();
        private volatile List<Runnable> queue = new CopyOnWriteArrayList<>();

        @Override
        public void execute(Runnable command) {
            synchronized (lock) {
                queue.add(command);
            }
        }

        /**
         * Execute all scheduled tasks.
         */
        public void flush() {
            List<Runnable> old;
            synchronized (lock) {
                old = this.queue;
                this.queue = new CopyOnWriteArrayList<>();
            }
            for(Runnable runnable: old) {
                runnable.run();
            }
        }
    }

    /**
     * @see DeferredExecutor
     * @return new instance of {@link DeferredExecutor }
     */
    public static DeferredExecutor deferred() {
        return new DeferredExecutor();
    }

    private static class ThreadFactoryImpl implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger count = new AtomicInteger(1);
        private final String prefix;
        private final boolean daemon;
        private final Thread.UncaughtExceptionHandler ueh;

        ThreadFactoryImpl(String name, boolean daemon, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
            SecurityManager sm = System.getSecurityManager();
            group = (sm != null)? sm.getThreadGroup():
              Thread.currentThread().getThreadGroup();
            this.prefix = name.endsWith("-")? name : (name + "-");
            this.daemon = daemon;
            this.ueh = uncaughtExceptionHandler;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(group, r, prefix + count.getAndIncrement());
            thread.setDaemon(daemon);
            thread.setPriority(Thread.NORM_PRIORITY);
            if(this.ueh != null) {
                thread.setUncaughtExceptionHandler(this.ueh);
            }
            return thread;
        }
    }

    @Data
    public static final class ExecutorBuilder {
        private String name;
        private boolean daemon = true;
        private Thread.UncaughtExceptionHandler exceptionHandler = Throwables.uncaughtHandler();
        private RejectedExecutionHandler rejectedHandler = new ThreadPoolExecutor.AbortPolicy();
        private int maxSize = 5;
        private int coreSize = 2;
        private long keepAlive = 30;
        private int queueSize = 10;

        /**
         * Name of thread, without thread number pattern.
         * @param name name
         * @return this
         */
        public ExecutorBuilder name(String name) {
            setName(name);
            return this;
        }

        /**
         * Daemon flag.
         * @param daemon default true
         * @return this
         */
        public ExecutorBuilder daemon(boolean daemon) {
            setDaemon(daemon);
            return this;
        }

        /**
         * Uncaught exception handler.
         * @param exceptionHandler handler, default {@link Throwables#uncaughtHandler()}
         * @return this
         */
        public ExecutorBuilder exceptionHandler(Thread.UncaughtExceptionHandler exceptionHandler) {
            setExceptionHandler(exceptionHandler);
            return this;
        }

        /**
         * Rejected execution handler.
         * @param rejectedHandler rejected execution handler, default {@link ThreadPoolExecutor.AbortPolicy()}
         * @return this
         */
        public ExecutorBuilder rejectedHandler(RejectedExecutionHandler rejectedHandler) {
            setRejectedHandler(rejectedHandler);
            return this;
        }

        public ExecutorBuilder coreSize(int coreSize) {
            setCoreSize(coreSize);
            return this;
        }

        public ExecutorBuilder maxSize(int maxSize) {
            setMaxSize(maxSize);
            return this;
        }

        public ExecutorBuilder keepAlive(long keepAlive) {
            setKeepAlive(keepAlive);
            return this;
        }

        public ExecutorBuilder queueSize(int queueSize) {
            setQueueSize(queueSize);
            return this;
        }

        public ExecutorService build() {
            ThreadFactory tf = new ThreadFactoryImpl(name, daemon, exceptionHandler);
            return new ThreadPoolExecutor(coreSize, maxSize, keepAlive, TimeUnit.SECONDS, new ArrayBlockingQueue<>(queueSize), tf, rejectedHandler);
        }
    }

    public static ExecutorBuilder executorBuilder() {
        return new ExecutorBuilder();
    }

    /**
     * Make single daemon thread scheduled executor service.
     * @param clazz class, it user for calculation name of thread
     * @return executor service
     */
    public static ScheduledExecutorService singleThreadScheduledExecutor(Class<?> clazz) {
        return Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat(clazz.getSimpleName() + "-%d")
          .build());
    }
}
