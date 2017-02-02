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

package com.codeabovelab.dm.cluman.ui.model;

import com.codeabovelab.dm.cluman.cluster.docker.model.Network;
import com.codeabovelab.dm.cluman.ds.container.ContainerRegistration;
import com.codeabovelab.dm.cluman.ds.container.ContainerStorage;
import com.codeabovelab.dm.cluman.model.DockerContainer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
@Data
public class UiNetwork {
    private String id;
    private String name;
    private String cluster;
    private Network.Scope scope;
    private String driver;
    private Ipam ipam;
    private final List<Container> containers = new ArrayList<>();

    @Data
    public static class Ipam {

        private String driver;

        private final List<IpamConfig> configs = new ArrayList<>();

    }

    @Data
    public static class IpamConfig {

        private String subnet;

        private String range;

        private String gateway;
    }

    @Data
    public static class Container {

        private String id;

        private String name;

        private String image;

        private String ipv4Address;

        private String ipv6Address;

    }


    public void from(Network network, ContainerStorage cs) {
        this.setId(network.getId());
        this.setName(network.getName());
        this.setScope(network.getScope());
        this.setDriver(network.getDriver());
        Network.Ipam origIpam = network.getIpam();
        Ipam uiIpam = new Ipam();
        uiIpam.setDriver(origIpam.getDriver());
        List<Network.IpamConfig> configs = origIpam.getConfigs();
        if(configs != null) {
            configs.forEach(oc -> {
                IpamConfig uic = new IpamConfig();
                uic.setGateway(oc.getGateway());
                uic.setRange(oc.getIpRange());
                uic.setSubnet(oc.getSubnet());
                uiIpam.getConfigs().add(uic);
            });
        }
        Map<String, Network.EndpointResource> containers = network.getContainers();
        if(cs != null && containers != null) {
            containers.forEach((key, val) -> {
                ContainerRegistration cr = cs.getContainer(key);
                // not any endpoint is a container!
                if(cr == null) {
                    return;
                }
                Container c = new Container();
                c.setId(val.getEndpointId());
                DockerContainer dc = cr.getContainer();
                c.setName(dc.getName());
                c.setImage(dc.getImage());
                c.setIpv4Address(val.getIpv4Address());
                c.setIpv6Address(val.getIpv6Address());
                getContainers().add(c);
            });
        }
        this.setIpam(uiIpam);
    }

}
