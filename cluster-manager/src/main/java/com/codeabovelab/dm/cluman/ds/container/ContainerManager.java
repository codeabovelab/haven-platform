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

package com.codeabovelab.dm.cluman.ds.container;

import com.codeabovelab.dm.cluman.ContainerUtils;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerServiceImpl;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerUtils;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.CalcNameArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.CreateContainerArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.CreateAndStartContainerResult;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ProcessEvent;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.*;
import com.codeabovelab.dm.cluman.configs.container.ConfigProvider;
import com.codeabovelab.dm.cluman.ds.DockerServiceRegistry;
import com.codeabovelab.dm.cluman.ds.SwarmUtils;
import com.codeabovelab.dm.cluman.ds.swarm.NetworkManager;
import com.codeabovelab.dm.cluman.model.ImageDescriptor;
import com.codeabovelab.dm.cluman.model.ContainerSource;
import com.codeabovelab.dm.cluman.model.NodeInfo;
import com.codeabovelab.dm.cluman.model.NodeRegistry;
import com.codeabovelab.dm.cluman.source.ContainerSourceFactory;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.codeabovelab.dm.common.utils.Consumers;
import com.google.common.base.MoreObjects;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Consumer;
import static com.codeabovelab.dm.cluman.cluster.docker.management.DockerUtils.SCALABLE;
import static com.codeabovelab.dm.cluman.cluster.docker.model.RestartPolicy.parse;
import static com.google.common.base.MoreObjects.firstNonNull;

/**
 * Manager for containers, it use DockerService as backend, but do something things
 * which is not provided by docker out of box.
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ContainerManager {
    private class CreateContainerContext {
        final CreateContainerArg arg;
        final Consumer<ProcessEvent> watcher;
        /**
         * Service instance of concrete node or cluster on which do creation .
         */
        final DockerService dockerService;
        private String name;

        CreateContainerContext(CreateContainerArg arg, DockerService service) {
            this.arg = arg;
            Assert.notNull(arg.getContainer(), "arg.container is null");
            this.watcher = firstNonNull(arg.getWatcher(), Consumers.<ProcessEvent>nop());
            if (service != null) {
                this.dockerService = service;
            } else {
                this.dockerService = getDocker(arg);
            }
        }

        private DockerService getDocker(CreateContainerArg arg) {
            //we create container only on node, ignore swarm and virtual service
            String node = arg.getContainer().getNode();
            Assert.hasText(node, "Node is null or empty");
            DockerService service = nodeRegistry.getNodeService(node);
            Assert.notNull(service, "Can not fins service for node: " + node);
            return service;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(DockerServiceImpl.class);
    private static final int CREATE_CONTAINER_TRIES = 3;
    private final DockerServiceRegistry dockerServiceRegistry;
    private final NodeRegistry nodeRegistry;
    private final ConfigProvider configProvider;
    private final ContainersNameService containersNameService;
    private final ContainerStorage containerStorage;
    private final NetworkManager networkManager;
    private final ContainerSourceFactory containerSourceFactory;

    /**
     * Create container by image information (image name, tag) also can be specified optional params <p/>
     * <b>Must not throw any exception after start creation of container.</b>
     * @param arg
     * @return id of new container
     */
    public CreateAndStartContainerResult createContainer(CreateContainerArg arg) {
        LOG.info("CreateContainerArg: {}", arg);

        DockerService docker;
        ContainerSource container = arg.getContainer();
        String cluster = container.getCluster();
        if(cluster != null) {
            docker = getDockerForCluster(cluster);
        } else {
            String node = container.getNode();
            if(node != null) {
                docker = nodeRegistry.getNodeService(node);
            } else {
                throw new IllegalArgumentException("Cluster and node is null.");
            }
        }
        CreateContainerContext cc = new CreateContainerContext(arg, docker);
        return createContainerInternal(cc);
    }

    private CreateAndStartContainerResult createContainerInternal(CreateContainerContext cc) {
        CreateAndStartContainerResult result = new CreateAndStartContainerResult();
        try {
            CreateContainerResponse response = createWithTries(cc);
            final String containerId = response.getId();
            result.setContainerId(containerId);
            ServiceCallResult startRes = cc.dockerService.startContainer(containerId);
            ResultCode code = startRes.getCode();
            if (code != ResultCode.OK) {
                LOG.error("Start container '{}' was failed.", startRes.getMessage());
            }
            result.setCode(code);
            result.setMessage(startRes.getMessage());
            if (result.getContainerId() != null) {
                ContainerDetails container = cc.dockerService.getContainer(containerId);
                if (container != null) {
                    String node = container.getNode() == null ? null : container.getNode().getName();
                    if(node == null) {
                        LOG.error("Container '{}' has null node.", containerId);
                    }
                    container.setName(ContainerUtils.fixContainerName(container.getName()));
                    ContainerRegistration orCreateContainer = containerStorage.getOrCreateContainer(container, node);
                    orCreateContainer.setAdditionalLabels(cc.arg.getContainer().getLabels());
                } else {
                    LOG.error("Can't receive container '{}' from service.", containerId);
                }
            }
        } catch (Exception e) {
            LOG.error("Can't create container", e);
            result.setCode(ResultCode.ERROR);
            result.setMessage(e.getMessage());
        }
        // name previously set in this.buildCreateContainer(),
        //   and we need name in for cleanup container if this will be failed at start
        result.setName(cc.getName());
        return result;
    }

    // we try create container, if we got conflict, then try again with other name
    private CreateContainerResponse createWithTries(CreateContainerContext cc) {

        int tries = CREATE_CONTAINER_TRIES;
        CreateContainerResponse response = null;
        while (tries > 0) {
            tries--;
            response = doCreation(cc);
            if (response.getCode() == ResultCode.OK) {
                return response;
            }
            boolean weCanTryAgain = response.getCode() == ResultCode.CONFLICT &&
                    !StringUtils.hasText(cc.arg.getContainer().getName());
            if (!weCanTryAgain) {
                break;
            }
        }
        if (response == null || ResultCode.OK != response.getCode()) {
            throw new IllegalStateException("Can't create container, due: " + response.getCode() + " " + response.getMessage());
        }
        return response;
    }

    /**
     * add scalable to doc
     * @param clusterId
     * @param scaleFactor
     * @param id
     * @return
     */
    public ServiceCallResult scale(String clusterId, Integer scaleFactor, String id) {
        DockerService docker = getDockerForCluster(clusterId);
        ContainerDetails container = docker.getContainer(id);
        ExtendedAssert.notFound(container, "Can not find container: " + id);
        String scalable = container.getConfig().getLabels().get(SCALABLE);
        if (scalable == null || "true".equals(scalable)) {
            int scale = scaleFactor == null ? 1 : scaleFactor;
            for (int i = 0; i < scale; i++) {
                ContainerSource nc = new ContainerSource();
                containerSourceFactory.toSource(container, nc);
                nc.setCluster(clusterId);
                nc.setNode(null);
                nc.setName(null);
                nc.setHostname(null);
                nc.setDomainname(null);
                SwarmUtils.clearLabels(nc.getLabels());
                CreateContainerArg arg = new CreateContainerArg();
                arg.setContainer(nc);
                CreateContainerContext cc = new CreateContainerContext(arg, docker);
                CreateAndStartContainerResult containerInternal = createContainerInternal(cc);
                if (containerInternal.getCode() == ResultCode.ERROR) {
                    return containerInternal;
                }
            }
            return new ServiceCallResult()
                    .code(ResultCode.OK).message("Created " + scale + " instances");
        } else {
            return new ServiceCallResult()
                    .code(ResultCode.ERROR)
                    .message("Image not scalable " + container.getConfig().getImage());
        }
    }

    private CreateContainerResponse doCreation(CreateContainerContext cc) {
        CreateContainerCmd cmd = buildCreateContainer(cc);
        CreateContainerResponse response = cc.dockerService.createContainer(cmd);
        ProcessEvent.watch(cc.watcher, "Result of execution: {0}", response);
        return response;
    }

    protected CreateContainerCmd buildCreateContainer(CreateContainerContext cc) {
        DockerService dockerService = cc.dockerService;
        ContainerSource nc = cc.arg.getContainer();
        String imageName = nc.getImage();
        ImageDescriptor image = dockerService.pullImage(imageName, cc.watcher);
        ContainerSource result = configProvider.resolveProperties(nc.getCluster(), image, imageName, nc);
        Map<String, Integer> appCountPerNode = getContainersPerNodeForImage(cc, imageName);
        List<String> existsNodes = DockerUtils.listNodes(dockerService.getInfo());
        // we want to save order of entries, but skip duplicates
        LinkedHashSet<String> env = new LinkedHashSet<>();
        env.addAll(result.getEnvironment());
        ContainerStarterHelper.calculateConstraints(existsNodes,
          result.getNode(),
          appCountPerNode,
          dockerService.getClusterConfig().getMaxCountOfInstances(), env);
        LOG.info("Env: {}", env);
        ProcessEvent.watch(cc.watcher, "Environment: {0}", env);

        String name = containersNameService.calculateName(CalcNameArg.builder()
                .allocate(true)
                .containerName(result.getName())
                .imageName(imageName)
                .dockerService(dockerService).build());
        cc.setName(name);
        ProcessEvent.watch(cc.watcher, "Calculated name of the container: {0}", name);
        LOG.info("Calculated name of the container {}", name);

        CreateContainerCmd cmd = new CreateContainerCmd();
        cmd.setName(name);
        cmd.setHostName(MoreObjects.firstNonNull(nc.getHostname(), name));
        cmd.setDomainName(nc.getDomainname());
        cmd.setEnv(env.toArray(new String[env.size()]));
        cmd.setImage(imageName);
        cmd.setLabels(result.getLabels());
        List<String> command = nc.getCommand();
        if(!CollectionUtils.isEmpty(command)) {
            cmd.setCmd(command.toArray(new String[command.size()]));
        }
        cmd.setHostConfig(getHostConfig(cc, result));


        LOG.info("Command for execution: {}", cmd);
        ProcessEvent.watch(cc.watcher, "Command for execution: {0}", cmd);
        return cmd;
    }

    private Map<String, Integer> getContainersPerNodeForImage(CreateContainerContext cc, String imageName) {
        //first we need to resolve use we cluster or single node
        String cluster = cc.arg.getContainer().getCluster();
        DockerService service = null;
        if (cluster != null) {
            service = getDockerForCluster(cluster);
        }
        if (service == null) {
            service = cc.dockerService;
        }
        Map<String, Integer> map = new HashMap<>();
        for (NodeInfo ni : service.getInfo().getNodeList()) {
            String nodeName = ni.getName();
            List<ContainerRegistration> containers = containerStorage.getContainersByNode(ni.getName());
            if(CollectionUtils.isEmpty(containers)) {
                continue;
            }
            int count = (int) containers.stream().filter(c -> imageName.equals(c.getContainer().getImage())).count();
            map.put(nodeName, count);
        }
        return map;
    }

    private HostConfig getHostConfig(CreateContainerContext cc, ContainerSource arg) {

        Long mem = arg.getMemoryLimit();
        RestartPolicy restartPolicy = getRestartPolicy(cc, arg);
        HostConfig.Builder builder = HostConfig.newHostConfig()
                .memory(mem)
                .blkioWeight(arg.getBlkioWeight())
                .cpuQuota(arg.getCpuQuota())
                .cpuShares(arg.getCpuShares())
                .binds(getHostBindings(cc, arg))
                .portBindings(getBindings(arg.getPorts()))
                .publishAllPorts(arg.isPublishAllPorts())
                .restartPolicy(restartPolicy);

        String cluster = arg.getCluster();
        List<Network> networks = cluster == null ? null : networkManager.getNetworks(cluster);
        if (networks != null && networks.stream().filter(n -> n.getName().equals(cluster)).count() > 0) {
            builder.networkMode(cluster);
        } else {
            LOG.warn("Cluster \"{}\" does not have any network, so container \"{}\" will be created with default network.", cluster, cc.getName());
        }
        return builder.build();
    }

    private RestartPolicy getRestartPolicy(CreateContainerContext cc, ContainerSource arg) {
        String restartPolicyString;
        if (arg.getRestart() != null) {
            restartPolicyString = arg.getRestart();
        } else {
            restartPolicyString = cc.dockerService.getClusterConfig().getDockerRestart();
        }
        //we can define default policy in future
        return restartPolicyString == null ? RestartPolicy.noRestart() : parse(restartPolicyString);
    }


    private Binds getHostBindings(CreateContainerContext cc, ContainerSource arg) {
        ArrayList<Bind> list = new ArrayList<>();
        for(Map.Entry<String, String> entry: arg.getVolumeBinds().entrySet()) {
            // note that key - is volume, value - path in container
            String value = entry.getValue();
            Bind bind = new Bind(value, new Volume(entry.getKey()));
            list.add(bind);
        }
        return new Binds(list);
    }

    private DockerService getDockerForCluster(String clusterId) {
        return dockerServiceRegistry.getService(clusterId);
    }

    private Ports getBindings(Map<String, String> publish) {
        Ports ports = new Ports();
        if (publish != null) {
            for (String key : publish.keySet()) {
                if (StringUtils.hasText(key)) {
                    String value = publish.get(key);
                    ExposedPort exposedPort = new ExposedPort(Integer.parseInt(key));
                    Ports.Binding binding = new Ports.Binding(Integer.parseInt(value));
                    ports.bind(exposedPort, binding);
                }
            }
        }
        return ports;
    }


}
