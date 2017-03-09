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

package com.codeabovelab.dm.cluman.ds;

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.*;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.CreateAndStartContainerResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerDetails;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.UpdateContainerCmd;
import com.codeabovelab.dm.cluman.ds.container.ContainerCreator;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 */
public class SwarmClusterContainers extends AbstractContainersManager {

    private final ContainerCreator containerCreator;

    public SwarmClusterContainers(Supplier<DockerService> supplier, ContainerCreator containerCreator) {
        super(supplier);
        Assert.notNull(supplier, "supplier can't be null");
        this.containerCreator = containerCreator;
    }

    @Override
    public Collection<ContainerService> getServices() {
        return Collections.emptyList();
    }

    @Override
    public Collection<DockerContainer> getContainers() {
        List<DockerContainer> containers = getDocker().getContainers(new GetContainersArg(true));
        return containers;
    }

    @Override
    public CreateAndStartContainerResult createContainer(CreateContainerArg arg) {
        return this.containerCreator.createContainer(arg, getDocker());
    }

    @Override
    public ServiceCallResult updateContainer(EditContainerArg arg) {
        UpdateContainerCmd cmd = new UpdateContainerCmd();
        EditableContainerSource src = arg.getSource();
        cmd.from(src);
        cmd.setId(arg.getContainerId());
        return getDocker().updateContainer(cmd);
    }

    @Override
    public ServiceCallResult stopContainer(StopContainerArg arg) {
        return getDocker().stopContainer(arg);
    }

    @Override
    public ServiceCallResult startContainer(String containerId) {
        return getDocker().startContainer(containerId);
    }

    @Override
    public ServiceCallResult pauseContainer(String containerId) {
        return getDocker().pauseContainer(containerId);
    }

    @Override
    public ServiceCallResult unpauseContainer(String containerId) {
        return getDocker().unpauseContainer(containerId);
    }

    @Override
    public ServiceCallResult deleteContainer(DeleteContainerArg arg) {
        return getDocker().deleteContainer(arg);
    }

    @Override
    public ServiceCallResult restartContainer(StopContainerArg arg) {
        return getDocker().restartContainer(arg);
    }

    @Override
    public ServiceCallResult scaleContainer(ScaleContainerArg arg) {
        return containerCreator.scale(getDocker(), arg.getScale(), arg.getContainerId());
    }

    @Override
    public ContainerDetails getContainer(String id) {
        return getDocker().getContainer(id);
    }

    @Override
    public ContainerService getService(String id) {
        return null;
    }

    @Override
    public ServiceCallResult createService(CreateServiceArg arg) {
        return ServiceCallResult.unsupported();
    }

    @Override
    public ServiceCallResult updateService(UpdateServiceArg arg) {
        return ServiceCallResult.unsupported();
    }

    @Override
    public ServiceCallResult deleteService(String service) {
        return ServiceCallResult.unsupported();
    }
}
