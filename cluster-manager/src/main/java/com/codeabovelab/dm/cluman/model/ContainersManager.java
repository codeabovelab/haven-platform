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

package com.codeabovelab.dm.cluman.model;

import com.codeabovelab.dm.cluman.cluster.docker.management.argument.CreateServiceArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.DeleteContainerArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.StopContainerArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.UpdateServiceArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.*;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;

import java.util.Collection;

/**
 * Tol for managing cluster containers. <p/>
 * Old standalone swarm can show cluster containers, but swarm-mode, use tasks & service concept.
 */
public interface ContainersManager {

    /**
     * Collection of swarm-mode services. Not all clusters support this, then return empty collection.
     * @return non null collection
     */
    Collection<ContainerService> getServices();

    /**
     * Collection of all containers (include service tasks and standalone containers)
     * @return non null collection
     */
    Collection<DockerContainer> getContainers();

    /**
     * Create container. Note that it created on
     * @param arg argument
     * @return container creation result
     */
    CreateAndStartContainerResult createContainer(CreateContainerArg arg);

    /**
     * Update (edit) container.
     * @param arg arg
     * @return update container result
     */
    ServiceCallResult updateContainer(EditContainerArg arg);
    ServiceCallResult stopContainer(StopContainerArg arg);
    ServiceCallResult restartContainer(StopContainerArg arg);
    ServiceCallResult startContainer(String containerId);
    ServiceCallResult pauseContainer(String containerId);
    ServiceCallResult deleteContainer(DeleteContainerArg arg);
    ServiceCallResult scaleContainer(ScaleContainerArg arg);

    /**
     * Create service, when cluster does not supported services underline code emulate them.
     * @param arg argument
     * @return service creation result
     */
    ServiceCallResult createService(CreateServiceArg arg);

    /**
     * Update (edit) service.
     * @param arg arg
     * @return update service result
     */
    ServiceCallResult updateService(UpdateServiceArg arg);
    ServiceCallResult deleteService(String service);

}
