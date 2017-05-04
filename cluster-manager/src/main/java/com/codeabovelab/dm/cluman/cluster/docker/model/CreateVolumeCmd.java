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

package com.codeabovelab.dm.cluman.cluster.docker.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 */
@Data
public class CreateVolumeCmd {
    /**
     * The new volume's name. If not specified, Docker generates a name.
     */
    @JsonProperty("Name")
    private String name;

    /**
     * Name of the volume driver to use.
     * Default 'local'.
     */
    @JsonProperty("Driver")
    private String driver;

    /**
     * A mapping of driver options and values. These options are passed directly to the driver and are driver specific.
     */
    @JsonProperty("DriverOpts")
    private Map<String, String> driverOpts;

    /**
     * User-defined key/value metadata.
     */
    @JsonProperty("Labels")
    private Map<String, String> labels;


}
