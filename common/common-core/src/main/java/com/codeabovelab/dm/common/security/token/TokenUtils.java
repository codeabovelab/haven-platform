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
 * Utilites for token.
 */
public final class TokenUtils {
    public static String getTypeFromKey(String key) {
        if(key == null || key.isEmpty()) {
            throw new TokenException("Token is null or empty");
        }
        int i = key.indexOf(":");
        return key.substring(0, i);
    }

    public static String getTokenFromKey(String key) {
        if(key == null || key.isEmpty()) {
            throw new TokenException("Token is null or empty");
        }
        int i = key.indexOf(":");
        return key.substring(i + 1);
    }

    public static String getKeyWithTypeAndToken(String type, String token) {
        return type + ":" + token;
    }
}
