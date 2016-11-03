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

package com.codeabovelab.dm.cluman.cluster.docker.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum  EventType {

    /**
     * @since 1.24
     */
    CONTAINER("container"),

    /**
     * @since 1.24
     */
    DAEMON("daemon"),

    /**
     * @since 1.24
     */
    IMAGE("image"),
    NETWORK("network"),
    PLUGIN("plugin"),
    VOLUME("volume");

    private static final Map<String, EventType> EVENT_TYPES = new HashMap<>();

    static {
        for (EventType t : values()) {
            EVENT_TYPES.put(t.name().toLowerCase(), t);
        }
    }

    private String value;

    EventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static EventType forValue(String s) {
        return EVENT_TYPES.get(s);
    }

}
