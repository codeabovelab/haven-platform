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

package com.codeabovelab.dm.cluman.model;

import com.codeabovelab.dm.cluman.cluster.docker.model.EventType;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DockerLogEvent extends LogEvent implements WithCluster {

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Builder extends LogEvent.Builder<Builder, DockerLogEvent> {

        private String cluster;
        private String node;
        private ContainerBase container;
        private String status;
        private EventType type;

        public Builder type(EventType type) {
            setType(type);
            return this;
        }

        public Builder cluster(String cluster) {
            setCluster(cluster);
            return this;
        }

        public Builder node(String node) {
            setNode(node);
            return this;
        }

        public Builder container(ContainerBase container) {
            setContainer(container);
            return this;
        }

        public Builder status(String status) {
            setStatus(status);
            return this;
        }

        @Override
        public DockerLogEvent build() {
            return new DockerLogEvent(this);
        }
    }

    public static final String BUS = "bus.cluman.log.docker";

    private final String cluster;
    private final String node;

    private final ContainerBase container;

    /**
     * Status of docker image or container. List of statuses is available in <a
     * href="https://docs.docker.com/reference/api/docker_remote_api_v1.16/#monitor-dockers-events">Docker API v.1.16</a>
     */
    private final String status;
    private final EventType type;

    @JsonCreator
    public DockerLogEvent(Builder b) {
        super(b);
        this.type = b.type;
        this.cluster = b.cluster;
        this.node = b.node;
        this.container = b.container;
        this.status = b.status;
    }

    public static Builder builder() {
        return new Builder();
    }
}
