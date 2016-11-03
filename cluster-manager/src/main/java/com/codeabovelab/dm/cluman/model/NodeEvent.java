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

/**
 * Event of node changes. Usually clean different caches.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public final class NodeEvent extends Event implements WithCluster, WithAction {

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Builder extends Event.Builder<Builder, NodeEvent> {

        private NodeInfo node;
        private String action;

        public Builder node(NodeInfo node) {
            setNode(node);
            return this;
        }

        /**
         * @see StandardActions
         * @param action
         * @return
         */
        public Builder action(String action) {
            setAction(action);
            return this;
        }

        @Override
        public NodeEvent build() {
            return new NodeEvent(this);
        }
    }

    /**
     * Id of message bus
     */
    public static final String BUS = "bus.cluman.node";
    private final NodeInfo node;
    private final String action;

    @JsonCreator
    public NodeEvent(Builder b) {
        super(b);
        this.node = b.node;
        this.action = b.action;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getCluster() {
        return node.getCluster();
    }
}
