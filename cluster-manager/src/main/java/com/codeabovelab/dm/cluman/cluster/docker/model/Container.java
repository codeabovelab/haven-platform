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

package com.codeabovelab.dm.cluman.cluster.docker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.List;
import java.util.Map;

/**
 * Used for Listing containers.
 *
 * @author Konstantin Pelykh (kpelykh@gmail.com)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Container {

    @JsonProperty("Command")
    private String command;

    @JsonProperty("Created")
    private Long created;

    @JsonProperty("Id")
    private String id;

    @JsonProperty("Image")
    private String image;

    /**
     * @since since {@link RemoteApiVersion#VERSION_1_21}
     */
    @JsonProperty("ImageID")
    private String imageId;

    @JsonProperty("Names")
    private String[] names;

    @JsonProperty("Ports")
    public List<Port> ports;

    @JsonProperty("Labels")
    public Map<String, String> labels;

    @JsonProperty("Status")
    private String status;

    /**
     * @since ~{@link RemoteApiVersion#VERSION_1_19}
     */
    @JsonProperty("SizeRw")
    private Long sizeRw;

    /**
     * Returns only when {@link ListContainersCmd#withShowSize(java.lang.Boolean)} set
     *
     * @since ~{@link RemoteApiVersion#VERSION_1_19}
     */
    @JsonProperty("SizeRootFs")
    private Long sizeRootFs;

    /**
     * @since ~{@link RemoteApiVersion#VERSION_1_20}
     */
    @JsonProperty("HostConfig")
    private ContainerHostConfig hostConfig;

    /**
     * Docker API docs says "list of networks", but json names `networkSettings`.
     * So, reusing existed NetworkSettings model object.
     *
     * @since ~{@link RemoteApiVersion#VERSION_1_22}
     */
    @JsonProperty("NetworkSettings")
    private ContainerNetworkSettings networkSettings;

    public String getId() {
        return id;
    }

    public String getCommand() {
        return command;
    }

    public String getImage() {
        return image;
    }

    public String getImageId() {
        return imageId;
    }

    public Long getCreated() {
        return created;
    }

    public String getStatus() {
        return status;
    }

    public List<Port> getPorts() {
        return ports;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public String[] getNames() {
        return names;
    }

    /**
     * @see #sizeRw
     */
    public Long getSizeRw() {
        return sizeRw;
    }

    /**
     * @see #sizeRootFs
     */
    public Long getSizeRootFs() {
        return sizeRootFs;
    }

    /**
     * @see #networkSettings
     */
    public ContainerNetworkSettings getNetworkSettings() {
        return networkSettings;
    }

    /**
     * @see #hostConfig
     */
    public ContainerHostConfig getHostConfig() {
        return hostConfig;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("command", command)
                .add("created", created)
                .add("id", id)
                .add("image", image)
                .add("imageId", imageId)
                .add("names", names)
                .add("ports", ports)
                .add("labels", labels)
                .add("status", status)
                .add("sizeRw", sizeRw)
                .add("sizeRootFs", sizeRootFs)
                .add("hostConfig", hostConfig)
                .add("networkSettings", networkSettings)
                .omitNullValues()
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Container container = (Container) o;
        return com.google.common.base.Objects.equal(command, container.command) &&
                Objects.equal(created, container.created) &&
                Objects.equal(id, container.id) &&
                Objects.equal(image, container.image) &&
                Objects.equal(imageId, container.imageId) &&
                Objects.equal(names, container.names) &&
                Objects.equal(ports, container.ports) &&
                Objects.equal(labels, container.labels) &&
                Objects.equal(status, container.status) &&
                Objects.equal(sizeRw, container.sizeRw) &&
                Objects.equal(sizeRootFs, container.sizeRootFs) &&
                Objects.equal(hostConfig, container.hostConfig) &&
                Objects.equal(networkSettings, container.networkSettings);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(command, created, id, image, imageId, names, ports, labels, status, sizeRw, sizeRootFs, hostConfig, networkSettings);
    }
}