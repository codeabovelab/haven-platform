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

import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Service;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Task;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.source.ServiceSourceConverter;
import com.codeabovelab.dm.common.utils.Comparables;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * UI representation for Container service.
 * @see ContainerService
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UiContainerService extends ServiceSource implements WithUiPermission {
    protected long version;
    protected LocalDateTime created;
    protected LocalDateTime updated;
    private UiPermission permission;
    private long runningReplicas;
    private final List<UiServiceTask> tasks = new ArrayList<>();

    @Override
    public int compareTo(ServiceSource o) {
        int comp = Comparables.compare(getCluster(), o.getCluster());
        if(comp == 0) {
            comp = Comparables.compare(getApplication(), o.getApplication());
        }
        if(comp == 0) {
            comp = Comparables.compare(getName(), o.getName());
        }
        ContainerSource cs = getContainer();
        ContainerSource ocs = o.getContainer();
        if(comp == 0 && cs != null && ocs != null) {
            comp = Comparables.compare(cs.getImage(), ocs.getImage());
        }
        if(comp == 0) {
            comp = Comparables.compare(getId(), o.getId());
        }
        return comp;
    }

    /**
     * Convert service to its ui presentation
     * @param ng group which contains specified service
     * @param s service
     * @return ui presentation of service
     */
    public static UiContainerService from(NodesGroup ng, ContainerService s) {
        UiContainerService uic = new UiContainerService();
        Service srv = s.getService();
        uic.setId(srv.getId());
        Service.ServiceSpec srvSpec = srv.getSpec();
        uic.setVersion(srv.getVersion().getIndex());
        uic.setCreated(srv.getCreated());
        uic.setUpdated(srv.getUpdated());
        ServiceSourceConverter ssc = new ServiceSourceConverter();
        ssc.setNodesGroup(ng);
        ssc.setServiceSpec(srvSpec);
        ssc.toSource(uic);
        uic.setCluster(s.getCluster());
        List<UiServiceTask> tasks = uic.getTasks();
        Map<String, String> clusterIdToNodeName = ng.getNodes().stream().collect(Collectors.toMap(NodeInfo::getIdInCluster, NodeInfo::getName));
        s.getTasks().forEach(st -> {
            UiServiceTask ut = UiServiceTask.from(st, clusterIdToNodeName::get);
            if(ut.getState() == ut.getDesiredState() && ut.getDesiredState() == Task.TaskState.RUNNING) {
                //we may change it in future, therefore calc count of replicas on backend
                uic.runningReplicas++;
            }
            tasks.add(ut);
        });
        return uic;
    }

    @Data
    public static class UiServiceTask {

        private String id;
        private String container;
        private String node;
        private String error;
        private String message;
        private LocalDateTime timestamp;
        private Task.TaskState state;
        private Task.TaskState desiredState;

        public static UiServiceTask from(Task st, Function<String, String> nodeNameById) {
            UiServiceTask ut = new UiServiceTask();
            ut.setId(st.getId());
            ut.setNode(nodeNameById.apply(st.getNodeId()));
            Task.TaskStatus status = st.getStatus();
            ut.setError(status.getError());
            ut.setMessage(status.getMessage());
            ut.setTimestamp(status.getTimestamp());
            ut.setState(status.getState());
            ut.setDesiredState(st.getDesiredState());
            Task.ContainerStatus containerStatus = status.getContainerStatus();
            ut.setContainer(containerStatus.getContainerId());
            return ut;
        }
    }
}
