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

import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryAdapter;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryCredentials;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryConfig;
import com.codeabovelab.dm.cluman.cluster.registry.model.RestTemplateFactory;
import org.springframework.web.client.RestTemplate;

/**
 */
public class AwsRegistryAdapter implements RegistryAdapter {

    private final AwsToken awsToken;
    private final AwsRegistryConfig config;
    private final RestTemplate rt;

    public AwsRegistryAdapter(AwsService awsService, AwsRegistryConfig config, RestTemplateFactory rtf) {
        this.config = config;
        this.awsToken = awsService.fetchToken(config);
        String name = config.getName();
        if(name == null) {
           config.setName(awsToken.getProxyEndpoint());
        }
        this.rt = rtf.create(new AwsRegistryAuthAdapter(this));
    }

    @Override
    public RestTemplate getRestTemplate() {
        return rt;
    }

    @Override
    public String getUrl() {
        return awsToken.getProxyEndpoint();
    }

    @Override
    public RegistryConfig getConfig() {
        return config;
    }

    @Override
    public RegistryCredentials getCredentials() {
        return awsToken;
    }
}
