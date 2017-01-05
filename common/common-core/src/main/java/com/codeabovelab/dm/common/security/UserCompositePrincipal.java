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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;

/**
 * This is a principal which is used for credentials by one of user identifiers like login, email or phone.
 */
@Data
public class UserCompositePrincipal implements UserIdentifiers {
    private final String username;
    private final String email;
    @JsonCreator
    public UserCompositePrincipal(Builder b) {
        this.username = b.username;
        this.email = b.email;
        SecurityUtils.validate(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Data
    public static class Builder implements MutableUserIdentifiers {
        private String username;
        private String email;

        public Builder username(String username) {
            setUsername(username);
            return this;
        }

        public Builder email(String email) {
            setEmail(email);
            return this;
        }

        public UserCompositePrincipal build() {
            return new UserCompositePrincipal(this);
        }
    }

}
