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

package com.codeabovelab.dm.cluman.cluster.docker.management.result;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.function.Consumer;

/**
 * Process event entry
 */
@Data
@Slf4j
public class ProcessEvent {
    private final Long time;
    private final String message;

    @JsonCreator
    public ProcessEvent(@JsonProperty("time") Long time, @JsonProperty("message") String message) {
        this.time = time;
        this.message = message;
    }

    /**
     * Accept null watcher and use {@link MessageFormat} for interpolating 'msg' with args.
     * @param watcher
     * @param msg a string or template for {@link MessageFormat}
     * @param args
     */
    public static void watch(Consumer<ProcessEvent> watcher, String msg, Object ... args) {
        String formatted;
        if(args.length > 0) {
            formatted = MessageFormat.format(msg, args);
        } else {
            formatted = msg;
        }
        watchRaw(watcher, formatted, true);
    }

    /**
     * Pass message without any processing to watcher if watcher is not null. If watcher is null do nothing.
     * @param watcher
     * @param msg
     * @param addTime to event
     */
    public static void watchRaw(Consumer<ProcessEvent> watcher, String msg, boolean addTime) {
        ProcessEvent event = new ProcessEvent(addTime ? System.currentTimeMillis() : null, msg);
        // below line in some cases send to many info into log, also it put log from each container to our log
        //log.info("added event {}", event);
        if(watcher != null) {
            watcher.accept(event);
        }
    }

    @Override
    public String toString() {
        return message;
    }
}
