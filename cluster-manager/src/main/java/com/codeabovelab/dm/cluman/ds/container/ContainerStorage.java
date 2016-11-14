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

import com.codeabovelab.dm.cluman.model.ContainerBaseIface;

import java.util.List;

public interface ContainerStorage {

    List<ContainerRegistration> getContainers();
    ContainerRegistration getContainer(String id);

    /**
     * Find container by 'cluster:name' too.
     * @param name
     * @return find registration or null
     */
    ContainerRegistration findContainer(String name);
    List<ContainerRegistration> getContainersByNode(String nodeName);
    ContainerRegistration getOrCreateContainer(ContainerBaseIface container, String node);

}
