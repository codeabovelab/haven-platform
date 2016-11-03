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

import com.codeabovelab.dm.cluman.cluster.docker.model.Port;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Now iot simply copy of docker container with additional fields, but in future we need to refactor it.
 */
public class DockerContainer implements ContainerBaseIface, WithNode {
    public static class Builder {
        private String id;
        private String name;
        private String image;
        private String imageId;
        private String command;
        private long created;
        private final List<Port> ports = new ArrayList<>();
        private final Map<String, String> labels = new HashMap<>();
        private String status;
        private Node node;

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

        public String getImageId() {
            return imageId;
        }

        public Builder imageId(String imageId) {
            setImageId(imageId);
            return this;
        }

        public void setImageId(String imageId) {
            this.imageId = imageId;
        }

        public String getCommand() {
            return command;
        }

        public Builder command(String command) {
            setCommand(command);
            return this;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public long getCreated() {
            return created;
        }

        public Builder created(long created) {
            setCreated(created);
            return this;
        }

        public void setCreated(long created) {
            this.created = created;
        }

        public List<Port> getPorts() {
            return ports;
        }

        public Builder ports(List<Port> ports) {
            setPorts(ports);
            return this;
        }

        public void setPorts(List<Port> ports) {
            this.ports.clear();
            if(ports != null) {
                this.ports.addAll(ports);
            }
        }

        public Map<String, String> getLabels() {
            return labels;
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

        public String getStatus() {
            return status;
        }

        public Builder status(String status) {
            setStatus(status);
            return this;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Node getNode() {
            return node;
        }

        public Builder node(Node node) {
            setNode(node);
            return this;
        }

        public void setNode(Node node) {
            this.node = node;
        }

        public Builder from(ContainerBaseIface container) {
            setId(container.getId());
            setName(container.getName());
            setImage(container.getImage());
            setImageId(container.getImageId());
            if(container instanceof DockerContainer) {
                DockerContainer dc = (DockerContainer) container;
                setCommand(dc.getCommand());
                setCreated(dc.getCreated());
                setPorts(dc.getPorts());
                setLabels(dc.getLabels());
                setStatus(dc.getStatus());
                setNode(dc.getNode());
            }
            return this;
        }

        public DockerContainer build() {
            return new DockerContainer(this);
        }

    }

    private final String id;
    private final String name;
    private final String image;
    private final String imageId;
    private final String command;
    private final long created;
    private final List<Port> ports;
    private final Map<String, String> labels;
    private final String status;
    private final Node node;

    public DockerContainer(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.image = builder.image;
        this.imageId = builder.imageId;
        this.command = builder.command;
        this.created = builder.created;
        this.ports = ImmutableList.copyOf(builder.ports);
        this.labels = ImmutableMap.copyOf(builder.labels);
        this.status = builder.status;
        this.node = builder.node;
        Assert.notNull(this.node, "node is null");
        Assert.notNull(this.imageId, "imageId is null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        Builder b = new Builder();
        b.setId(this.id);
        b.setName(this.name);
        b.setNode(this.node);
        b.setImage(this.image);
        b.setImageId(this.imageId);
        b.setCommand(this.command);
        b.setCreated(this.created);
        b.setPorts(this.ports);
        b.setLabels(this.labels);
        b.setStatus(this.status);
        return b;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getImage() {
        return image;
    }

    public String getImageId() {
        return imageId;
    }

    public String getCommand() {
        return command;
    }

    public long getCreated() {
        return created;
    }

    public List<Port> getPorts() {
        return ports;
    }

    @Override
    public Map<String, String> getLabels() {
        return labels;
    }

    public String getStatus() {
        return status;
    }

    /**
     * value calculated from status
     * @return
     */
    public boolean isRun() {
        return status != null && status.contains("Up");
    }

    @Override
    public Node getNode() {
        return node;
    }

    @Override
    public String toString() {
        return "DockerContainer{" +
          "id='" + id + '\'' +
          ", name=" + name +
          ", image='" + image + '\'' +
          ", command='" + command + '\'' +
          ", created=" + created +
          ", ports=" + ports +
          ", labels=" + labels +
          ", status='" + status + '\'' +
          '}';
    }
}
