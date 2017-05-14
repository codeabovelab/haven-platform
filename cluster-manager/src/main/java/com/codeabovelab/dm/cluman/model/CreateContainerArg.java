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

import com.codeabovelab.dm.cluman.cluster.docker.management.result.ProcessEvent;
import lombok.Data;

import java.util.function.Consumer;

/**
 * Parameters required for creating new container
 */
@Data
public class CreateContainerArg {

    private Consumer<ProcessEvent> watcher;
    private ContainerSource container;
    private boolean enrichConfigs;

    public CreateContainerArg watcher(Consumer<ProcessEvent> watcher) {
        setWatcher(watcher);
        return this;
    }

    public CreateContainerArg container(ContainerSource container) {
        setContainer(container);
        return this;
    }

    public CreateContainerArg enrichConfigs(boolean enrichConfigs) {
        setEnrichConfigs(enrichConfigs);
        return this;
    }
}

