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

import com.codeabovelab.dm.cluman.model.ContainerBaseIface;
import com.codeabovelab.dm.common.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Detailed info for container
 * https://github.com/docker/docker/blob/e1da516598e6f4e8f58964fce62ff13be1d8cc09/api/types/types.go#L331
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ContainerDetails implements ContainerBaseIface {

    @JsonProperty("Args")
    private List<String> args;

    @JsonProperty("Config")
    private ContainerConfig config;

    @JsonProperty("Created")
    private Date created;

    @JsonProperty("Driver")
    private String driver;

    @JsonProperty("ExecDriver")
    private String execDriver;

    @JsonProperty("HostConfig")
    private HostConfig hostConfig;

    @JsonProperty("HostnamePath")
    private String hostnamePath;

    @JsonProperty("HostsPath")
    private String hostsPath;

    @JsonProperty("Id")
    private String id;

    @JsonProperty("Image")
    private String imageId;

    @JsonProperty("MountLabel")
    private String mountLabel;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("NetworkSettings")
    private NetworkSettings networkSettings;

    @JsonProperty("RestartCount")
    private Integer restartCount;

    @JsonProperty("Path")
    private String path;

    @JsonProperty("ProcessLabel")
    private String processLabel;

    @JsonProperty("ResolvConfPath")
    private String resolvConfPath;

    @JsonProperty("ExecIDs")
    private List<String> execIds;

    @JsonProperty("State")
    private ContainerState state;

    @JsonProperty("Mounts")
    private List<MountPoint> mounts;

    @JsonProperty("Node")
    private Node node;

    @Override
    public String getImage() {
        return config.getImage();
    }

    @Override
    public Map<String, String> getLabels() {
        return config.getLabels();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class MountPoint {

        @JsonProperty("Type")
        private Mount.Type type;

        @JsonProperty("Name")
        private String name;

        @JsonProperty("Source")
        private String source;

        @JsonProperty("Destination")
        private String destination;

        @JsonProperty("Driver")
        private String driver;

        @JsonProperty("Mode")
        private String mode;

        @JsonProperty("RW")
        private boolean rw;

        @JsonProperty("Propagation")
        private final Mount.Propagation propagation;

        public boolean isSystem() {
            return name != null && name.trim().length() == 64 && StringUtils.matchHex(name);
        }
    }
}
