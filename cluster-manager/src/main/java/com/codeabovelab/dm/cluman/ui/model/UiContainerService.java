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
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.source.SourceUtil;
import com.codeabovelab.dm.common.utils.Comparables;
import com.codeabovelab.dm.common.utils.Sugar;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.*;

/**
 * UI representation for Container service.
 * @see ContainerService
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UiContainerService extends ServiceSource implements WithUiPermission {
    protected long version;
    protected LocalDateTime created;
    protected LocalDateTime updated;
    private UiPermission permission;

    @Override
    public int compareTo(ServiceSource o) {
        int comp = Comparables.compare(getCluster(), o.getCluster());
        if(comp == 0) {
            comp = Comparables.compare(getApplication(), o.getApplication());
        }
        if(comp == 0) {
            comp = Comparables.compare(getName(), o.getName());
        }
        if(comp == 0) {
            comp = Comparables.compare(getImage(), o.getImage());
        }
        if(comp == 0) {
            comp = Comparables.compare(getId(), o.getId());
        }
        return comp;
    }

    public static UiContainerService from(ContainerService s) {
        UiContainerService uic = new UiContainerService();
        Service srv = s.getService();
        uic.setId(srv.getId());
        Service.ServiceSpec srvSpec = srv.getSpec();
        uic.setVersion(srv.getVersion().getIndex());
        uic.setCreated(srv.getCreated());
        uic.setUpdated(srv.getUpdated());
        SourceUtil.toSource(srvSpec, uic);
        uic.setCluster(s.getCluster());
        return uic;
    }
}
