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

package com.codeabovelab.dm.cluman.ds.clusters;

/**
 */
public interface NodesGroupConfig {
    /**
     * Cluster is present simple group of nodes.
     */
    String TYPE_DEFAULT = "DEFAULT";
    /**
     * Cluster contains nodes united by standalone swarm.
     */
    String TYPE_SWARM = "SWARM";
    /**
     * Cluster contains nodes united by docker in swarm-mode.
     */
    String TYPE_DOCKER = "DOCKER";

    String getName();
    void setName(String name);
    String getImageFilter();
    void setImageFilter(String imageFilter);
    String getTitle();
    void setTitle(String title);
    String getDescription();
    void setDescription(String description);

    static <T extends NodesGroupConfig> T copy(NodesGroupConfig src, T dst) {
        dst.setName(src.getName());
        dst.setImageFilter(src.getImageFilter());
        dst.setTitle(src.getTitle());
        dst.setDescription(src.getDescription());
        return dst;
    }
}
