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

package com.codeabovelab.dm.security.token;

import com.codeabovelab.dm.common.security.token.SignedTokenServiceBackend;
import com.codeabovelab.dm.common.security.token.TokenService;
import com.codeabovelab.dm.security.repository.TokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.codec.Hex;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

/**
 * configuration for token service with backend
 */
@Configuration
public class BackendTokenServiceConfiguration {

    @Value("${token.serverSecret:3153620caaf300c37b345d0c2e8dc3aa322c6d9d}")
    private String serverSecret;

    @Value("${token.macAlgorithm:HmacSHA1}")
    private String macAlgorithm;

    @Bean
    TokenService tokenServiceBackend(TokenRepository tokenRepository) {
        PersistedTokenServiceBackend backend = new PersistedTokenServiceBackend();
        backend.setSecureRandom(new SecureRandom());
        backend.setMacAlgorithm(macAlgorithm);
        backend.setSecret(new SecretKeySpec(Hex.decode(serverSecret), "none"));
        backend.setTokenRepository(tokenRepository);
        return backend;
    }
}
