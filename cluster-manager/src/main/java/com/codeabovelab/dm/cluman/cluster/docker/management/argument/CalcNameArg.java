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

package com.codeabovelab.dm.cluman.cluster.docker.management.argument;

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.google.common.base.MoreObjects;
import lombok.Builder;
import lombok.Data;

/**
 * Parameters required for creating new container
 */
@Builder
@Data
public class CalcNameArg {

    /**
     * explicitly passed name has highest priority
     * value of containerName field in configurations (image labels, external configuration, etc) has second priority
     */
    private final String containerName;
    /**
     * name of Docker Image
     */
    private final String imageName;
    /**
     * allocate created name
     */
    private final boolean allocate;
    private final DockerService dockerService;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("containerName", containerName)
                .add("imageName", imageName)
                .add("allocate", allocate)
                .add("dockerService", dockerService)
                .omitNullValues()
                .toString();
    }
}

