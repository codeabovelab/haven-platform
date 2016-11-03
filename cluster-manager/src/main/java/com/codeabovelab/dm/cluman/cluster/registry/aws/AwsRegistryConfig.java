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

package com.codeabovelab.dm.cluman.cluster.registry.aws;

import com.codeabovelab.dm.cluman.cluster.registry.RegistryType;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryConfig;
import com.codeabovelab.dm.common.kv.mapping.KvMapping;
import com.codeabovelab.dm.common.kv.mapping.PropertyCipher;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Configuration for registry service
 */
@EqualsAndHashCode(callSuper = true)
@Data
public final class AwsRegistryConfig extends RegistryConfig {
    @KvMapping
    private String accessKey;
    @KvMapping(interceptors = PropertyCipher.class)
    private String secretKey;
    @KvMapping
    private String region;
    {
        setRegistryType(RegistryType.AWS);
    }

    @Override
    public AwsRegistryConfig clone() {
        return (AwsRegistryConfig) super.clone();
    }

    @Override
    public void cleanCredentials() {
        setSecretKey(null);
    }
}
