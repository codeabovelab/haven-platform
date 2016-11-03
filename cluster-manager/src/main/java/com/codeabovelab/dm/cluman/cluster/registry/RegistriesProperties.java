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

import com.codeabovelab.dm.cluman.cluster.registry.aws.AwsRegistryConfig;
import com.codeabovelab.dm.cluman.cluster.registry.model.HubRegistryConfig;
import com.codeabovelab.dm.cluman.cluster.registry.model.PrivateRegistryConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 */
@ConfigurationProperties("dm.registries")
@Data
public class RegistriesProperties {
    /**
     * Used for synchronous initialization of registries.
     * <p/> Usually applicable for debugging and tests.
     */
    private boolean syncInit = false;

    private List<PrivateRegistryConfig> privateRegistry;
    private List<HubRegistryConfig> hubRegistry;
    private List<AwsRegistryConfig> awsRegistry;
}
