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

import com.codeabovelab.dm.common.json.JtToMap;
import com.codeabovelab.dm.common.utils.Cloneables;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Root entry for source file.
 */
@Data
public class RootSource implements Cloneable {
    /**
     * First an only one supported version
     */
    public static final String V_1_0 = "1.0";
    private String version = V_1_0;
    @JtToMap(key = "name")
    @Setter(AccessLevel.NONE)
    private List<ClusterSource> clusters = new ArrayList<>();

    @Override
    public RootSource clone() {
        try {
            RootSource clone = (RootSource) super.clone();
            clone.clusters = Cloneables.clone(clone.clusters);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
