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

package com.codeabovelab.dm.cluman.ui;

import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfig;
import com.codeabovelab.dm.cluman.cluster.docker.management.ApplicationService;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.*;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.CreateAndStartContainerResult;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ProcessEvent;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerDetails;
import com.codeabovelab.dm.cluman.cluster.docker.model.Statistics;
import com.codeabovelab.dm.cluman.cluster.docker.model.UpdateContainerCmd;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryRepository;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryService;
import com.codeabovelab.dm.cluman.configs.container.ConfigProvider;
import com.codeabovelab.dm.cluman.ds.DockerServiceRegistry;
import com.codeabovelab.dm.cluman.ds.container.ContainerManager;
import com.codeabovelab.dm.cluman.ds.container.ContainerRegistration;
import com.codeabovelab.dm.cluman.ds.container.ContainerStorage;
import com.codeabovelab.dm.cluman.ds.container.ContainersNameService;
import com.codeabovelab.dm.cluman.ds.nodes.NodeRegistration;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.ds.swarm.DockerServices;
import com.codeabovelab.dm.cluman.model.Application;
import com.codeabovelab.dm.cluman.model.ImageDescriptor;
import com.codeabovelab.dm.cluman.model.NodeInfo;
import com.codeabovelab.dm.cluman.model.ContainerSource;
import com.codeabovelab.dm.cluman.source.ContainerSourceFactory;
import com.codeabovelab.dm.cluman.ui.model.*;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.codeabovelab.dm.common.cache.DefineCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.SettableFuture;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * Container pPart of cluster API.
 */
@RestController
@RequestMapping(value = "/ui/api", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ContainerApi {

    private final ObjectMapper objectMapper;
    private final DockerServiceRegistry dockerServiceRegistry;
    private final RegistryRepository registryRepository;
    private final ContainersNameService containersNameService;
    private final ContainerManager containerManager;
    private final ConfigProvider configProvider;
    private final DockerServices dockerServices;
    private final ContainerStorage containerStorage;
    private final NodeStorage nodeStorage;
    private final ApplicationService applicationService;
    private final ContainerSourceFactory containerSourceFactory;

    @RequestMapping(value = "/containers/{id}/stop", method = RequestMethod.POST)
    public ResponseEntity<?> stopContainer(@PathVariable("id") String id) {
        StopContainerArg arg = StopContainerArg.builder().id(id).build();
        DockerService service = getService(id);
        ServiceCallResult res = service.stopContainer(arg);
        return UiUtils.createResponse(res);
    }

    @Deprecated
    @RequestMapping(value = "/containers/{id}/refresh", method = RequestMethod.POST)
    public ResponseEntity<?> refreshContainer(@PathVariable("id") String id) {
        StopContainerArg arg = StopContainerArg.builder().id(id).build();
        DockerService service = getService(id);
        ServiceCallResult resStop = service.stopContainer(arg);
        log.info("resStop {}", resStop);
        ServiceCallResult resStart = service.startContainer(id);
        return UiUtils.createResponse(resStart);
    }

    @RequestMapping(value = "/containers/{id}/remove", method = RequestMethod.POST)
    public ResponseEntity<?> removeContainer(@PathVariable("id") String id) {
        DockerService service = getService(id);
        service.stopContainer(StopContainerArg.builder().id(id).build());
        DeleteContainerArg arg = DeleteContainerArg.builder().id(id).build();
        ServiceCallResult res = service.deleteContainer(arg);
        return UiUtils.createResponse(res);
    }

    @RequestMapping(value = "/containers/{id}/start", method = RequestMethod.POST)
    public ResponseEntity<?> startContainer(@PathVariable("id") String id) {
        DockerService service = getService(id);
        ServiceCallResult res = service.startContainer(id);
        return UiUtils.createResponse(res);
    }

    @RequestMapping(value = "/containers/", method = RequestMethod.GET)
    public List<UiContainer> getAll() {
        List<ContainerRegistration> crs = containerStorage.getContainers();
        Map<String, String> app2cont = UiUtils.mapAppContainer(applicationService, null);
        List<UiContainer> containers = crs.stream().map((cr) -> {
            UiContainer uc = UiContainer.fromBase(new UiContainer(), cr.getContainer());
            uc.setNode(cr.getNode());
            uc.getLabels().putAll(cr.getAdditionalLabels());
            uc.setCluster(getClusterForNode(cr.getNode()));
            uc.setApplication(app2cont.get(uc.getId()));
            return uc;
        }).collect(Collectors.toList());
        containers.sort(null);
        return containers;
    }



    @RequestMapping(value = "/containers/{id}/details", method = RequestMethod.GET)
    @DefineCache(expireAfterWrite = Integer.MAX_VALUE)
    public UIContainerDetails getDetails(@PathVariable("id") String id) {
        log.info("got getDetails request id: {}", id);
        ContainerRegistration cr = containerStorage.getContainer(id);
        ExtendedAssert.notFound(cr, "Not found container: " + id);
        String node = cr.getNode();
        DockerService nodeService = dockerServices.getNodeService(node);
        ContainerDetails container = nodeService.getContainer(id);
        return toContainerDetails(cr, container);
    }

    private UIContainerDetails toContainerDetails(ContainerRegistration cr, ContainerDetails container) {
        String node = cr.getNode();
        String id = cr.getId();
        UIContainerDetails res = UIContainerDetails.from(containerSourceFactory, container);
        res.setNode(node);
        String cluster = getClusterForNode(node);
        res.setCluster(cluster);
        if(cluster != null) {
            List<Application> apps = applicationService.getApplications(cluster);
            for(Application app: apps) {
                if(app.getContainers().contains(id)) {
                    res.setApplication(app.getName());
                    break;
                }
            }
        }
        Map<String, String> additionalLabels = cr.getAdditionalLabels();
        if(additionalLabels != null) {
            res.getLabels().putAll(additionalLabels);
        }
        return res;
    }

    @RequestMapping(value = "/containers/{id}/statistics", method = RequestMethod.GET)
    @Cacheable("UIStatistics")
    @SuppressWarnings("unchecked")
    public UIStatistics getStatistics(@PathVariable("id") String id) throws Exception {
        DockerService service = getService(id);
        log.info("got getStatistics request id: {}", id);
        GetStatisticsArg.Builder argb = GetStatisticsArg.builder();
        argb.id(id);
        argb.stream(false);
        SettableFuture<Statistics> holder = SettableFuture.create();
        argb.watcher(holder::set);
        service.getStatistics(argb.build());
        Statistics statistics = holder.get();
        return UIStatistics.from(statistics);
    }

    private DockerService getService(String id) {
        DockerService service = dockerServices.getServiceByContainer(id);
        ExtendedAssert.notFound(service, "Can not find container: " + id);
        return service;
    }

    @RequestMapping(value = "/containers/{id}/restart", method = RequestMethod.POST)
    public ResponseEntity<?> restartContainer(@PathVariable("id") String id) {
        ServiceCallResult res = getService(id)
                .restartContainer(StopContainerArg.builder().id(id).build());
        return UiUtils.createResponse(res);
    }


    @RequestMapping(value = "/clusters/{cluster}/defaultparams/{image}/{tag}/", method = GET)
    public ContainerSource defaultParams(@PathVariable("cluster") String cluster,
                                         @PathVariable("image") String image,
                                         @PathVariable("tag") String tag,
                                         @RequestParam(value = "registry", required = false) String registry) throws Exception {
        RegistryService regisrty = registryRepository.getRegistry(registry);
        String id = regisrty.getConfig().getName() + "/" + image + ":" + tag;
        DockerService dockerService = dockerServiceRegistry.getService(cluster);
        ImageDescriptor img = regisrty.getImage(image, tag);
        log.info("image info {}", img);
        ContainerSource res = configProvider.resolveProperties(cluster, img, id, new ContainerSource());

        res.setName(containersNameService.calculateName(CalcNameArg.builder()
                .allocate(false)
                .containerName(res.getName())
                .imageName(image)
                .dockerService(dockerService)
                .build()));

        ClusterConfig clusterConfig = dockerService.getClusterConfig();
        if (!StringUtils.hasText(res.getRestart())) {
            res.setRestart(clusterConfig.getDockerRestart());
        }

        return res;
    }


    @RequestMapping(value = "/containers/{id}/logs", method = RequestMethod.GET)
    public void getContainerLog(@PathVariable("id") String id,
                                @RequestParam(value = "stdout", defaultValue = "true", required = false) boolean stdout,
                                @RequestParam(value = "stderr", defaultValue = "true", required = false) boolean stderr,
                                @RequestParam(value = "follow", defaultValue = "false", required = false) boolean follow,
                                @RequestParam(value = "timestamps", defaultValue = "true", required = false) boolean timestamps,
                                @RequestParam(value = "since", required = false) Date since,
                                @RequestParam(value = "tail", defaultValue = "200") Integer tail,
                                final HttpServletResponse response) throws IOException {

        DockerService service = getService(id);
        try (final ServletOutputStream writer = response.getOutputStream()) {
            GetLogContainerArg arg = GetLogContainerArg.builder()
                    .id(id)
                    .tail(tail)
                    .follow(follow)
                    .stdout(stdout)
                    .stderr(stderr)
                    .timestamps(timestamps)
                    .since(since)
                    .watcher(processEvent -> {
                        // we use '\n' as delimiter for log formatter in js
                        try {
                            writer.println(processEvent.getMessage());
                            writer.flush();
                        } catch (IOException e) {
                            log.error("", e);
                        }
                    }).build();
            ServiceCallResult res = service.getContainerLog(arg);
            objectMapper.writeValue(writer, res);
        }
    }

    @RequestMapping(value = "/containers/create", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void createContainer(@RequestBody ContainerSource container, final HttpServletResponse response) throws Exception {
        String node = container.getNode();
        String cluster = container.getCluster();
        if(node != null) {
            NodeInfo nodeInfo = nodeStorage.getNodeInfo(node);
            String nodeCluster = nodeInfo.getCluster();
            if(!Objects.equals(nodeCluster, cluster)) {
                if(cluster != null) {
                    // when cluster is null we simply use node cluster
                    log.info("Node has different cluster '{}' than specified '{}', we use node cluster.", nodeCluster, cluster);
                }
                cluster = nodeCluster;
            }
        }
        log.info("got create request container request at cluster: {} : {}", cluster, container);
        CreateContainerArg arg = new CreateContainerArg();
        arg.setContainer(container);

        try (final ServletOutputStream writer = response.getOutputStream()) {
            arg.setWatcher(processEvent -> {
                // we use '\n' as delimiter for log formatter in js
                try {
                    writer.println(processEvent.getMessage());
                    writer.flush();
                } catch (IOException e) {
                    log.error("", e);
                }
            });
            try {
                ProcessEvent.watch(arg.getWatcher(), "Creating container with params: {0}", container);
                CreateAndStartContainerResult res = containerManager.createContainer(arg);
                ProcessEvent.watch(arg.getWatcher(), "Finished with {0}", res.getCode());
                objectMapper.writeValue(writer, res);
            } catch (Exception e) {
                log.error("Error during creating", e);
                objectMapper.writeValue(writer, new ServiceCallResult()
                        .code(ResultCode.ERROR)
                        .message(e.getMessage()));
                return;
            }
        }
    }

    @RequestMapping(value = "/containers/{id}/updateLabels", method = RequestMethod.PUT)
    public void updateLabels(@PathVariable("id") String containerId,
                             Map<String, String> additionalLabels) throws Exception {

        ContainerRegistration container = containerStorage.getContainer(containerId);
        ExtendedAssert.notFound(container, "Container not found by id " + containerId);
        container.setAdditionalLabels(additionalLabels);
        container.flush();
    }

    @ApiOperation("this method allows to get container's id by name and cluster")
    @RequestMapping(value = "/containers/{cluster}/{name}", method = RequestMethod.GET)
    public UIContainerDetails getContainerDetailsByName(@PathVariable("cluster") String cluster, @PathVariable("name") String name)
            throws Exception {
        ContainerRegistration cr = containerStorage.findContainer(name);
        ExtendedAssert.notFound(cr, "Can't find container by name " + name);
        DockerService service = dockerServices.getService(cluster);
        ExtendedAssert.notFound(service, "Can't find cluster by id " + cluster);
        ContainerDetails container = service.getContainer(name);
        ExtendedAssert.notFound(container, "Can't find container by id " + container + " in cluster " + cluster);
        return toContainerDetails(cr, container);
    }

    @RequestMapping(value = "/containers/{id}/update", method = RequestMethod.PUT)
    public ResponseEntity<?> updateContainer(@PathVariable("id") String containerId,
                                             @RequestBody UiUpdateContainer container) throws Exception {
        String cluster = getClusterForContainer(containerId);
        log.info("Begin update container '{}' at cluster: '{}' request: '{}'", containerId, cluster, container);
        UpdateContainerCmd cmd = new UpdateContainerCmd();
        cmd.setId(containerId);
        cmd.setBlkioWeight(container.getBlkioWeight());
        cmd.setCpuPeriod(container.getCpuPeriod());
        cmd.setCpuQuota(container.getCpuQuota());
        cmd.setCpuShares(container.getCpuShares());
        cmd.setCpusetCpus(container.getCpusetCpus());
        cmd.setCpusetMems(container.getCpusetMems());
        cmd.setKernelMemory(container.getKernelMemory());
        cmd.setMemory(container.getMemoryLimit());
        cmd.setMemoryReservation(container.getMemoryReservation());
        cmd.setMemorySwap(container.getMemorySwap());
        DockerService service = dockerServiceRegistry.getService(cluster);
        ServiceCallResult res = service.updateContainer(cmd);
        log.info("Begin update container '{}' at cluster: '{}' result: '{}'", containerId, cluster, res);
        return UiUtils.createResponse(res);
    }

    private String getClusterForContainer(String containerId) {
        ContainerRegistration container = containerStorage.getContainer(containerId);
        ExtendedAssert.notFound(container, "Container \"" + containerId + "\" is not found.");
        String node = container.getNode();
        String cluster = getClusterForNode(node);
        ExtendedAssert.badRequest(cluster != null,
                "Container \"{0}\" is placed on node \"{1}\" which is not included to cluster.", containerId, node);
        return cluster;
    }

    /**
     * Return cluster of null
     * @param node
     * @return
     */
    private String getClusterForNode(String node) {
        NodeRegistration nodeReg = this.nodeStorage.getNodeRegistration(node);
        //below is not an 404, because above we found container with link to node, but cannot give existed node
        Assert.notNull(nodeReg, "Node \"" + node + "\" has invalid registration.");
        String cluster = nodeReg.getNodeInfo().getCluster();
        return cluster;
    }

    @RequestMapping(value = "/containers/{id}/rename", method = RequestMethod.PUT)
    public ResponseEntity<?> rename(@PathVariable("id") String id,
                                    @RequestParam(value = "newName") String newName) {
        log.info("got rename request id: {}, name: {}", id, newName);
        DockerService service = getService(id);
        ServiceCallResult res = service.renameContainer(id, newName);
        return UiUtils.createResponse(res);
    }

    @RequestMapping(value = "/containers/{id}/scale", method = RequestMethod.POST)
    public ResponseEntity<?> scale(@PathVariable("id") String id,
                                   @RequestParam(value = "scaleFactor", required = false, defaultValue = "1") Integer scaleFactor) {
        log.info("got scale request id: {}, count {}", id, scaleFactor);
        String cluster = getClusterForContainer(id);
        ServiceCallResult res = containerManager.scale(cluster, scaleFactor, id);
        return UiUtils.createResponse(res);
    }

}
