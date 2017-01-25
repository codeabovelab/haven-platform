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

package com.codeabovelab.dm.cluman.source;

import com.codeabovelab.dm.cluman.cluster.application.ApplicationService;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.model.CreateContainerArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.DeleteContainerArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.CreateAndStartContainerResult;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.ds.SwarmUtils;
import com.codeabovelab.dm.cluman.ds.container.ContainerRegistration;
import com.codeabovelab.dm.cluman.ds.container.ContainerStorage;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.ds.swarm.NetworkManager;
import com.codeabovelab.dm.cluman.job.JobBean;
import com.codeabovelab.dm.cluman.job.JobContext;
import com.codeabovelab.dm.cluman.job.JobParam;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.common.utils.Joiner;
import com.codeabovelab.dm.common.utils.Throwables;
import com.google.common.base.Objects;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A job which do deploying of system source.
 */
@JobBean(DeploySourceJob.NAME)
public class DeploySourceJob implements Runnable {

    public static final String NAME = "job.deploySource";

    public enum ConflictResolution {
        /**
         * fail on each conflict
         */
        FAIL,
        /**
         * try to ignore conflicts (default)
         */
        LEAVE,
        /**
         * overwrite colliding resources
         */
        OVERWRITE
    }

    /**
     * Diagnostic context
     */
    @Data
    private static class Ctx {
        private ClusterSource cluster;
        private ApplicationSource app;
        private DockerService service;
        private NodesGroup nodesGroup;

        public String getPath(String container) {
            StringBuilder sb = new StringBuilder()
              .append(getClusterName()).append('/')
              .append(getApplicationName()).append('/')
              .append(container);
            return sb.toString();
        }

        private String getApplicationName() {
            return app == null? "<no app>" : app.getName();
        }

        private String getClusterName() {
            return cluster.getName();
        }
    }

    @JobParam("source")
    private RootSource source;

    @JobParam("options")
    private DeployOptions options = DeployOptions.DEFAULT;

    @Autowired
    private DiscoveryStorage discoveryStorage;

    @Autowired
    private ContainerStorage containerStorage;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private NodeStorage nodeStorage;

    @Autowired
    private NetworkManager networkManager;

    @Autowired
    private JobContext jobContext;

    @Override
    public void run() {
        SourceUtil.validateSource(source);
        options.validate();
        Ctx dc = new Ctx();
        for(ClusterSource clusterSource: source.getClusters()) {
            try {
                doCluster(dc, clusterSource);
            } catch (Exception e) {
                throw Throwables.asRuntime(e);
            }
        }
    }

    private void doCluster(Ctx dc, ClusterSource clusterSource) throws Exception {
        dc.setCluster(clusterSource);
        String cluster = clusterSource.getName();
        jobContext.fire("Begin create cluster: {0}", cluster);
        NodesGroup ng = discoveryStorage.getOrCreateCluster(cluster, ccc -> {
            ccc.setBeforeClusterInit((c) -> {
                jobContext.fire("Create cluster: {0}, with config: {1}", cluster, c.getConfig());
            });
            return ccc.createConfig(clusterSource.getType());
        });
        dc.setNodesGroup(ng);
        dc.setService(ng.getDocker());
        addNodes(dc, clusterSource);
        deployContainers(dc, clusterSource, ContainerHandler.NOP);
        for(ApplicationSource appSrc: clusterSource.getApplications()) {
            deployApp(dc, appSrc);
        }
        jobContext.fire("End create cluster: {0}", cluster);
    }

    private void addNodes(Ctx dc, ClusterSource clusterSource) throws Exception {
        List<String> nodes = clusterSource.getNodes();
        String clusterName = clusterSource.getName();
        jobContext.fire("Try to add nodes: {0}, to cluster: {1}", Joiner.on(", ").join(nodes).toString(), clusterName);
        nodes.forEach((node) -> nodeStorage.setNodeCluster(node, clusterName));

        // below code (very terrible, and need rewrite)
        // is wait up to minute when node joined into cluster
        // after that it wait for creation of cluster-wide network
        // TODO we need calculate timeout as 'max(nodeUpdatePeriod)*2'
        DockerService service = dc.getService();
        int i = 0;
        int unexists = nodes.size();
        while(i < 3) {
            i++;
            List<NodeInfo> exists = service.getInfo().getNodeList();
            for(NodeInfo ni: exists) {
                String name = ni.getName();
                if(nodes.contains(name)) {
                    unexists--;
                }
            }
            if(unexists == 0) {
                break;
            }
            Thread.sleep(30_000L);
            unexists = nodes.size();
        }
        if(unexists > 0) {
            //TODO which nodes we can not add?
            throw new RuntimeException("Can not add nodes " + nodes + " to cluster: " + clusterName);
        }
        if(nodes.isEmpty()) {
            return;
        }
        i = 0;
        while(true) {
            i++;
            ServiceCallResult res = networkManager.createNetwork(clusterName);
            ResultCode code = res.getCode();
            if(code == ResultCode.OK || code == ResultCode.NOT_MODIFIED) {
                break;
            }
            if(i > 3) {
                throw new RuntimeException("Can not create network for cluster: " + clusterName);
            }
            Thread.sleep(30_000L);
        }
    }

    private void deployApp(Ctx dc, ApplicationSource appSrc) throws Exception {
        dc.setApp(appSrc);
        jobContext.fire("Begin create app {0}", appSrc.getName());
        List<String> containerNames = new ArrayList<>();
        ContainerHandler ch = (cs, cr) -> {
            containerNames.add(cr.getName());
        };
        deployContainers(dc, appSrc, ch);
        ApplicationImpl app = ApplicationImpl.builder()
          .name(appSrc.getName())
          .cluster(dc.getClusterName())
          .creatingDate(new Date())
          .containers(containerNames)
          .build();
        applicationService.addApplication(app);
        jobContext.fire("End create app {0}", appSrc.getName());
    }

    private void deployContainers(Ctx ctx, ApplicationSource containersSrc, ContainerHandler ch) {
        for(ContainerSource containerSource: containersSrc.getContainers()) {
            deployContainer(ctx, containerSource, ch);
        }
    }

    private void deployContainer(Ctx ctx, ContainerSource containerSource, ContainerHandler ch) {
        String name = containerSource.getName();
        String containerLogId = ctx.getPath(name);
        if (checkContainerConflicts(ctx, containerSource)) {
            return;
        }
        jobContext.fire("Begin create container {0}", containerLogId);
        ContainerSource clone = containerSource.clone();
        clone.setApplication(ctx.getApplicationName());
        clone.setCluster(ctx.getClusterName());
        SwarmUtils.clearConstraints(clone.getLabels());
        CreateContainerArg cca = CreateContainerArg.builder().container(clone)
                .watcher((pe) -> jobContext.fire("On {0}, {1}", containerLogId, pe.getMessage())).build();
        CreateAndStartContainerResult ccr = ctx.getNodesGroup().getContainers().createContainer(cca);
        ch.handle(clone, ccr);
        String containerId = ccr.getContainerId();
        jobContext.fire("End create container {0} with id {1} and result {2}", containerLogId, containerId, ccr);
    }

    private boolean checkContainerConflicts(Ctx ctx, ContainerSource containerSource) {
        String name = containerSource.getName();
        if(name == null) {
            return false;
        }
        String containerLogId = ctx.getPath(name);
        boolean exists = false;
        String conflictId = null;
        String newName = null;
        //check exists
        String id = containerSource.getId();
        List<ContainerRegistration> containers = containerStorage.getContainers();
        for(ContainerRegistration cr: containers) {
            DockerContainer cb = cr.getContainer();
            if(cb == null) {
                continue;
            }
            String crName = cb.getName();
            final String crId = cr.getId();
            if(name.equals(crName)) {
                if(exists) {
                    throw new RuntimeException("Multiple containers with same name: " + crName
                      + " ids: " + conflictId + ", " + crId);
                }
                exists = true;
                conflictId = crId;
            } else if(id != null && id.equals(crId)) {
                newName = crName;
            }
        }
        if(newName != null) {
            // in future we may rename all dependencies for renamed container, or detect that it not has any dependency
            // but now we only can fail
            throw new RuntimeException("Container " + containerLogId + " (" + id + ") is renamed to " + newName);
        }
        if(exists) {
            //conflict
            switch (this.options.getContainersConflict()) {
                case LEAVE:
                    jobContext.fire("Container {0} is already exists, leave it.", containerLogId);
                    return true;
                case OVERWRITE:
                    deleteContainer(ctx, conflictId);
                default:
                throw new RuntimeException("Container " + containerLogId + " (" + id + ") is already exists with " +
                  (Objects.equal(id, conflictId)? "same": conflictId) + " id.");
            }
        }
        return false;
    }

    private void deleteContainer(Ctx ctx, String id) {
        jobContext.fire("Delete container \"{0}\"", id);
        DockerService service = ctx.getService();
        ServiceCallResult res = service.deleteContainer(DeleteContainerArg.builder().id(id).kill(true).build());
        jobContext.fire("Delete container \"{0}\" result: {1}", id, res);

    }
}
