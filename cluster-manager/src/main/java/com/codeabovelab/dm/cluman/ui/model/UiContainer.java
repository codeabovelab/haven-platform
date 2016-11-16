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

package com.codeabovelab.dm.cluman.ui.model;

import com.codeabovelab.dm.cluman.cluster.docker.model.Port;
import com.codeabovelab.dm.cluman.ds.container.ContainerRegistration;
import com.codeabovelab.dm.cluman.ds.container.ContainerStorage;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.ui.UiUtils;
import com.codeabovelab.dm.common.utils.Comparables;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * UI representation for Container
 */
@Data
public class UiContainer implements Comparable<UiContainer>, UiContainerIface, WithUiPermission {
    @NotNull protected String id;
    @NotNull protected String name;
    @NotNull protected String node;
    @NotNull protected String image;
    @NotNull protected String imageId;
    protected String application;
    protected String cluster;
    protected final List<String> command = new ArrayList<>();
    protected final List<Port> ports = new ArrayList<>();
    protected String status;
    protected Date created;
    protected boolean lock;
    protected String lockCause;
    protected final Map<String, String> labels = new HashMap<>();
    protected boolean run;
    private UiPermission permission;

    @Override
    public int compareTo(UiContainer o) {
        int comp = Comparables.compare(cluster, o.cluster);
        if(comp == 0) {
            comp = Comparables.compare(application, o.application);
        }
        if(comp == 0) {
            comp = Comparables.compare(node, o.node);
        }
        if(comp == 0) {
            comp = Comparables.compare(name, o.name);
        }
        if(comp == 0) {
            comp = Comparables.compare(image, o.image);
        }
        if(comp == 0) {
            comp = Comparables.compare(id, o.id);
        }
        return comp;
    }

    public static UiContainer from(DockerContainer container) {
        UiContainer uic = new UiContainer();
        return from(uic, container);
    }

    protected static  UiContainer from(UiContainer uic, DockerContainer container) {
        fromBase(uic, container);
        com.codeabovelab.dm.cluman.model.Node node = container.getNode();
        uic.setNode(node.getName());
        uic.setCreated(new Date(container.getCreated()));
        uic.getPorts().addAll(container.getPorts());
        String status = container.getStatus();
        uic.setStatus(status);
        uic.setRun(UiUtils.calculateIsRun(status));
        // this is workaround, because docker use simply command representation in container,
        // for full you need use ContainerDetails
        uic.getCommand().add(container.getCommand());
        return uic;
    }

    public static UiContainer fromBase(UiContainer uic, ContainerBaseIface container) {
        uic.setId(container.getId());
        uic.setName(container.getName());
        uic.setImage(container.getImage());
        uic.setImageId(container.getImageId());
        uic.getLabels().putAll(container.getLabels());
        UiUtils.resolveContainerLock(uic, container);
        return uic;
    }

    /**
     * Fill container data with some values from specified storages.
     * @param discoveryStorage
     * @param containerStorage
     */
    public void enrich(DiscoveryStorage discoveryStorage, ContainerStorage containerStorage) {
        //note that cluster can be virtual
        NodesGroup nodeCluster = discoveryStorage.getClusterForNode(getNode());
        if(nodeCluster != null) {
            setCluster(nodeCluster.getName());
        }

        ContainerRegistration registration = containerStorage.getContainer(getId());
        if (registration != null && registration.getAdditionalLabels() != null) {
            getLabels().putAll(registration.getAdditionalLabels());
        }
    }
}
