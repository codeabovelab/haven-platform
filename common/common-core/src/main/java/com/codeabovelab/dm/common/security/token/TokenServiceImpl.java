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


/**
 * Default implementation of token service.
 */
public class TokenServiceImpl implements TokenService {

    private TokenService backend;

    public TokenService getBackend() {
        return backend;
    }

    public void setBackend(TokenService backend) {
        this.backend = backend;
    }

    @Override
    public TokenData createToken(TokenConfiguration config) {
        return this.backend.createToken(config);
    }

    @Override
    public TokenData getToken(String token) {
        return this.backend.getToken(token);
    }

    @Override
    public void removeToken(String token) {
        this.backend.removeToken(token);
    }

    @Override
    public void removeUserTokens(String userName) {
        backend.removeUserTokens(userName);
    }

}
