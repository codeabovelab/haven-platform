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

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.*;

/**
 * UI representation for Container service.
 * @see ContainerService
 */
@Data
public class UiContainerService implements Comparable<UiContainerService>, WithUiPermission {
    @NotNull protected String id;
    @NotNull protected String name;
    @NotNull protected String image;
    @NotNull protected String imageId;
    protected String application;
    protected String cluster;
    protected final List<String> command = new ArrayList<>();
    protected final List<Port> ports = new ArrayList<>();
    protected LocalDateTime created;
    protected LocalDateTime updated;
    protected final Map<String, String> labels = new HashMap<>();
    private UiPermission permission;

    @Override
    public int compareTo(UiContainerService o) {
        int comp = Comparables.compare(cluster, o.cluster);
        if(comp == 0) {
            comp = Comparables.compare(application, o.application);
        }
        if(comp == 0) {
            comp = Comparables.compare(name, o.name);
        }
        if(comp == 0) {
            comp = Comparables.compare(image, o.image);
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
        uic.setCreated(s.getCreated());
        uic.setUpdated(s.getUpdated());
        uic.setImage(s.getImage());
        uic.setImageId(s.getId());
        uic.getLabels().putAll(s.getLabels());
        uic.getPorts().addAll(s.getPorts());
        uic.getCommand().addAll(s.getCommand());
        return uic;
    }
}
