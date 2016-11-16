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

import com.codeabovelab.dm.common.security.ExtendedUserDetails;
import com.codeabovelab.dm.common.security.UserIdentifiers;
import com.codeabovelab.dm.common.security.UserIdentifiersDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 */
@Primary
@Slf4j
@Component
public class CompositeUserDetailsService implements UserIdentifiersDetailsService {
    private List<UserIdentifiersDetailsService> services;

    public CompositeUserDetailsService() {
    }

    @Autowired
    public void setServices(List<UserIdentifiersDetailsService> services) {
        this.services = new ArrayList<>(services);
        this.services.sort(AnnotationAwareOrderComparator.INSTANCE);
    }

    @Override
    public Collection<ExtendedUserDetails> getUsers() {
        Map<String, ExtendedUserDetails> map = new HashMap<>();
        for(UserIdentifiersDetailsService service: services) {
            Collection<ExtendedUserDetails> users = service.getUsers();
            users.forEach(e -> {
                if(e == null) {
                    log.error("Service {} has null in user list.", service);
                    return;
                }
                // note that services has precedence, so we cannot replace details
                map.putIfAbsent(e.getUsername(), e);
            });
        }
        List<ExtendedUserDetails> list = new ArrayList<>(map.values());
        list.sort(null);
        return list;
    }

    @Override
    public ExtendedUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        for(UserIdentifiersDetailsService service: services) {
            try {
                ExtendedUserDetails details = service.loadUserByUsername(username);
                if(details != null) {
                    return details;
                }
            } catch (UsernameNotFoundException e) {
                //suppress
            }
        }
        throw new UsernameNotFoundException("User name: " + username);
    }

    @Override
    public ExtendedUserDetails loadUserByIdentifiers(UserIdentifiers identifiers) {
        for(UserIdentifiersDetailsService service: services) {
            try {
                ExtendedUserDetails details = service.loadUserByIdentifiers(identifiers);
                if(details != null) {
                    return details;
                }
            } catch (UsernameNotFoundException e) {
                //suppress
            }
        }
        throw new UsernameNotFoundException("User identifiers: " + identifiers);
    }
}
