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


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.Map;

/**
 * Log driver to use for a created/running container. The available types are:
 *
 * json-file (default) syslog journald none
 *
 * If a driver is specified that is NOT supported,docker will default to null. If configs are supplied that are not
 * supported by the type docker will ignore them. In most cases setting the config option to null will suffice. Consult
 * the docker remote API for a more detailed and up-to-date explanation of the available types and their options.
 */
public class LogConfig {

    @JsonProperty("Type")
    public LoggingType type = null;

    @JsonProperty("Config")
    public Map<String, String> config;

    public LogConfig(LoggingType type, Map<String, String> config) {
        this.type = type;
        this.config = config;
    }

    public LogConfig(LoggingType type) {
        this(type, null);
    }

    public LogConfig() {
    }

    public LoggingType getType() {
        return type;
    }

    public LogConfig setType(LoggingType type) {
        this.type = type;
        return this;
    }

    @JsonIgnore
    public Map<String, String> getConfig() {
        return config;
    }

    @JsonIgnore
    public LogConfig setConfig(Map<String, String> config) {
        this.config = config;
        return this;
    }

    @JsonDeserialize(using = LoggingType.Deserializer.class)
    @JsonSerialize(using = LoggingType.Serializer.class)
    public enum LoggingType {
        DEFAULT("json-file"), JSON_FILE("json-file"), NONE("none"), SYSLOG("syslog"), JOURNALD("journald");

        private String type;

        LoggingType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public static final class Serializer extends JsonSerializer<LoggingType> {
            @Override
            public void serialize(LoggingType value, JsonGenerator jgen, SerializerProvider provider)
                    throws IOException {
                jgen.writeString(value.getType());
            }
        }

        public static final class Deserializer extends JsonDeserializer<LoggingType> {
            @Override
            public LoggingType deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                    throws IOException {

                ObjectCodec oc = jsonParser.getCodec();
                JsonNode node = oc.readTree(jsonParser);

                for (LoggingType loggingType : values()) {
                    if (loggingType.getType().equals(node.asText()))
                        return loggingType;
                }

                throw new IllegalArgumentException("No enum constant " + LoggingType.class + "." + node.asText());
            }
        }
    }

    @Override
    public String toString() {
        return "LogConfig{" +
                "type=" + type +
                ", config=" + config +
                '}';
    }
}
