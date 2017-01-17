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

package com.codeabovelab.dm.cluman.cluster.docker.management.argument;

import com.codeabovelab.dm.cluman.cluster.docker.model.AuthConfig;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Service;
import lombok.Data;

/**
 * 'Create a service. When using this endpoint to create a service using a private repository from the registry,
 * the X-Registry-Auth header must be used to include a base64-encoded AuthConfig object. Refer to the create
 * an image section for more details.'
 */
@Data
public class UpdateServiceArg {
    private AuthConfig registryAuth;
    /**
     * id or name
     */
    private String service;
    private Service.ServiceSpec spec;
}
