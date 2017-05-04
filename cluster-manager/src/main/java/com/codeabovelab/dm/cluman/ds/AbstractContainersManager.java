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
import com.codeabovelab.dm.cluman.model.ContainersManager;
import org.springframework.util.Assert;

import java.util.function.Supplier;

/**
 */
public abstract class AbstractContainersManager implements ContainersManager {
    protected final Supplier<DockerService> supplier;

    public AbstractContainersManager(Supplier<DockerService> supplier) {
        this.supplier = supplier;
    }

    protected DockerService getDocker() {
        DockerService service = supplier.get();
        Assert.notNull(service, "supplier " + supplier + " return null value");
        return service;
    }
}
