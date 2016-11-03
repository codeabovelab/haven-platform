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
 * The representation of token.
 */
public interface TokenData {
    String getUserName();
    String getDeviceHash();

    /**
     * type of token, we can support many types of token.
     * @see com.codeabovelab.dm.platform.users.token.TokenUtils#getTypeFromKey(String)
     * @return
     */
    String getType();

    String getKey();

    long getCreationTime();
}
