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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compose schema_v2.0
 */
@Data
public class ComposeModel {

    /**
     * Name of container
     */
    @JsonProperty("container_name")
    private String containerName;

    /**
     * CPU share constraint, for example: "512"
     * (equals shell option: -c [cpuShares])
     * see https://docs.docker.com/engine/reference/run/#cpu-share-constraint
     */
    @JsonProperty("cpu_shares")
    private Integer cpuShares;

    /**
     * the --cpu-quota flag limits the container’s CPU usage. The default 0 value allows the container to take 100% of a CPU resource (1 CPU).
     * The CFS (Completely Fair Scheduler) handles resource allocation for executing processes and is default Linux Scheduler used by the kernel.
     * Set this value to 50000 to limit the container to 50% of a CPU resource. For multiple CPUs, adjust the --cpu-quota as necessary.
     * For more information, see the CFS documentation on bandwidth limiting.
     * see https://docs.docker.com/engine/reference/run/#cpu-share-constraint
     */
    @JsonProperty("cpu_quota")
    private Integer cpuQuota;
    @JsonProperty("cpuset")
    private String cpuset;
    @JsonProperty("depends_on")
    private List<String> dependsOn;
    @JsonProperty("devices")
    private List<String> devices;
    @JsonProperty("dns")
    private List<String> dns;
    @JsonProperty("dns_search")
    private List<String> dnsSearch;
    @JsonProperty("domainname")
    private String domainname;
    @JsonProperty("env_file")
    private String envFile;

    /**
     * Environment variables
     * for example: "JAVA_OPTS=Xmx1G"
     * equals shell option: -e [env]
     */
    @JsonProperty("environment")
    private Map<String, String> environment = new HashMap<>();
    @JsonProperty("extends")
    private String extend;
    @JsonProperty("external_links")
    private String externalLinks;
    @JsonProperty("extra_hosts")
    private List<String> extraHosts;
    @JsonProperty("hostname")
    private String hostname;

    /**
     * Name of image
     */
    @JsonProperty("image")
    @NotNull
    private String image;
    @JsonProperty("ipc")
    private String ipc;

    /**
     * Docker labels.
     */
    @JsonProperty("labels")
    private Map<String, String> labels = new HashMap<>();
    @JsonProperty("links")
    private Map<String, String> links;
    @JsonProperty("logging")
    private Logging logging;
    @JsonProperty("mac_address")
    private String macAddress;

    /**
     * Memory constraint, for example: "3G"
     * (equals shell option: -m [memory])
     */
    @JsonProperty("mem_limit")
    private String memory;
    @JsonProperty("network_mode")
    private String networkMode;
    @JsonProperty("networks")
    private Set<String> networks;
    @JsonProperty("pid")
    private String pid;

    /**
     * Publish a container's port(s) to the host
     */
    @JsonProperty("publish")
    private Map<String, String> publish;
    @JsonProperty("privileged")
    private Boolean privileged;
    @JsonProperty("read_only")
    private Boolean readOnly;

    /**
     * Restart policies (–restart), for example: "always" or "on-failure:2"
     * (equals shell option: –restart [restart])
     */
    @JsonProperty("restart")
    private String restart;
    @JsonProperty("security_opt")
    private List<String> securityOpt;
    @JsonProperty("shm_size")
    private Integer shmSize;
    @JsonProperty("stdin_open")
    private Boolean stdinOpen;
    @JsonProperty("stop_signal")
    private String stopSignal;
    @JsonProperty("user")
    private String user;
    @JsonProperty("volumes")
    private Map<String, String> volumes;
    @JsonProperty("volume_driver")
    private String volumeDriver;
    @JsonProperty("volumes_from")
    private Set<String> volumesFrom;
    @JsonProperty("working_dir")
    private String workingDir;

    /**
     * alias for publish
     * @return
     */
    @JsonProperty("ports")
    public Map<String, String> getPorts() {
        return getPublish();
    }

    /**
     * alias for publish
     * @return
     */
    @JsonProperty("ports")
    public void setPorts(Map<String, String> ports) {
        setPublish(ports);
    }

}

