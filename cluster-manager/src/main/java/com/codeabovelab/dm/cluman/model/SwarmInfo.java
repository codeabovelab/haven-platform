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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;

/**
 */
@Data
public final class SwarmInfo {

    @Data
    public static class Builder {
        private String clusterId;
        private String nodeId;
        private boolean manager;

        public Builder clusterId(String clusterId) {
            setClusterId(clusterId);
            return this;
        }

        public Builder nodeId(String nodeId) {
            setNodeId(nodeId);
            return this;
        }

        public Builder manager(boolean manager) {
            setManager(manager);
            return this;
        }

        public SwarmInfo build() {
            return new SwarmInfo(this);
        }
    }

    private final String clusterId;
    private final String nodeId;
    private final boolean manager;

    @JsonCreator
    public SwarmInfo(Builder b) {
        this.clusterId = b.clusterId;
        this.manager = b.manager;
        this.nodeId = b.nodeId;
    }

    public static Builder builder() {
        return new Builder();
    }
}
