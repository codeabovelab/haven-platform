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

package com.codeabovelab.dm.cluman.cluster.docker.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor(onConstructor = @__(@JsonCreator))
@Data
public class Info {

    @JsonProperty("Id")
    private final String id;
    @JsonProperty("Containers")
    private final Integer containers;
    @JsonProperty("Images")
    private final Integer images;
    @JsonProperty("NCPU")
    private final Integer ncpu;
    @JsonProperty("MemTotal")
    private final Long memory;
    @JsonProperty("Name")
    private final String name;

    @JsonProperty("DriverStatus")
    private final List<List<String>> driverStatus;
    @JsonProperty("SystemStatus")
    private final List<List<String>> systemStatus;

}
