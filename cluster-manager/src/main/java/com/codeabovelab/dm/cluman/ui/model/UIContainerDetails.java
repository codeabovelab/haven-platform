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

import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerDetails;
import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerState;
import com.codeabovelab.dm.cluman.model.ContainerSource;
import com.codeabovelab.dm.cluman.model.DockerContainer;
import com.codeabovelab.dm.cluman.source.ContainerSourceFactory;
import com.codeabovelab.dm.cluman.ui.UiUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * Container details. Must strong complement with {@link ContainerSource } for filling UI forms with default
 * values and etc.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UIContainerDetails extends ContainerSource implements UiContainerIface, WithUiPermission {

    private Date created;
    private Date started;
    private Date finished;
    private List<String> args;
    private Integer restartCount;
    private boolean lock;
    private String lockCause;
    private boolean run;
    private String status;
    private DockerContainer.State state;
    private UiPermission permission;

    public UIContainerDetails() {
    }

    public UIContainerDetails from(ContainerSourceFactory containerSourceFactory, ContainerDetails container) {
        containerSourceFactory.toSource(container, this);
        setId(container.getId());
        UiUtils.resolveContainerLock(this, container);
        setArgs(container.getArgs());
        setRestartCount(container.getRestartCount());
        ContainerState state = container.getState();
        setStatus(state.getStatus());
        setRun(state.isRunning());
        setCreated(container.getCreated());
        setStarted(state.getStartedAt());
        setFinished(state.getFinishedAt());
        return this;
    }
}
