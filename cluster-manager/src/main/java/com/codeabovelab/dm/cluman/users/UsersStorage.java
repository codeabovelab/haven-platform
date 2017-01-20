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

import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.codeabovelab.dm.common.kv.KvUtils;
import com.codeabovelab.dm.common.kv.mapping.KvMap;
import com.codeabovelab.dm.common.kv.mapping.KvMapAdapter;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.common.security.ExtendedUserDetails;
import com.codeabovelab.dm.common.security.ExtendedUserDetailsImpl;
import com.codeabovelab.dm.common.security.UserIdentifiers;
import com.codeabovelab.dm.common.security.UserIdentifiersDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UsersStorage implements UserIdentifiersDetailsService {

    private final KvMap<UserRegistration> map;
    private final AccessDecisionManager adm;

    @Autowired
    public UsersStorage(KvMapperFactory mapperFactory, AccessDecisionManager accessDecisionManager) {
        this.adm = accessDecisionManager;
        String prefix = KvUtils.join(mapperFactory.getStorage().getPrefix(), "users");
        this.map = KvMap.builder(UserRegistration.class, ExtendedUserDetailsImpl.class)
          .mapper(mapperFactory)
          .path(prefix)
          .passDirty(true)
          .adapter(new KvMapAdapterImpl())
          .build();
    }

    @PostConstruct
    public void init() {
        load();
    }

    private void load() {
        map.load();
    }

    public UserRegistration remove(String name) {
        return map.remove(name);
    }

    /**
     * Create if user absent, if user update end with null userDetails, then registration will be removed.
     * @param name
     * @param consumer
     * @return updated registration
     */
    public UserRegistration update(String name, Consumer<UserRegistration> consumer) {
        return map.compute(name, (k, ur) -> {
            if(ur == null) {
                ur = new UserRegistration(this, k);
            }
            ur.update(consumer);
            //when update is end without details
            if (ur.getDetails() == null) {
                // we remove it
                return null;
            }
            return ur;
        });
    }

    public UserRegistration get(String name) {
        ExtendedAssert.matchAz09Hyp(name, "user name");
        return map.get(name);
    }

    @Override
    public Collection<ExtendedUserDetails> getUsers() {
        Collection<UserRegistration> values = map.values();
        return values.stream().map(UserRegistration::getDetails).collect(Collectors.toList());
    }

    @Override
    public ExtendedUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserRegistration ur = this.map.get(username);
        if (ur == null) {
            return null;
        }
        return ur.getDetails();
    }

    @Override
    public ExtendedUserDetails loadUserByIdentifiers(UserIdentifiers identifiers) {
        UserRegistration ur= this.map.values().stream()
          .filter(u -> u.match(identifiers))
          .findFirst()
          .orElseThrow(() -> new UsernameNotFoundException("Identifiers: " + identifiers));
        return ur.getDetails();
    }

    KvMap<UserRegistration> getMap() {
        return map;
    }

    AccessDecisionManager getAdm() {
        return adm;
    }

    private class KvMapAdapterImpl implements KvMapAdapter<UserRegistration> {
        @Override
        public Object get(String key, UserRegistration source) {
            return source.getDetails();
        }

        @Override
        public UserRegistration set(String key, UserRegistration source, Object value) {
            if(source == null) {
                source = new UserRegistration(UsersStorage.this, key);
            }
            source.loadDetails((ExtendedUserDetails) value);
            return source;
        }
    }
}
