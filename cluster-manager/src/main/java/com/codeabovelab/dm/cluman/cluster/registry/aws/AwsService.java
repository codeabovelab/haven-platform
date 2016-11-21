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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.ecr.AmazonECR;
import com.amazonaws.services.ecr.AmazonECRClient;
import com.amazonaws.services.ecr.model.*;
import com.codeabovelab.dm.common.utils.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class AwsService {

    private final LoadingCache<AwsCredentials, AwsToken> cache = CacheBuilder
            .newBuilder()
            .expireAfterAccess(6, TimeUnit.HOURS)
            .build(new CacheLoader<AwsCredentials, AwsToken>() {
                @Override
                public AwsToken load(AwsCredentials awsCredentials) throws Exception {
                    AmazonECR amazonECR = new AmazonECRClient(new AWSCredentialsProvider() {
                        @Override
                        public AWSCredentials getCredentials() {
                            return awsCredentials;
                        }

                        @Override
                        public void refresh() {
                        }
                    });
                    amazonECR.setRegion(RegionUtils.getRegion(awsCredentials.getRegion()));
                    GetAuthorizationTokenResult authorizationToken = amazonECR.getAuthorizationToken(new GetAuthorizationTokenRequest());
                    List<AuthorizationData> authorizationData = authorizationToken.getAuthorizationData();
                    Assert.isTrue(!CollectionUtils.isEmpty(authorizationData));
                    AuthorizationData data = authorizationData.get(0);
                    byte[] decode = Base64.getDecoder().decode(data.getAuthorizationToken());
                    String token = new String(decode);
                    String[] split = token.split(":");
                    log.info("about to connect to AWS endpoint: {}", data.getProxyEndpoint());
                    return AwsToken.builder().username(split[0]).password(split[1])
                            .expiresAt(data.getExpiresAt()).proxyEndpoint(data.getProxyEndpoint()).build();
                }

            });


    AwsToken fetchToken(AwsRegistryConfig config) {
        try {
            return cache.get(new AwsCredentials(config.getSecretKey(), config.getAccessKey(), config.getRegion()));
        } catch (ExecutionException e) {
            throw Throwables.asRuntime(e);
        }
    }
}
