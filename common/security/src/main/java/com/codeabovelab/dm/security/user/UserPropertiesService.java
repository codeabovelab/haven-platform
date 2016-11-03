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

package com.codeabovelab.dm.security.user;

import com.codeabovelab.dm.common.utils.AttributeSupport;
import com.codeabovelab.dm.security.entity.UserAuthDetails;
import com.codeabovelab.dm.security.entity.UserProperty;
import com.codeabovelab.dm.security.repository.UserPropertyRepository;
import com.codeabovelab.dm.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.transaction.Transactional;
import java.util.*;

/**
 */
@Component
public class UserPropertiesService {

    @Autowired
    private UserPropertyRepository upRepo;

    @Autowired
    private UserRepository userRepository;

    /**
     *
     * @param username
     * @param patch contains map of key-value. If key is present, but value is null - then property will be removed  from repository
     */
    @Transactional
    public void update(String username, Map<String, String> patch) {

        UserAuthDetails userAuthDetails = userRepository.findByUsername(username);
        Assert.notNull(userAuthDetails, "can not find user with name: " + username);
        AdapterImpl adapter = new AdapterImpl(userAuthDetails);
        AttributeSupport.merge(adapter, adapter, patch);
    }

    public Map<String, String> get(String username) {
        Map<String, String> map = new HashMap<>();
        List<UserProperty> properties = upRepo.getPropertiesByUsername(username);
        if(properties != null) {
            for(UserProperty property: properties) {
                map.put(property.getName(), property.getData());
            }
        }
        return map;
    }

    private class AdapterImpl implements AttributeSupport.Adapter<UserProperty>, AttributeSupport.Repository<UserProperty>  {
        private final UserAuthDetails details;

        public AdapterImpl(UserAuthDetails userAuthDetails) {
            this.details = userAuthDetails;
        }

        @Override
        public UserProperty create() {
            UserProperty property = new UserProperty();
            property.setUserAuthDetails(details);
            return property;
        }

        @Override
        public void setKey(UserProperty attr, String key) {
            attr.setName(key);
        }

        @Override
        public String getKey(UserProperty attr) {
            return attr.getName();
        }

        @Override
        public void setValue(UserProperty attr, String value) {
            attr.setData(value);
        }

        @Override
        public String getValue(UserProperty attr) {
            return attr.getData();
        }

        @Override
        public List<UserProperty> getAttributes() {
            return upRepo.getPropertiesByUsername(details.getUsername());
        }

        @Override
        public <S extends UserProperty> List<S> save(Iterable<S> entities) {
            return upRepo.save(entities);
        }

        @Override
        public void delete(Iterable<? extends UserProperty> entities) {
            upRepo.delete(entities);
        }
    }
}
