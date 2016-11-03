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

package com.codeabovelab.dm.common.security.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.codeabovelab.dm.common.security.ExtendedUserDetailsImpl;
import com.codeabovelab.dm.common.security.GrantedAuthorityImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

/**
 * The DTO of Authentication
 *
 */
public class AuthenticationData implements Authentication {

    public static class Builder {

        private boolean authenticated;
        private Object principal;
        private Object details;
        private Object credentials;
        private String name;
        private final Set<GrantedAuthority> authorities = new HashSet<>();

        public Builder() { }

        public boolean isAuthenticated() {
            return authenticated;
        }

        public void setAuthenticated(boolean authenticated) {
            this.authenticated = authenticated;
        }

        public Builder authenticated(boolean authenticated) {
            setAuthenticated(authenticated);
            return this;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Object getPrincipal() {
            return principal;
        }

        public void setPrincipal(Object principal) {
            this.principal = convertPrincipal(principal);
        }

        public Builder principal(Object principal) {
            setPrincipal(principal);
            return this;
        }

        private Object convertPrincipal(Object principal) {
            if(principal instanceof org.springframework.security.core.userdetails.User) {
                return ExtendedUserDetailsImpl.builder((UserDetails) principal).build();
            }
            return principal;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Object getDetails() {
            return details;
        }

        public void setDetails(Object details) {
            this.details = convertDetails(details);
        }

        public Builder details(Object details) {
            setDetails(details);
            return this;
        }

        private Object convertDetails(final Object details) {
            return details;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Object getCredentials() {
            return credentials;
        }

        public void setCredentials(Object credentials) {
            this.credentials = credentials;
        }

        public Builder credentials(Object credentials) {
            setCredentials(credentials);
            return this;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Builder name(Object credentials) {
            setName(name);
            return this;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
            this.authorities.clear();
            if(authorities != null) {
                for(GrantedAuthority authority: authorities) {
                    this.authorities.add(GrantedAuthorityImpl.convert(authority));
                }
            }
        }

        public Builder authorities(Collection<? extends GrantedAuthority> authorities) {
            setAuthorities(authorities);
            return this;
        }

        public AuthenticationData build() {
            return new AuthenticationData(this);
        }
    }

    private final boolean authenticated;
    private final Object principal;
    private final Object details;
    private final Object credentials;
    private final String name;
    private final Set<GrantedAuthority> authorities;

    @JsonCreator
    public AuthenticationData(Builder b) {
        this.authenticated = b.authenticated;
        this.authorities = Collections.unmodifiableSet(new HashSet<>(b.authorities));
        this.credentials = b.credentials;
        this.details = b.details;
        this.principal = b.principal;
        this.name = b.name;
    }

    public static Builder build() {
        return new Builder();
    }

    public static Builder from(Authentication authentication) {
        final Builder build = build();
        build.setAuthenticated(authentication.isAuthenticated());
        build.setPrincipal(authentication.getPrincipal());
        build.setDetails(authentication.getDetails());
        build.setCredentials(authentication.getCredentials());
        build.setName(authentication.getName());
        build.setAuthorities(authentication.getAuthorities());
        return build;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @Override
    public Object getCredentials() {
        return credentials;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @Override
    public Object getDetails() {
        return details;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if(this.authenticated != isAuthenticated) {
            throw new IllegalArgumentException("changing of data is not supported");
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "AuthenticationData{" +
                "authenticated=" + authenticated +
                ", principal=" + principal +
                ", details=" + details +
                ", credentials=" + credentials +
                ", name='" + name + '\'' +
                ", authorities=" + authorities +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationData that = (AuthenticationData) o;
        return Objects.equals(authenticated, that.authenticated) &&
                Objects.equals(credentials, that.credentials) &&
                Objects.equals(name, that.name) &&
                Objects.equals(authorities, that.authorities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authenticated, credentials, name, authorities);
    }


    @JsonProperty(value = "@class")
    private String getClassName() {
        return AuthenticationData.class.getName();
    }

}
