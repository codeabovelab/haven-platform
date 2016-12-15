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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

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
}
