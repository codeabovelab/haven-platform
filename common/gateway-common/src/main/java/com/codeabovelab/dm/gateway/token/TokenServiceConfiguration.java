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

package com.codeabovelab.dm.gateway.token;

import com.codeabovelab.dm.common.security.token.SignedTokenServiceBackend;
import com.codeabovelab.dm.common.security.token.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;

@Configuration
public class TokenServiceConfiguration {

    @Value("${token.serverSecret:3153620caaf300c37b345d0c2e8dc3aa322c6d9d}")
    private String serverSecret;

    @Value("${token.macAlgorithm:HmacSHA1}")
    private String macAlgorithm;

    @Bean
    TokenService tokenServiceBackend() {
        SignedTokenServiceBackend backend = new SignedTokenServiceBackend();
        backend.setSecureRandom(new SecureRandom());
        backend.setServerSecret(serverSecret);
        backend.setServerInteger(13);
        return backend;
    }
}
