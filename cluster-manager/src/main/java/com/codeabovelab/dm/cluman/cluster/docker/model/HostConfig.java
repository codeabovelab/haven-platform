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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.Collections;
import java.util.List;

/**
 * Used in `/containers/create`, and in inspect container.
 * TODO exclude usage for 2 different models.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HostConfig {

    @JsonProperty("Binds")
    private List<String> binds;

    @JsonProperty("BlkioWeight")
    private Integer blkioWeight;

    /**
     * @since {@link com.github.dockerjava.core.RemoteApiVersion#VERSION_1_22}
     */
    @JsonProperty("BlkioWeightDevice")
    private List<Object> blkioWeightDevice;

    /**
     * @since {@link com.github.dockerjava.core.RemoteApiVersion#VERSION_1_22}
     */
    @JsonProperty("BlkioDeviceReadBps")
    private List<Object> blkioDeviceReadBps;

    /**
     * @since {@link com.github.dockerjava.core.RemoteApiVersion#VERSION_1_22}
     */
    @JsonProperty("BlkioDeviceReadIOps")
    private List<Object> blkioDeviceReadIOps;

    /**
     * @since {@link com.github.dockerjava.core.RemoteApiVersion#VERSION_1_22}
     */
    @JsonProperty("BlkioDeviceWriteBps")
    private List<Object> blkioDeviceWriteBps;

    /**
     * @since {@link com.github.dockerjava.core.RemoteApiVersion#VERSION_1_22}
     */
    @JsonProperty("BlkioDeviceWriteIOps")
    private List<Object> blkioDeviceWriteIOps;

    /**
     * @since {@link com.github.dockerjava.core.RemoteApiVersion#VERSION_1_20}
     */
    @JsonProperty("MemorySwappiness")
    private Integer memorySwappiness;

    @JsonProperty("CapAdd")
    private Capability[] capAdd;

    @JsonProperty("CapDrop")
    private Capability[] capDrop;

    @JsonProperty("ContainerIDFile")
    private String containerIDFile;

    @JsonProperty("CpuPeriod")
    private Integer cpuPeriod;

    @JsonProperty("CpuShares")
    private Integer cpuShares;

    /**
     * @since ~{@link RemoteApiVersion#VERSION_1_20}
     */
    @JsonProperty("CpuQuota")
    private Integer cpuQuota;

    @JsonProperty("CpusetCpus")
    private String cpusetCpus;

    @JsonProperty("CpusetMems")
    private String cpusetMems;

    @JsonProperty("Devices")
    private Device[] devices;

    @JsonProperty("Dns")
    private List<String> dns;

    @JsonProperty("DnsSearch")
    private List<String> dnsSearch;

    @JsonProperty("ExtraHosts")
    private List<String> extraHosts;

    @JsonProperty("Links")
    private Links links;

    @JsonProperty("LogConfig")
    private LogConfig logConfig;

    @JsonProperty("LxcConf")
    private LxcConf[] lxcConf;

    @JsonProperty("Memory")
    private Long memory;

    @JsonProperty("MemorySwap")
    private Long memorySwap;

    /**
     * @since {@link RemoteApiVersion#VERSION_1_21}
     */
    @JsonProperty("MemoryReservation")
    private Long memoryReservation;

    /**
     * @since {@link RemoteApiVersion#VERSION_1_21}
     */
    @JsonProperty("KernelMemory")
    private Long kernelMemory;

    @JsonProperty("NetworkMode")
    private String networkMode;

    @JsonProperty("OomKillDisable")
    private Boolean oomKillDisable;

    /**
     * @since {@link RemoteApiVersion#VERSION_1_22}
     */
    @JsonProperty("OomScoreAdj")
    private Boolean oomScoreAdj;

    @JsonProperty("PortBindings")
    private Ports portBindings;

    @JsonProperty("Privileged")
    private Boolean privileged;

    @JsonProperty("PublishAllPorts")
    private Boolean publishAllPorts;

    @JsonProperty("ReadonlyRootfs")
    private Boolean readonlyRootfs;

    @JsonProperty("RestartPolicy")
    private RestartPolicy restartPolicy;

    @JsonProperty("Ulimits")
    private Ulimit[] ulimits;

    @JsonProperty("VolumesFrom")
    private VolumesFrom[] volumesFrom;

    @JsonProperty("PidMode")
    private String pidMode;

    /**
     * @since {@link RemoteApiVersion#VERSION_1_20}
     */
    @JsonProperty("SecurityOpt")
    private List<String> securityOpts;

    /**
     * @since {@link RemoteApiVersion#VERSION_1_20}
     */
    @JsonProperty("CgroupParent")
    private String cgroupParent;

    /**
     * @since {@link RemoteApiVersion#VERSION_1_21}
     */
    @JsonProperty("VolumeDriver")
    private String volumeDriver;

    /**
     * @since {@link RemoteApiVersion#VERSION_1_22}
     */
    @JsonProperty("ShmSize")
    private String shmSize;

    public HostConfig() {
    }

    private HostConfig(Builder builder) {
        this.binds = builder.binds;
        this.blkioWeight = builder.blkioWeight;
        this.blkioWeightDevice = builder.blkioWeightDevice;
        this.blkioDeviceReadBps = builder.blkioDeviceReadBps;
        this.blkioDeviceReadIOps = builder.blkioDeviceReadIOps;
        this.blkioDeviceWriteBps = builder.blkioDeviceWriteBps;
        this.blkioDeviceWriteIOps = builder.blkioDeviceWriteIOps;
        this.memorySwappiness = builder.memorySwappiness;
        this.capAdd = builder.capAdd;
        this.capDrop = builder.capDrop;
        this.containerIDFile = builder.containerIDFile;
        this.cpuPeriod = builder.cpuPeriod;
        this.cpuShares = builder.cpuShares;
        this.cpuQuota = builder.cpuQuota;
        this.cpusetCpus = builder.cpusetCpus;
        this.cpusetMems = builder.cpusetMems;
        this.devices = builder.devices;
        this.dns = builder.dns;
        this.dnsSearch = builder.dnsSearch;
        this.extraHosts = builder.extraHosts;
        this.links = builder.links;
        this.logConfig = builder.logConfig;
        this.lxcConf = builder.lxcConf;
        this.memory = builder.memory;
        this.memorySwap = builder.memorySwap;
        this.memoryReservation = builder.memoryReservation;
        this.kernelMemory = builder.kernelMemory;
        this.networkMode = builder.networkMode;
        this.oomKillDisable = builder.oomKillDisable;
        this.oomScoreAdj = builder.oomScoreAdj;
        this.portBindings = builder.portBindings;
        this.privileged = builder.privileged;
        this.publishAllPorts = builder.publishAllPorts;
        this.readonlyRootfs = builder.readonlyRootfs;
        this.restartPolicy = builder.restartPolicy;
        this.ulimits = builder.ulimits;
        this.volumesFrom = builder.volumesFrom;
        this.pidMode = builder.pidMode;
        this.securityOpts = builder.securityOpts;
        this.cgroupParent = builder.cgroupParent;
        this.volumeDriver = builder.volumeDriver;
        this.shmSize = builder.shmSize;
    }

    public static Builder newHostConfig() {
        return new Builder();
    }


    @JsonIgnore
    public List<String> getBinds() {
        return (binds == null) ? Collections.emptyList() : binds;
    }

    public Integer getBlkioWeight() {
        return blkioWeight;
    }

    public Capability[] getCapAdd() {
        return capAdd;
    }

    public Capability[] getCapDrop() {
        return capDrop;
    }

    public String getContainerIDFile() {
        return containerIDFile;
    }

    public Integer getCpuPeriod() {
        return cpuPeriod;
    }

    public Integer getCpuShares() {
        return cpuShares;
    }

    public String getCpusetCpus() {
        return cpusetCpus;
    }

    public String getCpusetMems() {
        return cpusetMems;
    }

    public Device[] getDevices() {
        return devices;
    }

    public List<String> getDns() {
        return dns;
    }

    public List<String> getDnsSearch() {
        return dnsSearch;
    }

    public List<String> getExtraHosts() {
        return extraHosts;
    }

    @JsonIgnore
    public List<Link> getLinks() {
        return (links == null) ? Collections.emptyList() : links.getLinks();
    }

    @JsonIgnore
    public LogConfig getLogConfig() {
        return (logConfig == null) ? new LogConfig() : logConfig;
    }

    public LxcConf[] getLxcConf() {
        return lxcConf;
    }

    public Long getMemory() {
        return memory;
    }

    public Long getMemorySwap() {
        return memorySwap;
    }

    public String getNetworkMode() {
        return networkMode;
    }

    public Ports getPortBindings() {
        return portBindings;
    }

    public RestartPolicy getRestartPolicy() {
        return restartPolicy;
    }

    public Ulimit[] getUlimits() {
        return ulimits;
    }

    public VolumesFrom[] getVolumesFrom() {
        return volumesFrom;
    }

    public Boolean isOomKillDisable() {
        return oomKillDisable;
    }

    public Boolean isPrivileged() {
        return privileged;
    }

    public Boolean isPublishAllPorts() {
        return publishAllPorts;
    }

    public Boolean isReadonlyRootfs() {
        return readonlyRootfs;
    }

    public String getPidMode() {
        return pidMode;
    }

    /**
     * @see #blkioDeviceReadBps
     */
    public List<Object> getBlkioDeviceReadBps() {
        return blkioDeviceReadBps;
    }

    /**
     * @see #blkioDeviceReadIOps
     */
    public List<Object> getBlkioDeviceReadIOps() {
        return blkioDeviceReadIOps;
    }

    /**
     * @see #blkioDeviceWriteBps
     */
    public List<Object> getBlkioDeviceWriteBps() {
        return blkioDeviceWriteBps;
    }

    /**
     * @see #blkioDeviceWriteIOps
     */
    public List<Object> getBlkioDeviceWriteIOps() {
        return blkioDeviceWriteIOps;
    }

    /**
     * @see #blkioWeightDevice
     */
    public List<Object> getBlkioWeightDevice() {
        return blkioWeightDevice;
    }

    /**
     * @see #oomScoreAdj
     */
    public Boolean getOomScoreAdj() {
        return oomScoreAdj;
    }

    /**
     * @see #cpuQuota
     */
    public Integer getCpuQuota() {
        return cpuQuota;
    }

    /**
     * @see #kernelMemory
     */
    public Long getKernelMemory() {
        return kernelMemory;
    }

    /**
     * @see #memoryReservation
     */
    public Long getMemoryReservation() {
        return memoryReservation;
    }

    /**
     * @see #memorySwappiness
     */
    public Integer getMemorySwappiness() {
        return memorySwappiness;
    }

    /**
     * @see #oomKillDisable
     */
    public Boolean getOomKillDisable() {
        return oomKillDisable;
    }

    /**
     * @see #securityOpts
     */
    public List<String> getSecurityOpts() {
        return securityOpts;
    }

    /**
     * @see #cgroupParent
     */
    public String getCgroupParent() {
        return cgroupParent;
    }

    /**
     * @see #shmSize
     */
    public String getShmSize() {
        return shmSize;
    }

    /**
     * @see #volumeDriver
     */
    public String getVolumeDriver() {
        return volumeDriver;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("binds", binds)
                .add("blkioWeight", blkioWeight)
                .add("blkioWeightDevice", blkioWeightDevice)
                .add("blkioDeviceReadBps", blkioDeviceReadBps)
                .add("blkioDeviceReadIOps", blkioDeviceReadIOps)
                .add("blkioDeviceWriteBps", blkioDeviceWriteBps)
                .add("blkioDeviceWriteIOps", blkioDeviceWriteIOps)
                .add("memorySwappiness", memorySwappiness)
                .add("capAdd", capAdd)
                .add("capDrop", capDrop)
                .add("containerIDFile", containerIDFile)
                .add("cpuPeriod", cpuPeriod)
                .add("cpuShares", cpuShares)
                .add("cpuQuota", cpuQuota)
                .add("cpusetCpus", cpusetCpus)
                .add("cpusetMems", cpusetMems)
                .add("devices", devices)
                .add("dns", dns)
                .add("dnsSearch", dnsSearch)
                .add("extraHosts", extraHosts)
                .add("links", links)
                .add("logConfig", logConfig)
                .add("lxcConf", lxcConf)
                .add("memory", memory)
                .add("memorySwap", memorySwap)
                .add("memoryReservation", memoryReservation)
                .add("kernelMemory", kernelMemory)
                .add("networkMode", networkMode)
                .add("oomKillDisable", oomKillDisable)
                .add("oomScoreAdj", oomScoreAdj)
                .add("portBindings", portBindings)
                .add("privileged", privileged)
                .add("publishAllPorts", publishAllPorts)
                .add("readonlyRootfs", readonlyRootfs)
                .add("restartPolicy", restartPolicy)
                .add("ulimits", ulimits)
                .add("volumesFrom", volumesFrom)
                .add("pidMode", pidMode)
                .add("securityOpts", securityOpts)
                .add("cgroupParent", cgroupParent)
                .add("volumeDriver", volumeDriver)
                .add("shmSize", shmSize)
                .omitNullValues()
                .toString();
    }

    public static final class Builder {
        private List<String> binds;
        private Integer blkioWeight;
        private List<Object> blkioWeightDevice;
        private List<Object> blkioDeviceReadBps;
        private List<Object> blkioDeviceReadIOps;
        private List<Object> blkioDeviceWriteBps;
        private List<Object> blkioDeviceWriteIOps;
        private Integer memorySwappiness;
        private Capability[] capAdd;
        private Capability[] capDrop;
        private String containerIDFile;
        private Integer cpuPeriod;
        private Integer cpuShares;
        private Integer cpuQuota;
        private String cpusetCpus;
        private String cpusetMems;
        private Device[] devices;
        private List<String> dns;
        private List<String> dnsSearch;
        private List<String> extraHosts;
        private Links links;
        private LogConfig logConfig;
        private LxcConf[] lxcConf;
        private Long memory;
        private Long memorySwap;
        private Long memoryReservation;
        private Long kernelMemory;
        private String networkMode;
        private Boolean oomKillDisable;
        private Boolean oomScoreAdj;
        private Ports portBindings;
        private Boolean privileged;
        private Boolean publishAllPorts;
        private Boolean readonlyRootfs;
        private RestartPolicy restartPolicy;
        private Ulimit[] ulimits;
        private VolumesFrom[] volumesFrom;
        private String pidMode;
        private List<String> securityOpts;
        private String cgroupParent;
        private String volumeDriver;
        private String shmSize;

        private Builder() {
        }

        public HostConfig build() {
            return new HostConfig(this);
        }

        public Builder binds(List<String> binds) {
            this.binds = binds;
            return this;
        }

        public Builder blkioWeight(Integer blkioWeight) {
            this.blkioWeight = blkioWeight;
            return this;
        }

        public Builder blkioWeightDevice(List<Object> blkioWeightDevice) {
            this.blkioWeightDevice = blkioWeightDevice;
            return this;
        }

        public Builder blkioDeviceReadBps(List<Object> blkioDeviceReadBps) {
            this.blkioDeviceReadBps = blkioDeviceReadBps;
            return this;
        }

        public Builder blkioDeviceReadIOps(List<Object> blkioDeviceReadIOps) {
            this.blkioDeviceReadIOps = blkioDeviceReadIOps;
            return this;
        }

        public Builder blkioDeviceWriteBps(List<Object> blkioDeviceWriteBps) {
            this.blkioDeviceWriteBps = blkioDeviceWriteBps;
            return this;
        }

        public Builder blkioDeviceWriteIOps(List<Object> blkioDeviceWriteIOps) {
            this.blkioDeviceWriteIOps = blkioDeviceWriteIOps;
            return this;
        }

        public Builder memorySwappiness(Integer memorySwappiness) {
            this.memorySwappiness = memorySwappiness;
            return this;
        }

        public Builder capAdd(Capability[] capAdd) {
            this.capAdd = capAdd;
            return this;
        }

        public Builder capDrop(Capability[] capDrop) {
            this.capDrop = capDrop;
            return this;
        }

        public Builder containerIDFile(String containerIDFile) {
            this.containerIDFile = containerIDFile;
            return this;
        }

        public Builder cpuPeriod(Integer cpuPeriod) {
            this.cpuPeriod = cpuPeriod;
            return this;
        }

        public Builder cpuShares(Integer cpuShares) {
            this.cpuShares = cpuShares;
            return this;
        }

        public Builder cpuQuota(Integer cpuQuota) {
            this.cpuQuota = cpuQuota;
            return this;
        }

        public Builder cpusetCpus(String cpusetCpus) {
            this.cpusetCpus = cpusetCpus;
            return this;
        }

        public Builder cpusetMems(String cpusetMems) {
            this.cpusetMems = cpusetMems;
            return this;
        }

        public Builder devices(Device[] devices) {
            this.devices = devices;
            return this;
        }

        public Builder dns(List<String> dns) {
            this.dns = dns;
            return this;
        }

        public Builder dnsSearch(List<String> dnsSearch) {
            this.dnsSearch = dnsSearch;
            return this;
        }

        public Builder extraHosts(List<String> extraHosts) {
            this.extraHosts = extraHosts;
            return this;
        }

        public Builder links(Links links) {
            this.links = links;
            return this;
        }

        public Builder logConfig(LogConfig logConfig) {
            this.logConfig = logConfig;
            return this;
        }

        public Builder lxcConf(LxcConf[] lxcConf) {
            this.lxcConf = lxcConf;
            return this;
        }

        public Builder memory(Long memory) {
            this.memory = memory;
            return this;
        }

        public Builder memorySwap(Long memorySwap) {
            this.memorySwap = memorySwap;
            return this;
        }

        public Builder memoryReservation(Long memoryReservation) {
            this.memoryReservation = memoryReservation;
            return this;
        }

        public Builder kernelMemory(Long kernelMemory) {
            this.kernelMemory = kernelMemory;
            return this;
        }

        public Builder networkMode(String networkMode) {
            this.networkMode = networkMode;
            return this;
        }

        public Builder oomKillDisable(Boolean oomKillDisable) {
            this.oomKillDisable = oomKillDisable;
            return this;
        }

        public Builder oomScoreAdj(Boolean oomScoreAdj) {
            this.oomScoreAdj = oomScoreAdj;
            return this;
        }

        public Builder portBindings(Ports portBindings) {
            this.portBindings = portBindings;
            return this;
        }

        public Builder privileged(Boolean privileged) {
            this.privileged = privileged;
            return this;
        }

        public Builder publishAllPorts(Boolean publishAllPorts) {
            this.publishAllPorts = publishAllPorts;
            return this;
        }

        public Builder readonlyRootfs(Boolean readonlyRootfs) {
            this.readonlyRootfs = readonlyRootfs;
            return this;
        }

        public Builder restartPolicy(RestartPolicy restartPolicy) {
            this.restartPolicy = restartPolicy;
            return this;
        }

        public Builder ulimits(Ulimit[] ulimits) {
            this.ulimits = ulimits;
            return this;
        }

        public Builder volumesFrom(VolumesFrom[] volumesFrom) {
            this.volumesFrom = volumesFrom;
            return this;
        }

        public Builder pidMode(String pidMode) {
            this.pidMode = pidMode;
            return this;
        }

        public Builder securityOpts(List<String> securityOpts) {
            this.securityOpts = securityOpts;
            return this;
        }

        public Builder cgroupParent(String cgroupParent) {
            this.cgroupParent = cgroupParent;
            return this;
        }

        public Builder volumeDriver(String volumeDriver) {
            this.volumeDriver = volumeDriver;
            return this;
        }

        public Builder shmSize(String shmSize) {
            this.shmSize = shmSize;
            return this;
        }
    }
}