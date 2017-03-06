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

package com.codeabovelab.dm.cluman.ui.model;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Endpoint;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Service;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Task;
import com.codeabovelab.dm.cluman.model.ContainerService;
import com.codeabovelab.dm.cluman.model.Port;
import com.codeabovelab.dm.cluman.source.SourceUtil;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * UI representation for Container service create.
 *
 * @see ContainerService
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UiContainerServiceCreate extends UiContainerServiceCore {

    @ApiModelProperty("service.version, need for update")
    protected long version;
    protected final List<Port> ports = new ArrayList<>();

    public Service.ServiceSpec.Builder toServiceSpec() {
        Service.ServiceSpec.Builder ssb = Service.ServiceSpec.builder();
        ssb.name(getName());
        ssb.labels(getLabels());
        ssb.endpointSpec(Endpoint.EndpointSpec.builder().build());
        Task.TaskSpec.Builder tsb = Task.TaskSpec.builder();
        SourceUtil.fromSource(getContainer(), tsb);
        ssb.taskTemplate(tsb.build());
        return ssb;
    }
}
