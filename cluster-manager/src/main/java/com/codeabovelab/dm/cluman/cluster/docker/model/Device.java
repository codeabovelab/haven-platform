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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonInclude(Include.NON_NULL)
public class Device {

    @JsonProperty("CgroupPermissions")
    private String cGroupPermissions = "";

    @JsonProperty("PathOnHost")
    private String pathOnHost = null;

    @JsonProperty("PathInContainer")
    private String pathInContainer = null;

    public Device() {
    }

    public Device(String cGroupPermissions, String pathInContainer, String pathOnHost) {
        checkNotNull(cGroupPermissions, "cGroupPermissions is null");
        checkNotNull(pathInContainer, "pathInContainer is null");
        checkNotNull(pathOnHost, "pathOnHost is null");
        this.cGroupPermissions = cGroupPermissions;
        this.pathInContainer = pathInContainer;
        this.pathOnHost = pathOnHost;
    }

    public String getcGroupPermissions() {
        return cGroupPermissions;
    }

    public String getPathInContainer() {
        return pathInContainer;
    }

    public String getPathOnHost() {
        return pathOnHost;
    }


}