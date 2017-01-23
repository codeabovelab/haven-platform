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

import com.codeabovelab.dm.cluman.cluster.docker.management.argument.CreateContainerArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.CreateAndStartContainerResult;

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

    CreateAndStartContainerResult createContainer(CreateContainerArg arg);
}
