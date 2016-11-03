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

/**
 * This is a principal which is used for credentials by one of user identifiers like login, email or phone.
 */
public class UserCompositePrincipal implements UserIdentifiers {

    public static class Builder implements MutableUserIdentifiers {
        private String username;
        private String email;

        @Override
        public String getUsername() {
            return username;
        }

        public Builder username(String username) {
            setUsername(username);
            return this;
        }

        @Override
        public void setUsername(String username) {
            this.username = username;
        }

        @Override
        public String getEmail() {
            return email;
        }

        public Builder email(String email) {
            setEmail(email);
            return this;
        }

        @Override
        public void setEmail(String email) {
            this.email = email;
        }

        public UserCompositePrincipal build() {
            return new UserCompositePrincipal(this);
        }
    }

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

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserCompositePrincipal)) {
            return false;
        }

        UserCompositePrincipal that = (UserCompositePrincipal) o;

        if (email != null ? !email.equals(that.email) : that.email != null) {
            return false;
        }
        if (username != null ? !username.equals(that.username) : that.username != null) {
            return false;
        }


        return true;
    }

    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + (email != null ? email.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserCompositePrincipal{" +
          "username='" + username + '\'' +
          ", email='" + email + '\'' +
          '}';
    }
}
