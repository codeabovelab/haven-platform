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

package com.codeabovelab.dm.cluman.cluster.registry;

import com.codeabovelab.dm.cluman.cluster.registry.model.*;
import org.springframework.web.client.RestTemplate;

/**
 */
abstract class DockerRegistryAdapter<T extends DockerRegistryConfig> implements RegistryAdapter {

    protected final T config;
    private final RestTemplate rt;

    public DockerRegistryAdapter(T config, RestTemplateFactory rtf) {
        this.config = config;
        this.rt = rtf.create(new DockerRegistryAuthAdapter(this::getCredentials));
    }

    @Override
    public RestTemplate getRestTemplate() {
        return rt;
    }

    @Override
    public T getConfig() {
        return config;
    }

    @Override
    public RegistryCredentials getCredentials() {
        return config;
    }
}
