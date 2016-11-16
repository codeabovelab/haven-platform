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

package com.codeabovelab.dm.cluman.cluster.docker;

import com.codeabovelab.dm.cluman.ds.swarm.Strategies;
import com.codeabovelab.dm.common.utils.Smelter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableList;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for docker service api config
 */
@EqualsAndHashCode
@Data
public class ClusterConfigImpl implements ClusterConfig {


    /**
     * Convert config to {@link  ClusterConfigImpl } or null
     * @param cc config or null
     * @return config or null
     */
    public static ClusterConfigImpl of(ClusterConfig cc) {
        if(cc instanceof ClusterConfigImpl || cc == null) {
            return (ClusterConfigImpl) cc;
        }
        return builder(cc).build();
    }

    /**
     * Constant for hold default values of config.
     */
    private static final ClusterConfigImpl DEFAULT = ClusterConfigImpl.builder().build();

    @Data
    public static class Builder implements ClusterConfig {


        /**
         * List of docker/swarm 'host:port'
         * @return host
         */
        private final List<String> hosts = new ArrayList<>();
        private int maxCountOfInstances = 1;
        private String dockerRestart;
        private String cluster;
        private Strategies strategy = Strategies.DEFAULT;
        /**
         * Time in seconds, which data was cached after last write.
         */
        private long cacheTimeAfterWrite = 10L;
        private int dockerTimeout = 5 * 60;
        /**
         * Name of registries
         * @return
         */
        private final List <String> registries = new ArrayList<>();

        public Builder from(ClusterConfig orig) {
            if(orig == null) {
                return this;
            }
            setHosts(orig.getHosts());
            setMaxCountOfInstances(orig.getMaxCountOfInstances());
            setDockerRestart(orig.getDockerRestart());
            setCluster(orig.getCluster());
            setRegistries(orig.getRegistries());
            setStrategy(orig.getStrategy());
            setCacheTimeAfterWrite(orig.getCacheTimeAfterWrite());
            setDockerTimeout(orig.getDockerTimeout());
            return this;
        }

        /**
         * Set only not default fields from specified config.
         * @param src
         */
        public Builder merge(ClusterConfig src) {
            if(src == null) {
                return this;
            }
            Smelter<ClusterConfig> s = new Smelter<>(src, DEFAULT);
            s.set(this::setHosts, ClusterConfig::getHosts);
            s.setInt(this::setMaxCountOfInstances, ClusterConfig::getMaxCountOfInstances);
            s.set(this::setDockerRestart, ClusterConfig::getDockerRestart);
            s.set(this::setCluster, ClusterConfig::getCluster);
            s.set(this::setRegistries, ClusterConfig::getRegistries);
            s.set(this::setStrategy, ClusterConfig::getStrategy);
            s.setLong(this::setCacheTimeAfterWrite, ClusterConfig::getCacheTimeAfterWrite);
            s.setInt(this::setDockerTimeout, ClusterConfig::getDockerTimeout);
            return this;
        }

        public Builder cluster(String cluster) {
            setCluster(cluster);
            return this;
        }

        public Builder addHost(String host) {
            checkHost(host);
            this.hosts.add(host);
            return this;
        }

        private void checkHost(String host) {
            Assert.hasText(host, "Host is null or empty");
        }

        public Builder hosts(List<String> hosts) {
            setHosts(hosts);
            return this;
        }

        public void setHosts(List<String> hosts) {
            this.hosts.clear();
            if(hosts != null) {
                hosts.forEach(this::checkHost);
                this.hosts.addAll(hosts);
            }
        }

        public Builder maxCountOfInstances(int maxCountOfInstances) {
            setMaxCountOfInstances(maxCountOfInstances);
            return this;
        }

        public Builder dockerRestart(String dockerRestart) {
            setDockerRestart(dockerRestart);
            return this;
        }

        public Builder cacheTimeAfterWrite(long cacheTimeAfterWrite) {
            setCacheTimeAfterWrite(cacheTimeAfterWrite);
            return this;
        }

        public Builder addRegistry(String registry) {
            this.registries.add(registry);
            return this;
        }

        public Builder registries(List<String> registries) {
            setRegistries(registries);
            return this;
        }

        public Builder strategy(Strategies strategy) {
            this.strategy = strategy;
            return this;
        }

        public void setRegistries(List<String> registries) {
            this.registries.clear();
            this.registries.addAll(registries);
        }

        public Builder dockerTimeout(int dockerTimeout) {
            setDockerTimeout(dockerTimeout);
            return this;
        }

        public ClusterConfigImpl build() {
            return new ClusterConfigImpl(this);
        }
    }

    /**
     * List of docker/swarm 'host:port'
     * @return host
     */
    private final List<String> hosts;
    private final int maxCountOfInstances;
    private final String dockerRestart;
    private final String cluster;
    /**
     * Time in seconds, which data was cached after last write.
     */
    private final long cacheTimeAfterWrite;
    private final int dockerTimeout;
    /**
     * Name of registries
     * @return
     */
    private final List <String> registries;
    private final Strategies strategy;

    @JsonCreator
    public ClusterConfigImpl(Builder builder) {
        this.hosts = ImmutableList.copyOf(builder.hosts);
        this.maxCountOfInstances = builder.maxCountOfInstances;
        this.strategy = builder.strategy;
        this.dockerRestart = builder.dockerRestart;
        this.cluster = builder.cluster;
        this.cacheTimeAfterWrite = builder.cacheTimeAfterWrite;
        this.dockerTimeout = builder.dockerTimeout;
        this.registries = ImmutableList.copyOf(builder.registries);
    }

    /**
     * Check that config is valid.
     * @return this
     */
    public ClusterConfigImpl validate() {
        Assert.notEmpty(this.hosts, "Hosts is empty or null");
        return this;
    }

    public static Builder builder(ClusterConfig cc) {
        return builder().from(cc);
    }

    public static Builder builder() {
        return new Builder();
    }
}
