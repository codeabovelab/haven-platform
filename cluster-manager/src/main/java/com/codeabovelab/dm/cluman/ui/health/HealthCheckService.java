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

package com.codeabovelab.dm.cluman.ui.health;

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerUtils;
import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerDetails;
import com.codeabovelab.dm.cluman.ds.DockerServiceRegistry;
import com.codeabovelab.dm.common.healthcheck.ServiceHealthCheckResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.function.Consumer;

/**
 * Service which doe health check of container.
 */
@Component
public class HealthCheckService {

    private final DockerServiceRegistry dockerServiceRegistry;


    @Autowired
    public HealthCheckService(DockerServiceRegistry dockerServiceRegistry) {
        this.dockerServiceRegistry = dockerServiceRegistry;
    }

    public void checkAll(Consumer<ServiceHealthCheckResult> callback) {
        //TODO we need to implement some hatcheck here
        throw new UnsupportedOperationException("we need to implement some hatcheck here");
    }

    /**
     * Check single container health, may return null.
     * @param id
     * @param timeout
     * @return null or ServiceHealthCheckResult
     * @throws InterruptedException
     */
    public ServiceHealthCheckResult checkContainer(String cluster, String id, long timeout) {
        Assert.hasText(id, "id is null or empty");
        ContainerDetails container = dockerServiceRegistry.getService(cluster).getContainer(id);
        if(container == null) {
            throw new RuntimeException("No containers with id: " + id);
        }
        final String hostname = DockerUtils.getFullHostName(container.getConfig());
        Assert.hasText(hostname, "container.config.hostname is null or empty, it strange");
        throw new UnsupportedOperationException("we need to implement some hatcheck here");
    }
}
