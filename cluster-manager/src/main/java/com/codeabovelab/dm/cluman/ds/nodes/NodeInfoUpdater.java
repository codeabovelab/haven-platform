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

package com.codeabovelab.dm.cluman.ds.nodes;

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerServiceEvent;
import com.codeabovelab.dm.cluman.ds.SwarmUtils;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.security.TempAuth;
import com.codeabovelab.dm.common.utils.AbstractAutostartup;
import com.codeabovelab.dm.common.mb.Subscriptions;
import com.codeabovelab.dm.common.utils.ExecutorUtils;
import com.codeabovelab.dm.common.utils.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 */
@Slf4j
@Component
class NodeInfoUpdater extends AbstractAutostartup {

    private final ScheduledExecutorService scheduledExecutor;
    private final NodeStorage storage;
    private final Subscriptions<DockerServiceEvent> dockerServiceSubs;
    private final Subscriptions<NodesGroupEvent> nodesGroupSubs;
    private final DiscoveryStorage discoveryStorage;
    @Value(SwarmUtils.EXPR_NODES_UPDATE_MS)
    private long updateTimeout;

    @Autowired
    public NodeInfoUpdater(DiscoveryStorage discoveryStorage, @Qualifier(NodesGroupEvent.BUS) Subscriptions<NodesGroupEvent> nodesGroupSubs,
                           @Qualifier(DockerServiceEvent.BUS) Subscriptions<DockerServiceEvent> dockerServiceSubs, NodeStorage storage) {
        this.scheduledExecutor = ExecutorUtils.singleThreadScheduledExecutor(this.getClass());
        this.discoveryStorage = discoveryStorage;
        this.nodesGroupSubs = nodesGroupSubs;
        this.dockerServiceSubs = dockerServiceSubs;
        this.storage = storage;
    }

    @Override
    protected void startInner() {
        this.addToClose(scheduledExecutor::shutdownNow);
        ScheduledFuture<?> sf = scheduledExecutor.scheduleWithFixedDelay(this::checkNodes, updateTimeout, updateTimeout, TimeUnit.MILLISECONDS);
        this.addToClose(() -> sf.cancel(true));
        this.addToClose(dockerServiceSubs.openSubscription(this::onDockerServiceEvent));
        this.addToClose(nodesGroupSubs.openSubscription(this::onNodesGroupEvent));

    }

    private void onNodesGroupEvent(NodesGroupEvent nge) {
        String action = nge.getAction();
        if(Objects.equals(action, StandardActions.UPDATE)) {
            return;
        }
        this.scheduledExecutor.execute(this::checkNodes);
    }


    private void onDockerServiceEvent(DockerServiceEvent e) {
        try {
            if(e instanceof DockerServiceEvent.DockerServiceInfoEvent) {
                // !it part of SwarmCluster! we need to move it
                //
                // we update health of all presented nodes
                // also it work only in standalone swarm cluster, and must be moved out here
                DockerServiceInfo info = ((DockerServiceEvent.DockerServiceInfoEvent) e).getInfo();
                for(NodeInfo node: info.getNodeList()) {
                    // we must not create nodes here,
                    // therefore they may be deleted before, and creation will restore its, that is not wanted
                    NodeRegistrationImpl reg = storage.getNodeRegistrationInternal(node.getName());
                    if(reg != null) {
                        reg.updateHealth(node.getHealth());
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Can not update nodes.", ex);
        }
    }

    private void checkNodes() {
        // periodically check online status of nodes
        try(TempAuth ta = TempAuth.asSystem()) {
            log.info("Begin update list of nodes");
            storage.forEachInternal(this::updateNodeInfo);
            log.info("End update list of nodes");
        } catch(Exception e) {
            log.error("Fail update list of nodes:", e);
        }
    }

    private void updateNodeInfo(NodeRegistrationImpl nr) {
        String clusterName = nr.getCluster();
        final NodesGroup ng = clusterName == null? null : discoveryStorage.getCluster(clusterName);
        log.info("Update node '{}' of '{}' cluster", nr.getName(), ng == null? null : ng.getName());
        final DockerServiceInfo dsi = loadDockerInfo(nr);
        nr.updateNodeInfo(b -> {
            NodeMetrics.Builder nmb = NodeMetrics.builder().from(b.getHealth());
            boolean online = dsi != null;
            if(online) {
                b.setLabels(dsi.getLabels());
                nmb.setTime(dsi.getSystemTime());
                nmb.setSysMemTotal(dsi.getMemory());
            }
            if(ng == null) {
                // we may handle healthy only when node out of cluster
                nmb.setState(online? NodeMetrics.State.ALONE : NodeMetrics.State.DISCONNECTED);
                nmb.setHealthy(online);
            }
            b.setHealth(nmb.build());
        });
        // this check offline status internal and cause status change event
        nr.getNodeInfo();
    }

    private DockerServiceInfo loadDockerInfo(NodeRegistrationImpl nr) {
        DockerServiceInfo tmp = null;
        try {
            DockerService docker = nr.getDocker();
            if(docker != null) {
                tmp = docker.getInfo();
            } else {
                log.error("Fail to load node '{}' info due to docker is null (address is '{}').", nr.getName(), nr.getAddress());
            }
        } catch (Exception e) {
            if(Throwables.has(e, SocketException.class)) {
                // suppress stack traces when exception is expected
                log.error("Fail to load node '{}' info due to error: {}", nr.getName(), e.toString());
            } else {
                log.error("Fail to load node '{}' info.", nr.getName(), e);
            }
        }
        return tmp;
    }
}
