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

package com.codeabovelab.dm.common.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * Token for for credentials  by name, phone or email.
 */
public class UserCompositeAuthenticationToken extends AbstractAuthenticationToken {

    private final Object credentials;
    private final UserCompositePrincipal principal;

    public UserCompositeAuthenticationToken(UserCompositePrincipal principal, Object credentials) {
        super(null);
        this.principal = principal;
        this.credentials = credentials;
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public UserCompositePrincipal getPrincipal() {
        return this.principal;
    }
}
