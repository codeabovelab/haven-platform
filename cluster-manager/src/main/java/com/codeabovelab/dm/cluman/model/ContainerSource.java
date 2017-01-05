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

import com.codeabovelab.dm.cluman.cluster.docker.management.argument.Reschedule;
import com.codeabovelab.dm.common.utils.Cloneables;
import com.codeabovelab.dm.common.utils.Comparables;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data which uses in creation of new container.
 */
@JsonPropertyOrder({"name", "hostname", "image", "cluster", "application", "node", "labels", "ports"})
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class ContainerSource extends EditableContainerSource implements Cloneable, ContainerBaseIface, Comparable<ContainerSource> {

    private String id;
    private String image;
    private String imageId;
    /**
     * Name of swarm in which container will be created
     */
    private String cluster;
    /**
     * Name of node. If node with this name is exists then container placed on it, otherwise containers with same
     * node name placed onto same node (node choosing algorithm currently is unspecified). When node is null, the node
     * will be chose by 'swarm scheduler filters'.
     */
    private String node;
    /**
     * Application name
     */
    private String application;
    @Setter(AccessLevel.NONE)
    private List<String> environment = new ArrayList<>();
    /**
     * include external files with common env variables for example
     */
    @Setter(AccessLevel.NONE)
    private List<String> include = new ArrayList<>();
    /**
     * List for define container volumes, in future. <p/>
     * The format is `container-dest[:<options>]`.
     * The comma-delimited `options` are [rw|ro], [z|Z], [[r]shared|[r]slave|[r]private], and [nocopy]. <p/>
     * <a href="https://docs.docker.com/engine/tutorials/dockervolumes/">See docs.docker.com</a>
     */
    @Setter(AccessLevel.NONE)
    private List<String> volumes = new ArrayList<>();
    /**
     * Binds of external (node paths or named volumes to container). <p/>
     * The format is `host-src:container-dest[:<options>]`.
     * The comma-delimited `options` are [rw|ro], [z|Z], [[r]shared|[r]slave|[r]private], and [nocopy].
     * The 'host-src' is an absolute path or a name value.<p/>
     * <a href="https://docs.docker.com/engine/tutorials/dockervolumes/">See docs.docker.com</a>
     */
    @Setter(AccessLevel.NONE)
    private List<String> volumeBinds = new ArrayList<>();
    /**
     *
     */
    private String volumeDriver;
    /**
     * List of entris like <code>container:['rw'|'ro']</code> which is used as volume source
     */
    @Setter(AccessLevel.NONE)
    private List<String> volumesFrom = new ArrayList<>();
    /**
     * Name must comply with requirements for hostname. Simply it allow only 'a'-'z' chars and '-'.
     */
    private String name;
    private String hostname;
    private String domainname;
    @Setter(AccessLevel.NONE)
    @JsonPropertyOrder(alphabetic = true)
    private Map<String, String> ports = new HashMap<>();
    @Setter(AccessLevel.NONE)
    @JsonPropertyOrder(alphabetic = true)
    private Map<String, String> labels = new HashMap<>();
    private Reschedule reschedule;
    private boolean publishAllPorts;
    @Setter(AccessLevel.NONE)
    @JsonPropertyOrder(alphabetic = true)
    private Map<String, String> links = new HashMap<>();
    @Setter(AccessLevel.NONE)
    private List<String> command = new ArrayList<>();
    private List<String> entrypoint = new ArrayList<>();
    /**
     * List of dependencies, note that deps may be in reverse order (mean that specified
     * container must be depended from this). Also, system consider dependencies placed as {@link #links }
     * TODO we need to split it to url, container, and reverse container deps (we need that current object must be flat)
     */
    // private final List<ContainerDependency> deps = new ArrayList<>();

    /**
     * List of dns servers for container, when null container will use it from host settings.
     */
    @Setter(AccessLevel.NONE)
    private List<String> dns = new ArrayList<>();
    /**
     * List of dns domains when container, when null container will use it from host settings.
     */
    @Setter(AccessLevel.NONE)
    private List<String> dnsSearch = new ArrayList<>();
    /**
     * Sets the networking mode for the container. <p/>
     * Supported standard values are: bridge, host, none, and container:&lt;name|id&gt;. <p/>
     * Any other value is taken as a custom network’s name to which this container should connect to.
     */
    private String network;
    /**
     * List of networks on which is container connected
     */
    @Setter(AccessLevel.NONE)
    private List<String> networks = new ArrayList<>();
    /**
     * A list of hostname -> IP mappings to add to the container’s /etc/hosts file. <p/>
     * Specified in the form "hostname:IP".
     */
    @Setter(AccessLevel.NONE)
    private List<String> extraHosts =  new ArrayList<>();
    /**
     * A list of string values to customize labels for MLS systems, such as SELinux.
     */
    @Setter(AccessLevel.NONE)
    private List<String> securityOpt =  new ArrayList<>();

    @Override
    public ContainerSource clone() {
        ContainerSource clone = (ContainerSource) super.clone();
        clone.command = Cloneables.clone(clone.command);
        clone.environment = Cloneables.clone(clone.environment);
        clone.volumes = Cloneables.clone(clone.volumes);
        clone.volumeBinds = Cloneables.clone(clone.volumeBinds);
        clone.volumesFrom = Cloneables.clone(clone.volumesFrom);
        clone.ports = Cloneables.clone(clone.ports);
        clone.labels = Cloneables.clone(clone.labels);
        clone.links = Cloneables.clone(clone.links);
        clone.dns = Cloneables.clone(clone.dns);
        clone.entrypoint = Cloneables.clone(clone.entrypoint);
        clone.dnsSearch = Cloneables.clone(clone.dnsSearch);
        clone.networks = Cloneables.clone(clone.networks);
        clone.extraHosts = Cloneables.clone(clone.extraHosts);
        clone.securityOpt = Cloneables.clone(clone.securityOpt);
        clone.include = Cloneables.clone(clone.include);
        return clone;
    }

    @Override
    public int compareTo(ContainerSource o) {
        return Comparables.compare(this.name, o.name);
    }


}
