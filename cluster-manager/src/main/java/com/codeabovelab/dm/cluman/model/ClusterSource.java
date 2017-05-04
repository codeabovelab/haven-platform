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

package com.codeabovelab.dm.cluman.model;

import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfigImpl;
import com.codeabovelab.dm.cluman.ds.clusters.NodesGroupConfig;
import com.codeabovelab.dm.common.json.JtToMap;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Source of cluster. We suppose that cluster can contains multiple applications. But
 * all resources (containers, networks and etc) not owned by app is belong to cluster directly.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonPropertyOrder({"title", "description", "config", "nodes", "applications", "containers"})
public class ClusterSource extends ApplicationSource implements NodesGroupConfig {
    @JtToMap(key = "name")
    @Setter(AccessLevel.NONE)
    private List<ApplicationSource> applications = new ArrayList<>();
    @Setter(AccessLevel.NONE)
    private List<String> nodes = new ArrayList<>();
    private String title;
    private String description;
    private String imageFilter;
    private String type;
    private ClusterConfigImpl config;
}
