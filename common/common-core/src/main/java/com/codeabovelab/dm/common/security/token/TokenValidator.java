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
 * Token verification service
 */
public interface TokenValidator {

    /**
     * Get specified token if it exists and valid (expiration also checked there). <p/>
     * If token has invalid format then exception will be thrown.
     * @param token
     * @param deviceHash {@link TokenData#getDeviceHash() Hash of device}. It
     *                   used for binding token to specific device, if token created without binding with device
     *                   then leave this property null.
     * @return
     */
    TokenData verifyToken(String token, String deviceHash) throws TokenException;

    /**
     * Get specified token if it exists and valid (expiration also checked there). <p/>
     * If token has invalid format then exception will be thrown.
     * @param token
     * @return
     */
    TokenData verifyToken(String token) throws TokenException;

}
