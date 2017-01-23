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
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.CreateContainerArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetContainersArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.CreateAndStartContainerResult;
import com.codeabovelab.dm.cluman.ds.container.ContainerManager;
import com.codeabovelab.dm.cluman.model.ContainerService;
import com.codeabovelab.dm.cluman.model.DockerContainer;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 */
public class DockerContainersManager extends AbstractContainersManager {

    private final ContainerManager containerManager;

    public DockerContainersManager(Supplier<DockerService> supplier, ContainerManager containerManager) {
        super(supplier);
        Assert.notNull(supplier);
        this.containerManager = containerManager;
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
        return this.containerManager.createContainer(arg);
    }
}
