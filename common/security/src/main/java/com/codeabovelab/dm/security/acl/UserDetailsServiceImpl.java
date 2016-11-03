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

package com.codeabovelab.dm.security.acl;

import com.codeabovelab.dm.common.security.ExtendedUserDetails;
import com.codeabovelab.dm.common.security.ExtendedUserDetailsImpl;
import com.codeabovelab.dm.common.security.UserIdentifiers;
import com.codeabovelab.dm.common.security.UserIdentifiersDetailsService;
import com.codeabovelab.dm.security.SecurityUtils;
import com.codeabovelab.dm.security.entity.Authority;
import com.codeabovelab.dm.security.entity.UserAuthDetails;
import com.codeabovelab.dm.security.repository.AuthorityRepository;
import com.codeabovelab.dm.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Set;

public class UserDetailsServiceImpl implements UserIdentifiersDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Override
    public Collection<ExtendedUserDetails> getUsers() {
        return null;
    }

    @Override
    public ExtendedUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAuthDetails user = userRepository.findByUsername(username);
        if(user == null) {
            try {// we also support phone num and email login
                user = userRepository.findByMobile(Long.valueOf(username));
            } catch (NumberFormatException e ) {// if throws NumberFormatException , username is not phone num, maybe is email
                user = userRepository.findByEmail(username);
            }
        }

        if(user == null) {
            throw new UsernameNotFoundException(username);
        }

        ExtendedUserDetailsImpl.Builder builder = ExtendedUserDetailsImpl.from(user);
        fill(user.getId(), builder);
        return builder.build();
    }

    @Override
    public ExtendedUserDetails loadUserByIdentifiers(UserIdentifiers identifiers) {
        UserAuthDetails user = null;
        com.codeabovelab.dm.common.security.SecurityUtils.validate(identifiers);
        final String username = identifiers.getUsername();
        final String email = identifiers.getEmail();
        if(StringUtils.hasText(username)) {
            user = userRepository.findByUsername(username);
        } else if(StringUtils.hasText(email)) {
            user = userRepository.findByEmail(email);
        }
        if(user == null) {
            throw new UsernameNotFoundException(identifiers.toString());
        }
        ExtendedUserDetailsImpl.Builder builder = ExtendedUserDetailsImpl.from(user);
        fill(user.getId(), builder);
        return builder.build();
    }

    private void fill(Long userId, ExtendedUserDetailsImpl.Builder builder) {
        Set<Authority> authorities = authorityRepository.findAdditionalUserAuthorities(userId);
        builder.getAuthorities().addAll(SecurityUtils.convert(authorities));
    }
}
