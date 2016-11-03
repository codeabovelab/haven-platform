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

import com.codeabovelab.dm.common.utils.Sugar;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health data and statistics about node. <p/>
 * We use only box objects instead of primitives for correct {@link Builder#fromNonNull(NodeMetrics) merging} data from different sources.
 */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS) // for info we want to see all fields include nulls
public class NodeMetrics {

    public enum State {
        PENDING, UNHEALTHY, HEALTHY, DISCONNECTED, MAINTENANCE
    }

    @Data
    public static class Builder {
        private LocalDateTime time;
        private Boolean healthy;
        private State state;
        private Long swarmMemReserved;
        private Long swarmMemTotal;
        private Integer swarmCpusReserved;
        private Integer swarmCpusTotal;
        private Long sysMemAvail;
        private Long sysMemTotal;
        private Long sysMemUsed;
        private Float sysCpuLoad;
        private final Map<String, DiskInfo> disks = new HashMap<>();
        private final Map<String, NetIfaceCounter> net = new HashMap<>();

        public Builder time(LocalDateTime time) {
            setTime(time);
            return this;
        }

        public Builder addDisk(DiskInfo diskInfo) {
            disks.put(diskInfo.getMount(), diskInfo);
            return this;
        }

        public void setDisks(Map<String, DiskInfo> disks) {
            this.disks.clear();
            if(disks != null) {
                this.disks.putAll(disks);
            }
        }

        public Builder addNet(NetIfaceCounter net) {
            this.net.put(net.getName(), net);
            return this;
        }

        public void setNet(Map<String, NetIfaceCounter> net) {
            this.net.clear();
            if(net != null) {
                this.net.putAll(net);
            }
        }

        public NodeMetrics build() {
            return new NodeMetrics(this);
        }

        public Builder from(NodeMetrics metric) {
            if(metric == null) {
                return this;
            }
            setTime(metric.getTime());
            setHealthy(metric.getHealthy());
            setState(metric.getState());
            setSwarmMemReserved(metric.getSwarmMemReserved());
            setSwarmMemTotal(metric.getSwarmMemTotal());
            setSwarmCpusReserved(metric.getSwarmCpusReserved());
            setSwarmCpusTotal(metric.getSwarmCpusTotal());
            setSysMemAvail(metric.getSysMemAvail());
            setSysMemTotal(metric.getSysMemTotal());
            setSysMemUsed(metric.getSysMemUsed());
            setSysCpuLoad(metric.getSysCpuLoad());
            setDisks(metric.getDisks());
            setNet(metric.getNet());
            return this;
        }

        /**
         * Set only non null values from source object
         * @param metrics
         * @return
         */
        public Builder fromNonNull(NodeMetrics metrics) {
            if(metrics == null) {
                return this;
            }
            //choose latest time
            LocalDateTime time = getTime();
            LocalDateTime newTime = metrics.getTime();
            if(time == null || newTime != null && newTime.isAfter(time)) {
                setTime(newTime);
            }
            Sugar.setIfNotNull(this::setHealthy, metrics.getHealthy());
            Sugar.setIfNotNull(this::setState, metrics.getState());
            Sugar.setIfNotNull(this::setSwarmMemReserved, metrics.getSwarmMemReserved());
            Sugar.setIfNotNull(this::setSwarmMemTotal, metrics.getSwarmMemTotal());
            Sugar.setIfNotNull(this::setSwarmCpusReserved, metrics.getSwarmCpusReserved());
            Sugar.setIfNotNull(this::setSwarmCpusTotal, metrics.getSwarmCpusTotal());
            Sugar.setIfNotNull(this::setSysMemAvail, metrics.getSysMemAvail());
            Sugar.setIfNotNull(this::setSysMemTotal, metrics.getSysMemTotal());
            Sugar.setIfNotNull(this::setSysMemUsed, metrics.getSysMemUsed());
            Sugar.setIfNotNull(this::setSysCpuLoad, metrics.getSysCpuLoad());
            Map<String, DiskInfo> disks = metrics.getDisks();
            if(!CollectionUtils.isEmpty(disks)) {
                this.setDisks(disks);
            }
            Map<String, NetIfaceCounter> net = metrics.getNet();
            if(!CollectionUtils.isEmpty(net)) {
                this.setNet(net);
            }
            return this;
        }


        public Builder healthy(Boolean healthy) {
            this.healthy = healthy;
            return this;
        }
        public Builder state(State state) {
            this.state = state;
            return this;
        }
        public Builder swarmMemReserved(Long swarmMemReserved) {
            this.swarmMemReserved = swarmMemReserved;
            return this;
        }
        public Builder swarmMemTotal(Long swarmMemTotal) {
            this.swarmMemTotal = swarmMemTotal;
            return this;
        }
        public Builder sysMemAvail(Long sysMemAvail) {
            this.sysMemAvail = sysMemAvail;
            return this;
        }
        public Builder sysMemTotal(Long sysMemTotal) {
            this.sysMemTotal = sysMemTotal;
            return this;
        }
        public Builder sysMemUsed(Long sysMemUsed) {
            this.sysMemUsed = sysMemUsed;
            return this;
        }
        public Builder sysCpuLoad(Float sysCpuLoad) {
            this.sysCpuLoad = sysCpuLoad;
            return this;
        }


    }

    private final LocalDateTime time;
    private final Boolean healthy;
    private final State state;
    private final Long swarmMemReserved;
    private final Long swarmMemTotal;
    private final Integer swarmCpusReserved;
    private final Integer swarmCpusTotal;
    private final Long sysMemAvail;
    private final Long sysMemTotal;
    private final Long sysMemUsed;
    private final Float sysCpuLoad;
    private final Map<String, DiskInfo> disks;
    private final Map<String, NetIfaceCounter> net;

    @JsonCreator
    public NodeMetrics(Builder b) {
        this.time = b.time;
        this.healthy = b.healthy;
        this.state = b.state;
        this.swarmMemReserved = b.swarmMemReserved;
        this.swarmMemTotal = b.swarmMemTotal;
        this.swarmCpusReserved = b.swarmCpusReserved;
        this.swarmCpusTotal = b.swarmCpusTotal;
        this.sysMemAvail = b.sysMemAvail;
        this.sysMemTotal = b.sysMemTotal;
        this.sysMemUsed = b.sysMemUsed;
        this.sysCpuLoad = b.sysCpuLoad;
        this.disks = ImmutableMap.copyOf(b.disks);
        this.net = ImmutableMap.copyOf(b.net);
    }


    public static Builder builder() {
        return new Builder();
    }
}
