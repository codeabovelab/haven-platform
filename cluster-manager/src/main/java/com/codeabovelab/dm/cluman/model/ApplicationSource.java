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
import com.codeabovelab.dm.common.utils.Comparables;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
public class ApplicationSource implements Cloneable, Comparable<ApplicationSource> {

    private String name;
    @JtToMap(key = "name")
    @Setter(AccessLevel.NONE)
    private List<ContainerSource> containers = new ArrayList<>();
    //TODO + networks
    //TODO + volumes


    @Override
    public ApplicationSource clone() {
        try {
            ApplicationSource clone = (ApplicationSource) super.clone();
            clone.containers = Cloneables.clone(clone.containers);
            // do not forget uncomment below
            //clone.networks = Cloneables.clone(clone.networks);
            //clone.volumes = Cloneables.clone(clone.volumes);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int compareTo(ApplicationSource o) {
        return Comparables.compare(name, o.name);
    }
}
