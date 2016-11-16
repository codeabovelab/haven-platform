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

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetContainersArg;
import com.codeabovelab.dm.cluman.model.DockerContainer;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 */
@Component
public class ContainerNamesSupplier implements Function<DockerService, Collection<String>> {

    @Override
    public Collection<String> apply(DockerService dockerService) {
        List<DockerContainer> containerIfaces = getContainers(dockerService);
        List<String> names = new ArrayList<>(containerIfaces.size());
        for(DockerContainer containerIface : containerIfaces) {
            String name = containerIface.getName();
            names.add(name);
        }
        return names;
    }

    private List<DockerContainer> getContainers(DockerService dockerService) {
        return dockerService.getContainers(new GetContainersArg(true));
    }

}
