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

import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Service;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Task;
import com.google.common.collect.ImmutableList;
import lombok.Data;

import java.util.List;

/**
 * 'service' - in terms of docker swarm-mode. Has id, name and represent couple
 * of instances of equals containers.
 */
@Data
public class ContainerService {

    @Data
    public static class Builder {
        private String cluster;
        private Service service;
        private List<Task> tasks;

        public ContainerService build() {
            return new ContainerService(this);
        }
    }

    private final String cluster;
    private final Service service;
    private final List<Task> tasks;

    public ContainerService(Builder b) {
        this.cluster = b.cluster;
        this.service = b.service;
        this.tasks = b.tasks == null? ImmutableList.of() : ImmutableList.copyOf(b.tasks);
    }

    public static Builder builder() {
        return new Builder();
    }
}
