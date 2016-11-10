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

import com.codeabovelab.dm.cluman.model.NodesGroup;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 */
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class UiCluster extends UiClusterEditablePart implements Comparable<UiCluster> {

    @AllArgsConstructor
    @Data
    public static class Entry {
        private Integer on = 0;
        private Integer off = 0;
    }

    private String name;
    private Set<NodesGroup.Feature> features;
    private Entry nodes;
    private Entry containers;
    private UiPermission permission;
    private Set<String> applications = new HashSet<>();

    @Override
    public int compareTo(UiCluster o) {
        return this.name.compareTo(o.name);
    }
}
