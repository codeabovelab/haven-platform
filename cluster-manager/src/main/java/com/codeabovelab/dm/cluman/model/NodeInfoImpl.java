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

import com.codeabovelab.dm.common.kv.mapping.KvMapping;
import com.codeabovelab.dm.common.utils.Comparables;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

@Data
public class NodeInfoImpl implements NodeInfo, Comparable<NodeInfoImpl> {

    public static final NodeMetrics DEFAULT_METRICS = NodeMetrics.builder()
      .healthy(false)
      .state(NodeMetrics.State.DISCONNECTED)
      .build();

    @Data
    public static final class Builder implements NodeInfo {
        private long version;
        private String name;
        @NotNull
        @KvMapping
        private String address;
        @KvMapping
        private String cluster;
        @KvMapping
        private String idInCluster;
        private boolean on;
        private NodeMetrics health = DEFAULT_METRICS;
        @KvMapping
        private final Map<String, String> labels = new HashMap<>();

        public Builder() {
        }

        public Builder from(Node node) {
            if(node == null) {
                return this;
            }
            setName(node.getName());
            setAddress(node.getAddress());
            if(node instanceof NodeInfo) {
                NodeInfo ni = (NodeInfo) node;
                labels(ni.getLabels());
                setOn(ni.isOn());
                setCluster(ni.getCluster());
                setVersion(ni.getVersion());
                setIdInCluster(ni.getIdInCluster());
                setHealth(ni.getHealth());
            }
            return this;
        }

        public NodeInfoImpl build() {
            return new NodeInfoImpl(this);
        }

        /**
         * Update current health with non null values from specified object.
         * @param metrics
         * @return
         */
        public Builder mergeHealth(NodeMetrics metrics) {
            NodeMetrics oldHealth = this.health;
            if (oldHealth != null && oldHealth != NodeInfoImpl.DEFAULT_METRICS) {
                setHealth(NodeMetrics.builder().from(oldHealth)
                  .fromNonNull(metrics)
                  .build());
            } else {
                setHealth(metrics);
            }
            return this;
        }

        public Builder health(NodeMetrics health) {
            setHealth(health);
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder version(long version) {
            setVersion(version);
            return this;
        }

        public Builder address(String host) {
            setAddress(host);
            return this;
        }

        public Builder labels(Map<String, String> labels) {
            setLabels(labels);
            return this;
        }

        public void setLabels(Map<String, String> labels) {
            this.labels.clear();
            if(labels != null) {
                this.labels.putAll(labels);
            }
        }

        public Builder on(boolean on) {
            setOn(on);
            return this;
        }

        public Builder cluster(String cluster) {
            setCluster(cluster);
            return this;
        }

        public Builder idInCluster(String idInCluster) {
            setIdInCluster(idInCluster);
            return this;
        }
    }

    private final String name;
    private final long version;
    private final boolean on;
    private final String address;
    private final String cluster;
    private final String idInCluster;
    private final Map<String, String> labels;
    private final NodeMetrics health;

    @JsonCreator
    public NodeInfoImpl(Builder builder) {
        this.name = builder.name;
        this.version = builder.version;
        this.on = builder.on;
        this.address = builder.address;
        this.cluster = builder.cluster;
        this.idInCluster = builder.idInCluster;
        this.health = builder.health;
        this.labels = builder.labels;
    }

    public static NodeInfoImpl of(NodeInfo src) {
        NodeInfoImpl res;
        if(src instanceof NodeInfoImpl) {
            res = (NodeInfoImpl) src;
        } else if(src instanceof NodeInfoImpl.Builder) {
            res = ((NodeInfoImpl.Builder)src).build();
        } else {
            res = NodeInfoImpl.builder().from(src).build();
        }
        return res;
    }

    public static Builder builder(NodeInfo nodeInfo) {
        return new Builder().from(nodeInfo);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Use {@linkplain #getName()}. This method is remain for backward capability with JSON public api.
     * @return
     */
    @Deprecated
    public String getId() {
        return getName();
    }

    @Override
    public int compareTo(NodeInfoImpl o) {
        int comp = Comparables.compare(getCluster(), o.getCluster());
        if(comp == 0) {
            comp = Comparables.compare(getName(), o.getName());
        }
        return comp;
    }
}
