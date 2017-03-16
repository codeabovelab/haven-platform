/*
 * Copyright 2017 Code Above Lab LLC
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

package com.codeabovelab.dm.cluman.source;

import com.codeabovelab.dm.cluman.cluster.docker.model.Mount;
import com.codeabovelab.dm.cluman.cluster.docker.model.Network;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.*;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.common.utils.Sugar;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 */
@Data
@Slf4j
public class ServiceSourceConverter {
    private static final Splitter SP_HOSTS = Splitter.on(CharMatcher.anyOf(" \t"));

    /**
     * nodes group of specified service
     */
    private NodesGroup nodesGroup;
    /**
     * Service which will be converted to source
     */
    private Service.ServiceSpec serviceSpec;


    /**
     * Fill specified source object from service specification.
     * @param srv empty source of service
     */
    public void toSource(ServiceSource srv) {
        srv.setName(serviceSpec.getName());
        Sugar.setIfNotNull(srv.getLabels()::putAll, serviceSpec.getLabels());
        List<Endpoint.PortConfig> ports = serviceSpec.getEndpointSpec().getPorts();
        if(ports != null) {
            ports.forEach(pc -> {
                srv.getPorts().add(new Port(pc.getTargetPort(), pc.getPublishedPort(), pc.getProtocol(), pc.getPublishMode()));
            });
        }

        Task.TaskSpec taskSpec = serviceSpec.getTaskTemplate();
        ContainerSource cs = srv.getContainer();
        ContainerSpec conSpec = taskSpec.getContainer();
        toSource(conSpec, cs);
        Task.ResourceRequirements rrs = taskSpec.getResources();
        if(rrs != null) {
            TaskResources limits = rrs.getLimits();
            if(limits != null) {
                cs.setMemoryLimit(limits.getMemory());
                cs.setCpuQuota((int)(limits.getNanoCPUs() / 1000L));
            }
            TaskResources reserv = rrs.getReservations();
            if(reserv != null) {
                cs.setMemoryReservation(reserv.getMemory());
                cs.setCpuPeriod((int)(reserv.getNanoCPUs() / 1000L));
            }
        }
        Task.Placement placement = taskSpec.getPlacement();
        if(placement != null) {
            Sugar.setIfNotNull(srv.getConstraints()::addAll, placement.getConstraints());
        }

        List<SwarmNetwork.NetworkAttachmentConfig> netsSpecs = taskSpec.getNetworks();
        if(!CollectionUtils.isEmpty(netsSpecs)) {
            Map<String, Network> map = nodesGroup.getNetworks().getNetworks();
            resolveNetName(map, netsSpecs.get(0), cs::setNetwork);
            for(int i = 1; i < netsSpecs.size(); ++i) {
                resolveNetName(map, netsSpecs.get(i), cs.getNetworks()::add);
            }
        }
    }

    private void resolveNetName(Map<String, Network> map, SwarmNetwork.NetworkAttachmentConfig spec, Consumer<String> to) {
        String netId = spec.getTarget();
        Network network = map.get(netId);
        if(network != null) {
            to.accept(network.getName());
        } else {
            log.warn("Can not find network with id: {} for service: {}", netId, serviceSpec.getName());
        }
    }


    private void toSource(ContainerSpec conSpec, ContainerSource cs) {
        hostsToSource(conSpec.getHosts(), cs.getExtraHosts());
        ContainerSpec.DnsConfig dc = conSpec.getDnsConfig();
        if(dc != null) {
            Sugar.setIfNotNull(cs.getDnsSearch()::addAll, dc.getSearch());
            Sugar.setIfNotNull(cs.getDns()::addAll, dc.getServers());
        }
        List<Mount> mounts = conSpec.getMounts();
        if(mounts != null) {
            mounts.forEach(m -> {
                cs.getMounts().add(SourceUtil.toMountSource(m));
            });
        }
        String image = conSpec.getImage();
        ImageName in = ImageName.parse(image);
        cs.setImage(in.getFullName());
        cs.setImageId(in.getId());
        Sugar.setIfNotNull(cs.getLabels()::putAll, conSpec.getLabels());
        Sugar.setIfNotNull(cs.getEntrypoint()::addAll, conSpec.getCommand());
        Sugar.setIfNotNull(cs.getCommand()::addAll, conSpec.getArgs());
        Sugar.setIfNotNull(cs.getEnvironment()::addAll, conSpec.getEnv());
        cs.setHostname(conSpec.getHostname());
    }


    /**
     *
     * @param src lines of /etc/hosts file
     * @param dst pairs like 'name:ip'
     */
    private static void hostsToSource(List<String> src, List<String> dst) {
        if(src == null) {
            return;
        }
        src.forEach((hostLine) -> {
            // line in /etc/hosts file
            int sharpPos = hostLine.indexOf("#");
            if(sharpPos == 0) {
                // skip comments
                return;
            }
            String data = hostLine;
            if(sharpPos > 0) {
                data = hostLine.substring(0, sharpPos);
            }
            Iterator<String> i = SP_HOSTS.split(data).iterator();
            if(!i.hasNext()) {
                return;
            }
            String ip = i.next();
            while(i.hasNext()) {
                dst.add(i.next() + ":" + ip);
            }
        });
    }

}
