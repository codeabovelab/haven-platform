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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * You must create watcher for each
 */
public class ScheduledJobWatcher implements JobWatcher {

    public static final int MAX_ERRORS = 10;
    private final int maxErrors;
    private final AtomicInteger errors = new AtomicInteger();

    public ScheduledJobWatcher() {
        this.maxErrors = MAX_ERRORS;
    }

    @Override
    public void onEvent(JobInstance instance, JobEvent event) {
        Throwable ex = event.getException();
        if(ex != null) {
            int count = errors.incrementAndGet();
            if(count >= maxErrors) {
                instance.cancel();
            }
        }
    }
}
