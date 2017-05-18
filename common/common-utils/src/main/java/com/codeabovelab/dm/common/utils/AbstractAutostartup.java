/*
 * Copyright 2017 Code Above Lab LLC
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

import org.springframework.context.SmartLifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class AbstractAutostartup implements SmartLifecycle {

    private boolean running;
    private final List<AutoCloseable> closeables = new ArrayList<>();

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public final void stop(Runnable callback) {
        if(!this.running) {
            return;
        }
        this.running = false;
        Closeables.closeAll(closeables);
        closeables.clear();
        stopInner(callback);
    }

    @Override
    public final void start() {
        this.running = true;
        startInner();
    }

    /**
     * Here you may place own shutdown logic. Do not forget call <code>callback.run()</code>
     */
    protected void stopInner(Runnable callback) {
        callback.run();
    }

    /**
     * Here you may place own startup logic.
     */
    protected void startInner() {

    }


    @Override
    public void stop() {
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    protected void addToClose(AutoCloseable closeable) {
        this.closeables.add(closeable);
    }
}
