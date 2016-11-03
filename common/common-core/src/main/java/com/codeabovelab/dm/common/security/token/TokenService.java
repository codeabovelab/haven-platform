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
 * Service which manages tokens.
 * Token can be identified by it {@link TokenData#getKey()} value.
 */
public interface TokenService {
    /**
     * Create and persist new token.
     * @param config
     * @return
     */
    TokenData createToken(TokenConfiguration config);

    /**
     * Get specified token if it exists and valid (expiration not checked there).
     * If token not valid then throw exception.
     * @param token
     * @return
     */
    TokenData getToken(String token) throws TokenException;

    /**
     * Remove specified token.
     * @param token
     */
    void removeToken(String token);

    /**
     * Remove all user tokens.
     * @param userName
     */
    void removeUserTokens(String userName);
}
