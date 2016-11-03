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

import com.codeabovelab.dm.cluman.cluster.docker.management.result.ProcessEvent;
import com.codeabovelab.dm.cluman.cluster.docker.model.Frame;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.function.Consumer;

public class ProcessEventProcessor implements ResponseStreamProcessor<ProcessEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessEventProcessor.class);

    @Override
    public void processResponseStream(StreamContext<ProcessEvent> context) {
        Consumer<ProcessEvent> watcher = context.getWatcher();
        InputStream response = context.getStream();
        SettableFuture<Boolean> interrupter = context.getInterrupter();
        interrupter.addListener(() -> Thread.currentThread().interrupt(), MoreExecutors.directExecutor());
        try (FrameReader frameReader = new FrameReader(response)) {

            Frame frame = frameReader.readFrame();
            while (frame != null && !interrupter.isDone()) {
                try {
                    ProcessEvent.watchRaw(watcher, frame.getMessage(), false);
                } catch (Exception e) {
                    LOG.error("Cannot read body", e);
                } finally {
                    frame = frameReader.readFrame();
                }
            }
        } catch (Exception t) {
            LOG.error("Cannot close reader", t);
        }

    }
}