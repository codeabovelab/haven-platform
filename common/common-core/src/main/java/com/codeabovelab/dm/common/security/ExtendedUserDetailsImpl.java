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

import com.codeabovelab.dm.common.utils.Comparables;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

/**
 * and implementation of user details with tenant support
 *
 */
@Cacheable
public class ExtendedUserDetailsImpl implements ExtendedUserDetails, Comparable<UserDetails> {

    public static class Builder implements ExtendedUserDetails {
        private String tenant;
        private final Set<GrantedAuthority> authorities = new HashSet<>();
        private String password;
        private String username;
        private boolean accountNonExpired = true;
        private boolean credentialsNonExpired = true;
        private boolean accountNonLocked = true;
        private boolean enabled = true;
        private String email;
        private String title;

        public Builder from(UserDetails other) {
            if(other == null) {
                return this;
            }
            if(other instanceof ExtendedUserDetails) {
                ExtendedUserDetails eo = (ExtendedUserDetails) other;
                setTenant(eo.getTenant());
                setEmail(eo.getEmail());
                setTitle(eo.getTitle());
            } else {
                this.setTenant(MultiTenancySupport.getTenant(other));
            }
            setAuthorities(other.getAuthorities());
            setPassword(other.getPassword());
            setUsername(other.getUsername());
            setAccountNonExpired(other.isAccountNonExpired());
            setCredentialsNonExpired(other.isCredentialsNonExpired());
            setAccountNonLocked(other.isAccountNonLocked());
            setEnabled(other.isEnabled());
            return this;
        }

        @Override
        public String getTenant() {
            return tenant;
        }

        public Builder tenant(String tenant) {
            setTenant(tenant);
            return this;
        }

        public void setTenant(String tenant) {
            this.tenant = tenant;
        }

        @Override
        public Collection<GrantedAuthority> getAuthorities() {
            return authorities;
        }

        public Builder addAuthority(GrantedAuthority authority) {
            this.authorities.add(GrantedAuthorityImpl.convert(authority));
            return this;
        }

        public Builder authorities(Collection<? extends GrantedAuthority> authorities) {
            setAuthorities(authorities);
            return this;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
            this.authorities.clear();
            if(authorities != null) {
                this.authorities.addAll(authorities.stream().map(GrantedAuthorityImpl::convert).collect(Collectors.toList()));
            }
        }

        @Override
        public String getPassword() {
            return password;
        }

        public Builder password(String password) {
            setPassword(password);
            return this;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        public String getUsername() {
            return username;
        }

        public Builder username(String username) {
            setUsername(username);
            return this;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        @Override
        public boolean isAccountNonExpired() {
            return accountNonExpired;
        }

        public Builder accountNonExpired(boolean accountNonExpired) {
            setAccountNonExpired(accountNonExpired);
            return this;
        }

        public void setAccountNonExpired(boolean accountNonExpired) {
            this.accountNonExpired = accountNonExpired;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return credentialsNonExpired;
        }

        public Builder credentialsNonExpired(boolean credentialsNonExpired) {
            setCredentialsNonExpired(credentialsNonExpired);
            return this;
        }

        public void setCredentialsNonExpired(boolean credentialsNonExpired) {
            this.credentialsNonExpired = credentialsNonExpired;
        }

        @Override
        public boolean isAccountNonLocked() {
            return accountNonLocked;
        }

        public Builder accountNonLocked(boolean accountNonLocked) {
            setAccountNonLocked(accountNonLocked);
            return this;
        }

        public void setAccountNonLocked(boolean accountNonLocked) {
            this.accountNonLocked = accountNonLocked;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        public Builder enabled(boolean enabled) {
            setEnabled(enabled);
            return this;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public String getEmail() {
            return email;
        }

        public Builder email(String email) {
            setEmail(email);
            return this;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public String getTitle() {
            return title;
        }

        public Builder title(String title) {
            setTitle(title);
            return this;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public ExtendedUserDetailsImpl build() {
            return new ExtendedUserDetailsImpl(this);
        }
    }

    private final String tenant;
    private final Set<GrantedAuthority> authorities;
    private final String password;
    private final String username;
    private final boolean accountNonExpired;
    private final boolean credentialsNonExpired;
    private final boolean accountNonLocked;
    private final boolean enabled;
    private final String email;
    private final String title;

    @JsonCreator
    public ExtendedUserDetailsImpl(Builder builder) {
        this.tenant = builder.tenant;
        this.authorities = Collections.unmodifiableSet(new HashSet<>(builder.authorities));
        this.password = builder.password;
        this.username = builder.username;
        this.accountNonExpired = builder.accountNonExpired;
        this.credentialsNonExpired = builder.credentialsNonExpired;
        this.accountNonLocked = builder.accountNonLocked;
        this.enabled = builder.enabled;
        this.email = builder.email;
        this.title = builder.title;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * copy all fields from specified UserDetails
     * @param other
     * @return
     */
    public static Builder builder(UserDetails other) {
        final Builder builder = builder();
        builder.from(other);
        return builder;
    }

    public static ExtendedUserDetailsImpl from(UserDetails details) {
        if(details instanceof ExtendedUserDetailsImpl) {
            return (ExtendedUserDetailsImpl) details;
        }
        return ExtendedUserDetailsImpl.builder(details).build();
    }

    @Override
    public String getTenant() {
        return tenant;
    }

    @Override
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtendedUserDetailsImpl that = (ExtendedUserDetailsImpl) o;
        return Objects.equals(tenant, that.tenant) &&
                Objects.equals(username, that.username) &&
                Objects.equals(email, that.email) &&
                Objects.equals(title, that.title) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenant, username, email, title);
    }

    @Override
    public String toString() {
        return "UserDetailsImpl{" +
                "username='" + username + '\'' +
                ", tenant=" + tenant +
                ", email='" + email + '\'' +
                ", authorities=" + authorities +
                ", accountNonExpired=" + accountNonExpired +
                ", credentialsNonExpired=" + credentialsNonExpired +
                ", accountNonLocked=" + accountNonLocked +
                ", enabled=" + enabled +
                ", title=" + title +
                '}';
    }

    @Override
    public int compareTo(UserDetails o) {
        return Comparables.compare(this.username, o.getUsername());
    }
}
