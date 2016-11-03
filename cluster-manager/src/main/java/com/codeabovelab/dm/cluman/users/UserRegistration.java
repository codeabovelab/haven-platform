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
import com.codeabovelab.dm.common.security.ExtendedUserDetails;
import com.codeabovelab.dm.common.security.ExtendedUserDetailsImpl;
import com.codeabovelab.dm.common.security.UserIdentifiers;

import java.util.function.Consumer;

/**
 */
public class UserRegistration {
    private final Object lock = new Object();
    @KvMapping
    private ExtendedUserDetailsImpl details;
    private final KvClassMapper<UserRegistration> mapper;
    private final String name;

    UserRegistration(UsersStorage storage, String name) {
        this.mapper = storage.getMapper();
        this.name = name;
    }

    public ExtendedUserDetails getDetails() {
        synchronized (lock) {
            return details;
        }
    }

    public void setDetails(ExtendedUserDetails details) {
        synchronized (lock) {
            this.details = ExtendedUserDetailsImpl.from(details);
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
            if(this.details != null) {
                if(!this.name.equals(this.details.getUsername())) {
                    this.details = ExtendedUserDetailsImpl.builder(this.details).username(name).build();
                }
            }
        }
    }

    public void update(Consumer<UserRegistration> consumer) {
        synchronized (lock) {
            consumer.accept(this);
            mapper.save(name, this);
        }
    }
}
