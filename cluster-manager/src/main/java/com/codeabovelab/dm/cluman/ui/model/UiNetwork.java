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
import lombok.EqualsAndHashCode;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UiNetwork extends UiNetworkBase {
    private String id;
    private Network.Scope scope;
    private final List<Container> containers = new ArrayList<>();
    private boolean attachable;

    @Data
    public static class Container {

        private String id;

        private String name;

        private String image;

        private String ipv4Address;

        private String ipv6Address;

    }


    public UiNetwork from(Network network, ContainerStorage cs) {
        super.from(network);
        this.setId(network.getId());
        this.setScope(network.getScope());
        this.setAttachable(isAttachable());

        Map<String, Network.EndpointResource> containers = network.getContainers();
        if(cs != null && containers != null) {
            containers.forEach((key, val) -> {
                ContainerRegistration cr = cs.getContainer(key);
                // not any endpoint is a container!
                if(cr == null) {
                    return;
                }
                Container c = new Container();
                c.setId(cr.getId());
                DockerContainer dc = cr.getContainer();
                c.setName(dc.getName());
                c.setImage(dc.getImage());
                c.setIpv4Address(val.getIpv4Address());
                String ipv6Address = val.getIpv6Address();
                if(StringUtils.hasText(ipv6Address)) {
                    c.setIpv6Address(ipv6Address);
                }
                getContainers().add(c);
            });
        }
        return this;
    }

}
