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

import lombok.Data;

/**
 */
@Data
final class OfflineCause {
    /**
     * Used for cases when service never been accessed before. Without this we
     * cannot detect 'cluster online' event at first cluster connection after startup.
     */
    static final OfflineCause INITIAL = new OfflineCause(0, null, Long.MIN_VALUE);

    private final long time;
    private final long timeout;
    private final Throwable throwable;

    OfflineCause(long timeout, Throwable throwable) {
        this(timeout, throwable, System.currentTimeMillis());
    }

    private OfflineCause(long timeout, Throwable throwable, long time) {
        this.time = time;
        this.timeout = timeout;
        this.throwable = throwable;
    }

    void throwIfActual(DockerService dockerService) {
        if(isActual()) {
            throw new DockerException("Docker of " + dockerService.getId() + " is offline.");
        }
    }

    boolean isActual() {
        return this.time + this.timeout >= System.currentTimeMillis();
    }
}
