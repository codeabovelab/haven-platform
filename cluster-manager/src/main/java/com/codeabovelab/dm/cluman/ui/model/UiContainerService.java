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

import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.common.utils.Comparables;
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
public class UiContainerService extends UiContainerServiceCore implements Comparable<UiContainerService>, WithUiPermission {
    protected long version;
    protected String application;
    protected final List<Port> ports = new ArrayList<>();
    protected LocalDateTime created;
    protected LocalDateTime updated;
    private UiPermission permission;

    @Override
    public int compareTo(UiContainerService o) {
        ContainerSource cs = getContainer();
        ContainerSource ocs = o.getContainer();
        int comp = Comparables.compare(cluster, o.cluster);
        if(comp == 0) {
            comp = Comparables.compare(application, o.application);
        }
        if(comp == 0) {
            comp = Comparables.compare(name, o.name);
        }
        if(comp == 0) {
            comp = Comparables.compare(cs.getImage(), ocs.getImage());
        }
        if(comp == 0) {
            comp = Comparables.compare(id, o.id);
        }
        return comp;
    }

    public static UiContainerService from(ContainerService s) {
        UiContainerService uic = new UiContainerService();
        uic.setId(s.getId());
        uic.setName(s.getName());
        uic.setCluster(s.getCluster());
        uic.setVersion(s.getVersion());
        uic.setCreated(s.getCreated());
        uic.setUpdated(s.getUpdated());
        ContainerSource cs = new ContainerSource();
        cs.setImage(s.getImage());
        cs.setImageId(s.getImageId());
        cs.getCommand().addAll(s.getCommand());
        uic.setContainer(cs);
        uic.getLabels().putAll(s.getLabels());
        uic.getPorts().addAll(s.getPorts());
        return uic;
    }
}
