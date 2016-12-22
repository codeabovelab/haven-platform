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
import com.google.common.collect.ImmutableList;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Data
public class ApplicationImpl implements Application {

    @Data
    public static class Builder {
        private String name;
        private String cluster;
        private String initFile;
        private Date creatingDate;
        private final List<String> containers = new ArrayList<>();

        public Builder name(String name) {
            setName(name);
            return this;
        }

        public Builder cluster(String cluster) {
            setCluster(cluster);
            return this;
        }

        public Builder initFile(String initFile) {
            setInitFile(initFile);
            return this;
        }

        public Builder creatingDate(Date creatingDate) {
            setCreatingDate(creatingDate);
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

        public Builder from(Application application) {
            setName(application.getName());
            setCluster(application.getCluster());
            setInitFile(application.getInitFile());
            setCreatingDate(application.getCreatingDate());
            setContainers(application.getContainers());
            return this;
        }

        public ApplicationImpl build() {
            return new ApplicationImpl(this);
        }
    }

    private final String name;
    private final String cluster;
    private final String initFile;
    private final Date creatingDate;
    private final List<String> containers;

    @JsonCreator
    public ApplicationImpl(Builder b) {
        this.name = b.name;
        this.cluster = b.cluster;
        this.initFile = b.initFile;
        this.creatingDate = b.creatingDate;
        this.containers = ImmutableList.copyOf(b.containers);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<String> getContainers() {
        return containers;
    }

    public static ApplicationImpl from(Application application) {
        if(application instanceof ApplicationImpl) {
            return (ApplicationImpl) application;
        }
        return ApplicationImpl.builder().from(application).build();
    }
}
