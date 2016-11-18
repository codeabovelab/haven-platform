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

package com.codeabovelab.dm.cluman.users;

import com.codeabovelab.dm.common.kv.mapping.KvClassMapper;
import com.codeabovelab.dm.common.kv.mapping.KvMapping;
import com.codeabovelab.dm.common.security.Authorities;
import com.codeabovelab.dm.common.security.ExtendedUserDetails;
import com.codeabovelab.dm.common.security.ExtendedUserDetailsImpl;
import com.codeabovelab.dm.common.security.UserIdentifiers;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;

/**
 */
public class UserRegistration {
    private final Object lock = new Object();
    private final UsersStorage storage;
    @KvMapping
    private ExtendedUserDetailsImpl details;
    private final KvClassMapper<UserRegistration> mapper;
    private final String name;

    UserRegistration(UsersStorage storage, String name) {
        this.storage = storage;
        this.mapper = storage.getMapper();
        this.name = name;
        normalizeDetails();
    }

    public ExtendedUserDetails getDetails() {
        synchronized (lock) {
            return details;
        }
    }

    public void setDetails(ExtendedUserDetails details) {
        synchronized (lock) {
            ExtendedUserDetailsImpl changed = ExtendedUserDetailsImpl.from(details);
            validate(changed);
            if(!details.getAuthorities().equals(changed.getAuthorities())) {
                // change authorities of user can do only global or tenant admin
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                this.storage.getAdm().decide(auth, this.details, Collections.singletonList(Authorities.fromName(Authorities.ADMIN_ROLE, this.details.getTenant())));
            }
            this.details = changed;
        }
    }

    public boolean match(UserIdentifiers identifiers) {
        synchronized (lock) {
            String username = identifiers.getUsername();
            String email = identifiers.getEmail();
            return (username == null || username.equals(name)) &&
              (email == null || email.equals(details.getEmail()));
        }
    }

    void load() {
        synchronized (lock) {
            this.mapper.load(name, this);
            if(details == null || !this.name.equals(this.details.getUsername())) {
                normalizeDetails();
            }
        }
    }

    private void normalizeDetails() {
        this.details = ExtendedUserDetailsImpl.builder(this.details).username(name).build();
    }

    /**
     * Invoke consumer in local lock.
     * @param consumer
     */
    public void update(Consumer<UserRegistration> consumer) {
        synchronized (lock) {
            consumer.accept(this);
            mapper.save(name, this);
        }
    }

    private void validate(ExtendedUserDetails another) {
        if(!this.name.equals(another.getUsername())) {
            throw new IllegalArgumentException("Changing of name (orig:" + this.name
              + ", new:" + another.getUsername() + ") is not allowed.");
        }
        String anotherTenant = another.getTenant();
        if(anotherTenant == null) {
            throw new IllegalArgumentException("tenant is null");
        }
        if(!Objects.equals(this.details.getTenant(), anotherTenant)) {
            throw new IllegalArgumentException("Change of tenant (orig:" + this.details.getTenant()
              + ", new:" + anotherTenant + ") is not allowed.");
        }
    }
}
