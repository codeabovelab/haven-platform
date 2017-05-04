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

package com.codeabovelab.dm.cluman.ui;

import com.codeabovelab.dm.cluman.cluster.docker.management.argument.CreateServiceArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.UpdateServiceArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.AuthConfig;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Service;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryRepository;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryService;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryCredentials;
import com.codeabovelab.dm.cluman.model.ContainerService;
import com.codeabovelab.dm.cluman.model.ContainersManager;
import com.codeabovelab.dm.cluman.model.DiscoveryStorage;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import com.codeabovelab.dm.cluman.ui.model.UiContainerService;
import com.codeabovelab.dm.cluman.ui.model.UiContainerServiceCreate;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.google.common.base.MoreObjects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;


/**
 */
@RestController
@RequestMapping(value = "/ui/api/services/", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ServiceApi {

    private final DiscoveryStorage ds;
    private final RegistryRepository registryRepository;

    private NodesGroup getNodesGroup(String cluster) {
        ExtendedAssert.badRequest(cluster != null, "Cluster name is null");
        NodesGroup ng = ds.getCluster(cluster);
        ExtendedAssert.notFound(ng, "can not find cluster: " + cluster);
        return ng;
    }

    @RequestMapping(path = "/get", method = RequestMethod.GET)
    public UiContainerService get(@RequestParam(name = "id") String id,
                                  @RequestParam(name = "cluster") String cluster) {
        NodesGroup ng = getNodesGroup(cluster);
        ContainerService service = ng.getContainers().getService(id);
        ExtendedAssert.notFound(service, "can not find service: " + id);
        return UiContainerService.from(ng, service);
    }

    @RequestMapping(path = "/create", method = RequestMethod.POST)
    public ResponseEntity<?> create(@RequestBody UiContainerServiceCreate body) {
        NodesGroup ng = getNodesGroup(body.getCluster());
        CreateServiceArg arg = new CreateServiceArg();
        arg.setSpec(body.toServiceSpec().build());
        arg.setRegistryAuth(getRegistryAuth(body));
        ServiceCallResult res = ng.getContainers().createService(arg);
        return UiUtils.createResponse(res);
    }

    @RequestMapping(path = "/update", method = RequestMethod.POST)
    public ResponseEntity<?> update(@RequestBody @Valid UiContainerServiceCreate body) {
        NodesGroup ng = getNodesGroup(body.getCluster());
        UpdateServiceArg arg = new UpdateServiceArg();
        arg.setService(MoreObjects.firstNonNull(body.getId(), body.getName()));
        arg.setVersion(body.getVersion());
        arg.setSpec(body.toServiceSpec().build());
        arg.setRegistryAuth(getRegistryAuth(body));
        ServiceCallResult res = ng.getContainers().updateService(arg);
        return UiUtils.createResponse(res);
    }

    @RequestMapping(path = "/scale", method = RequestMethod.POST)
    public ResponseEntity<?> scale(@RequestParam(name = "cluster") String cluster,
                                    @RequestParam(name = "id") String id,
                                    @RequestParam(name = "scale") int scale) {
        NodesGroup ng = getNodesGroup(cluster);
        ContainersManager cm = ng.getContainers();
        ContainerService cs = cm.getService(id);
        UpdateServiceArg arg = new UpdateServiceArg();
        arg.setService(id);
        Service service = cs.getService();
        arg.setVersion(service.getVersion().getIndex());
        Service.ServiceSpec spec = service.getSpec();
        Service.ServiceMode mode = spec.getMode();
        Service.ReplicatedService replicated = mode.getReplicated();
        if(replicated == null) {
            throw new IllegalArgumentException("Service " + id + " is not a replicated.");
        }
        ServiceCallResult res = ServiceCallResult.unmodified();
        if(replicated.getReplicas() != scale) {
            arg.setSpec(spec.toBuilder().mode(mode.toBuilder()
                .replicated(new Service.ReplicatedService(scale))
              .build()).build());
            res = cm.updateService(arg);
        }
        return UiUtils.createResponse(res);
    }


    @RequestMapping(path = "/delete", method = RequestMethod.POST)
    public ResponseEntity<?> delete(@RequestParam(name = "id") String id,
                                    @RequestParam(name = "cluster") String cluster) {
        NodesGroup ng = getNodesGroup(cluster);
        ServiceCallResult res = ng.getContainers().deleteService(id);
        return UiUtils.createResponse(res);
    }

    private AuthConfig getRegistryAuth(UiContainerServiceCreate body) {
        RegistryService registry = registryRepository.getRegistryByImageName(body.getContainer().getImage());
        if(registry == null) {
            return null;
        }
        RegistryCredentials credentials = registry.getCredentials();
        if(credentials == null) {
            return null;
        }
        String username = credentials.getUsername();
        if(username == null) {
            return null;
        }
        return AuthConfig.builder()
          .username(username)
          .password(credentials.getPassword())
          .build();
    }
}
