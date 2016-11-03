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

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * composite permission evaluator <p/>
 *
 */
public class CompositePermissionEvaluator implements PermissionEvaluator {

    public static final class Builder {
        private final Map<String, PermissionEvaluator> map = new HashMap<>();
        private PermissionEvaluator defaulEvaluator;

        public Map<String, PermissionEvaluator> getMap() {
            return map;
        }

        /**
         * add type specific permission evaluator
         * @param type
         * @param permissionEvaluator
         * @return
         */
        public Builder add(Class<?> type, PermissionEvaluator permissionEvaluator) {
            add(type.getName(), permissionEvaluator);
            return this;
        }

        /**
         * add type specific permission evaluator
         * @param type
         * @param permissionEvaluator
         * @return
         */
        public Builder add(String type, PermissionEvaluator permissionEvaluator) {
            map.put(type, permissionEvaluator);
            return this;
        }

        /**
         * default permissionEvaluator
         * @return
         */
        public PermissionEvaluator getDefaulEvaluator() {
            return defaulEvaluator;
        }

        /**
         * default permissionEvaluator
         * @param defaulEvaluator
         * @return
         */
        public Builder defaulEvaluator(PermissionEvaluator defaulEvaluator) {
            this.setDefaulEvaluator(defaulEvaluator);
            return this;
        }

        /**
         * default permissionEvaluator
         * @param defaulEvaluator
         */
        public void setDefaulEvaluator(PermissionEvaluator defaulEvaluator) {
            this.defaulEvaluator = defaulEvaluator;
        }

        public CompositePermissionEvaluator build() {
            return new CompositePermissionEvaluator(this);
        }
    }

    private final PermissionEvaluator defaulEvaluator;
    private final Map<String, PermissionEvaluator> map;

    private CompositePermissionEvaluator(Builder b) {
        this.defaulEvaluator = b.defaulEvaluator;
        Assert.notNull(this.defaulEvaluator, "defaultEvaluator is null");
        this.map = Collections.unmodifiableMap(new HashMap<>(b.map));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        final String name = targetDomainObject.getClass().getName();
        PermissionEvaluator permissionEvaluator = map.get(name);
        if(permissionEvaluator == null) {
            permissionEvaluator = this.defaulEvaluator;
        }
        return permissionEvaluator.hasPermission(authentication, targetDomainObject, permission);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        PermissionEvaluator permissionEvaluator = map.get(targetType);
        if(permissionEvaluator == null) {
            permissionEvaluator = this.defaulEvaluator;
        }
        return permissionEvaluator.hasPermission(authentication, targetId, targetType, permission);
    }
}
