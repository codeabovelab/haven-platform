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

import com.codeabovelab.dm.common.kv.KvStorageEvent;
import com.codeabovelab.dm.common.kv.KvUtils;
import com.codeabovelab.dm.common.kv.mapping.KvClassMapper;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.common.security.ExtendedUserDetails;
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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UsersStorage implements UserIdentifiersDetailsService {

    private final KvMapperFactory mapperFactory;
    private final KvClassMapper<UserRegistration> mapper;
    private final ConcurrentMap<String, UserRegistration> map = new ConcurrentHashMap<>();
    private final String prefix;
    private final AccessDecisionManager adm;

    @Autowired
    public UsersStorage(KvMapperFactory mapperFactory, AccessDecisionManager accessDecisionManager) {
        this.mapperFactory = mapperFactory;
        this.adm = accessDecisionManager;
        this.prefix = KvUtils.join(this.mapperFactory.getStorage().getDockMasterPrefix(), "users");
        this.mapper = mapperFactory.createClassMapper(prefix, UserRegistration.class);
    }

    @PostConstruct
    public void init() {
        this.mapperFactory.getStorage().subscriptions().subscribeOnKey(this::onKvEvent, prefix);
        load();
    }

    private void load() {
        List<String> list = mapper.list();
        for(String name: list) {
            internalLoadUser(name);
        }
    }

    private void onKvEvent(KvStorageEvent event) {
        KvStorageEvent.Crud action = event.getAction();
        final String name = KvUtils.name(prefix, event.getKey());
        if(name == null) {// it can be at empty KV
            return;
        }
        switch (action) {
            case UPDATE:
            case CREATE:
                internalLoadUser(name);
                break;
            case DELETE:
                map.remove(name);
                break;
        }
    }

    private UserRegistration internalLoadUser(String name) {
        UserRegistration ur = map.computeIfAbsent(name, s -> new UserRegistration(this, name));
        ur.load();
        return ur;
    }

    /**
     * Create if user absent
     * @param name
     * @param consumer
     */
    public void update(String name, Consumer<UserRegistration> consumer) {
        UserRegistration ur = internalLoadUser(name);
        ur.update(consumer);
    }

    public UserRegistration get(String name) {
        return map.get(name);
    }

    public UserRegistration getOrCreate(String name) {
        return internalLoadUser(name);
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

    KvClassMapper<UserRegistration> getMapper() {
        return mapper;
    }

    public void delete(String username) {
        this.mapper.delete(username);
    }

    AccessDecisionManager getAdm() {
        return adm;
    }
}
