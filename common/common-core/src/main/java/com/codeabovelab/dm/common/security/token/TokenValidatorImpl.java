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

package com.codeabovelab.dm.common.security.token;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;

/**
 * Token verification service
 */
@Builder
@Data
@Slf4j
public class TokenValidatorImpl implements TokenValidator {

    private final Cache cache;
    private final TokenService tokenService;
    private final TokenValidatorSettings settings;

    @Override
    public TokenData verifyToken(String token, String deviceHash) {
        TokenData tokenData;
        try {
            tokenData = this.tokenService.getToken(token);
        } catch (Exception e) {
            log.error("Error due check token", e);
            throw new TokenException("Token is not valid: " + e.getMessage());
        }
        final long currentTime = System.currentTimeMillis();
        Long lastAccess = getLastAccess(tokenData);
        final boolean ttl = (currentTime - tokenData.getCreationTime()) >= (settings.getExpireAfterInSec() * 1000L);
        final boolean tti = lastAccess == null || (currentTime - lastAccess) >= (settings.getExpireLastAccessInSec() * 1000L);
        boolean expired = ttl && tti;
        if(expired) {
            throw new TokenException("Token '" + token + "' is expired.");
        }
        setLastAccess(tokenData, currentTime);
        return tokenData;
    }

    private void setLastAccess(TokenData tokenData, long currentTime) {
        cache.put(tokenData.getKey(), currentTime);
    }

    private Long getLastAccess(TokenData tokenData) {
        Cache.ValueWrapper wrapper = cache.get(tokenData.getKey());
        return wrapper == null? null : (Long) wrapper.get();
    }

    @Override
    public TokenData verifyToken(String token) {
        return verifyToken(token, null);
    }

}
