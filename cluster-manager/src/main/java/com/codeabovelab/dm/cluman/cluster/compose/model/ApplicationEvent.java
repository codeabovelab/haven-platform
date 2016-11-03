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

package com.codeabovelab.dm.cluman.cluster.compose.model;

import com.codeabovelab.dm.cluman.model.LogEvent;
import com.codeabovelab.dm.cluman.model.WithCluster;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableList;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApplicationEvent extends LogEvent implements WithCluster {

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Builder extends LogEvent.Builder<Builder, ApplicationEvent> {

        private String applicationName;
        private String fileName;
        private String clusterName;
        private final List<String> containers = new ArrayList<>();

        public Builder applicationName(String applicationName) {
            setApplicationName(applicationName);
            return this;
        }

        public Builder fileName(String fileName) {
            setFileName(fileName);
            return this;
        }

        public Builder clusterName(String clusterName) {
            setClusterName(clusterName);
            return this;
        }

        public Builder containers(List<String> containers) {
            setContainers(containers);
            return this;
        }

        public void setContainers(List<String> containers) {
            this.containers.clear();
            if(containers != null) {
                this.containers.addAll(containers);
            }
        }

        @Override
        public ApplicationEvent build() {
            return new ApplicationEvent(this);
        }
    }
    public static final String BUS = "bus.cluman.log.application";

    private final String applicationName;
    private final String fileName;
    private final String clusterName;
    private final List<String> containers;

    @JsonCreator
    public ApplicationEvent(Builder b) {
        super(b);
        this.applicationName = b.applicationName;
        this.fileName = b.fileName;
        this.clusterName = b.clusterName;
        this.containers = ImmutableList.copyOf(b.containers);
    }

    @Override
    public String getCluster() {
        return this.clusterName;
    }

    public static Builder builder() {
        return new Builder();
    }
}
