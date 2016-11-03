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

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;

/**
 * Service which allow us to load user by name, or by its identifiers.
 */
public interface UserIdentifiersDetailsService extends UserDetailsService {

    Collection<ExtendedUserDetails> getUsers();

    ExtendedUserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

    /**
     *
     * @param identifiers
     * @throws UsernameNotFoundException if the user could not be found or the user has no
     * GrantedAuthority
     * @return user details
     */
    ExtendedUserDetails loadUserByIdentifiers(UserIdentifiers identifiers);
}
