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
import lombok.EqualsAndHashCode;
import org.springframework.util.Assert;

/**
 * We need immutable object which can be used as map key
 */
@EqualsAndHashCode
class AwsCredentials implements AWSCredentials {
    private final String secretKey;
    private final String accessKey;
    private final String region;

    public AwsCredentials(String secretKey, String accessKey, String region) {
        Assert.hasText(secretKey, "secretKey is null or empty");
        Assert.hasText(accessKey, "accessKey is null or empty");
        Assert.hasText(region, "region is null or empty");
        this.secretKey = secretKey;
        this.accessKey = accessKey;
        this.region = region;
    }

    @Override
    public String getAWSAccessKeyId() {
        return accessKey;
    }

    @Override
    public String getAWSSecretKey() {
        return secretKey;
    }

    public String getRegion() {
        return region;
    }
}
