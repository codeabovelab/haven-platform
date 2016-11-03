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

import com.codeabovelab.dm.cluman.cluster.registry.*;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 */
@AllArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class AwsRegistryFactoryAdapter implements RegistryFactoryAdapter<AwsRegistryConfig> {

    private final AwsService awsService;

    @Override
    public RegistryService create(RegistryFactory factory, AwsRegistryConfig config) {
        return RegistryServiceImpl.builder()
          .adapter(new AwsRegistryAdapter(awsService, config, factory::restTemplate))
          .scheduledExecutorService(factory.getScheduledExecutorService())
          .build();
    }

    @Override
    public void complete(AwsRegistryConfig config) {
        if (config.getName() != null) {
            return;
        }
        AwsToken token = awsService.fetchToken(config);
        String endpoint = token.getProxyEndpoint();
        config.setName(RegistryUtils.getNameByUrl(endpoint));
    }
}
