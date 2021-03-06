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

package com.codeabovelab.dm.cluman.cluster.registry.model;

import lombok.Data;
import org.springframework.http.HttpHeaders;

/**
 */
public interface RegistryAuthAdapter {
    void handle(AuthContext ctx);

    @Data
    class AuthContext {
        private final HttpHeaders requestHeaders;
        private final HttpHeaders responseHeaders;
        /**
         * "Www-Authenticate" request header value
         */
        private final String authenticate;
    }
}
