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

package com.codeabovelab.dm.cluman.batch;

import com.codeabovelab.dm.cluman.model.ContainerBaseIface;
import com.codeabovelab.dm.cluman.model.ContainerSource;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableMap;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * Concrete container representation
 */
public class ProcessedContainer implements ContainerBaseIface {

    public enum State {
        OK, FAILED
    }

    public static final class Builder {

        private String id;
        private String image;
        private String imageId;
        private ProcessedContainer old;
        private String name;
        private String node;
        private State state;
        private String cluster;
        private ContainerSource src;
        private final Map<String, String> labels = new HashMap<>();

        @JsonCreator
        public Builder() {

        }

        private Builder(ProcessedContainer pc) {
            this.id = pc.id;
            this.image = pc.image;
            this.imageId = pc.imageId;
            this.name = pc.name;
            this.node = pc.node;
            this.old = pc.old;
            this.state = pc.state;
            this.cluster = pc.cluster;
            this.src = pc.src;
            this.setLabels(pc.labels);
        }

        public String getId() {
            return id;
        }

        public Builder id(String id) {
            setId(id);
            return this;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getImage() {
            return image;
        }

        public Builder image(String image) {
            setImage(image);
            return this;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public Builder imageId(String imageId) {
            setImageId(imageId);
            return this;
        }

        public String getImageId() {
            return imageId;
        }

        public void setImageId(String imageId) {
            this.imageId = imageId;
        }

        public ProcessedContainer getOld() {
            return old;
        }

        public Builder old(ProcessedContainer old) {
            setOld(old);
            return this;
        }

        public void setOld(ProcessedContainer old) {
            this.old = old;
        }

        public String getName() {
            return name;
        }

        public Builder name(String name) {
            setName(name);
            return this;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNode() {
            return node;
        }

        public Builder node(String node) {
            setNode(node);
            return this;
        }

        public void setNode(String node) {
            this.node = node;
        }

        public State getState() {
            return state;
        }

        public Builder state(State state) {
            setState(state);
            return this;
        }

        public void setState(State state) {
            this.state = state;
        }

        public String getCluster() {
            return cluster;
        }

        public Builder cluster(String cluster) {
            setCluster(cluster);
            return this;
        }

        public void setCluster(String cluster) {
            this.cluster = cluster;
        }

        public Map<String, String> getLabels() {
            return labels;
        }

        public Builder putLabel(String key, String value) {
            this.labels.put(key, value);
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

        public ContainerSource getSrc() {
            return src;
        }

        public void setSrc(ContainerSource src) {
            this.src = src;
        }

        public Builder src(ContainerSource arg) {
            this.setSrc(arg);
            return this;
        }

        public ProcessedContainer build() {
            return new ProcessedContainer(this);
        }
    }

    private final ProcessedContainer old;
    private final String id;
    private final String name;
    private final String node;
    private final String image;
    private final String imageId;
    private final State state;
    private final String cluster;
    private final Map<String, String> labels;
    private final ContainerSource src;

    @JsonCreator
    public ProcessedContainer(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.node = b.node;
        this.image = b.image;
        this.imageId = b.imageId;
        this.old = b.old;
        this.state = b.state;
        this.cluster = b.cluster;
        this.labels = ImmutableMap.copyOf(b.labels);
        this.src = b.src;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCluster() {
        return cluster;
    }

    public String getName() {
        return name;
    }

    public String getNode() {
        return node;
    }

    public String getId() {
        return id;
    }

    /**
     * Make copy of current object with new id.
     * @param id
     * @return
     */
    public ProcessedContainer withId(String id) {
        return new Builder(this).id(id).build();
    }

    public String getImage() {
        return image;
    }

    public String getImageId() {
        return imageId;
    }

    /**
     * Previous version of container.
     * @return
     */
    public ProcessedContainer getOld() {
        return old;
    }

    /**
     * State of container, set by healthcheck, and may be null
     * @return
     */
    public State getState() {
        return state;
    }

    @Override
    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     * Make builder with copy of this. It also copy all fields.
     * @return
     */
    public Builder makeCopy() {
        return new Builder(this);
    }

    /**
     * Create new version (it place 'this' into 'Builder.old')
     * @return
     */
    public Builder makeNew() {
        return new Builder(this).old(this);
    }

    public ContainerSource getSrc() {
        return src;
    }

    @Override
    public String toString() {
        return "ProcessedContainer{" +
          "id='" + id + '\'' +
          ", name='" + name + '\'' +
          ", node='" + node + '\'' +
          ", image='" + image + '\'' +
          ", state=" + state +
          ", cluster='" + cluster + '\'' +
          '}';
    }
}
