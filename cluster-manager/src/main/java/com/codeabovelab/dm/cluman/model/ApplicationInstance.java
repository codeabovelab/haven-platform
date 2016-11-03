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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.google.common.base.MoreObjects.firstNonNull;

@Data
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
@Builder
public class ApplicationInstance implements Application {

    private final String name;
    private final String cluster;
    private final String initFile;
    private final Date creatingDate;
    private final List<String> containers;

    public ApplicationInstance.ApplicationInstanceBuilder cloneToBuilder() {
        return ApplicationInstance.builder()
                .name(name)
                .cluster(cluster)
                .initFile(initFile)
                .creatingDate(creatingDate)
                .containers(containers);
    }

    @Override
    public List<String> getContainers() {
        return firstNonNull(containers, Collections.<String>emptyList());
    }
}
