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
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class NodesGroupEvent extends LogEvent implements WithCluster {

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Builder extends LogEvent.Builder<Builder, NodesGroupEvent> {

        private String cluster;

        public Builder cluster(String cluster) {
            setCluster(cluster);
            return this;
        }

        @Override
        public NodesGroupEvent build() {
            return new NodesGroupEvent(this);
        }
    }

    public static final String BUS = "bus.cluman.log.nodesGroup";
    private final String cluster;

    @JsonCreator
    public NodesGroupEvent(Builder b) {
        super(b);
        this.cluster = b.cluster;
    }

    public static Builder builder() {
        return new Builder();
    }
}
