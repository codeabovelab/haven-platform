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

package com.codeabovelab.dm.cluman.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 'service' - in terms of docker swarm-mode. Has id, name and represent couple
 * of instances of equals containers.
 */
@Data
public class ContainerService {

    @Data
    public static class Builder {
        private String id;
        private String name;
        private String cluster;
        private final List<String> command = new ArrayList<>();
        private final List<Port> ports = new ArrayList<>();
        private LocalDateTime created;
        private LocalDateTime updated;
        private final Map<String, String> labels = new HashMap<>();
        private String image;
        private String imageId;

        public void setLabels(Map<String, String> labels) {
            this.labels.clear();
            if(labels != null) {
                this.labels.putAll(labels);
            }
        }

        public void setCommand(List<String> command) {
            this.command.clear();
            if(command != null) {
                this.command.addAll(command);
            }
        }

        public void setPorts(List<Port> ports) {
            this.ports.clear();
            if(ports != null) {
                this.ports.addAll(ports);
            }
        }

        public ContainerService build() {
            return new ContainerService(this);
        }
    }

    private final String id;
    private final String name;
    private final String cluster;
    private final LocalDateTime created;
    private final LocalDateTime updated;
    private final Map<String, String> labels;
    private final String image;
    private final String imageId;
    private final List<String> command;
    private final List<Port> ports;

    public ContainerService(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.cluster = b.cluster;
        this.created = b.created;
        this.updated = b.updated;
        this.labels = ImmutableMap.copyOf(b.labels);
        this.ports = ImmutableList.copyOf(b.ports);
        this.command = ImmutableList.copyOf(b.command);
        this.image = b.image;
        this.imageId = b.imageId;
    }

    public static Builder builder() {
        return new Builder();
    }
}
