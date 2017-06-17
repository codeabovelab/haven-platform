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

import com.codeabovelab.dm.common.utils.Throwables;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

@Slf4j
public class JsonStreamProcessor<T> implements ResponseStreamProcessor<T> {

    private static final Logger LOG = LoggerFactory.getLogger(JsonStreamProcessor.class);

    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
    }

    private final Class<T> clazz;

    public JsonStreamProcessor(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void processResponseStream(StreamContext<T> context) {
        Consumer<T> watcher = context.getWatcher();
        InputStream response = context.getStream();
        final Thread thread = Thread.currentThread();
        SettableFuture<Boolean> interrupter = context.getInterrupter();
        interrupter.addListener(thread::interrupt, MoreExecutors.directExecutor());
        try {
            JsonParser jp = JSON_FACTORY.createParser(response);
            Boolean closed = jp.isClosed();
            JsonToken nextToken = jp.nextToken();
            while (!closed && nextToken != null && nextToken != JsonToken.END_OBJECT && !interrupter.isDone()) {
                try {
                    ObjectNode objectNode = OBJECT_MAPPER.readTree(jp);
                    // exclude empty item serialization into class #461
                    if (!objectNode.isEmpty(null)) {
                        T next = OBJECT_MAPPER.treeToValue(objectNode, clazz);
                        LOG.trace("Monitor value: {}", next);
                        watcher.accept(next);
                    }
                } catch (Exception e) {
                    log.error("Error on process json item.", e);
                }

                closed = jp.isClosed();
                nextToken = jp.nextToken();
            }
        } catch (Throwable t) {
            throw Throwables.asRuntime(t);
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                LOG.error("Can't close stream", e);

            }
        }

    }

}
