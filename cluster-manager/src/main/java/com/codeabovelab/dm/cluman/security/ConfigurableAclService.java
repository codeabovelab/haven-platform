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

package com.codeabovelab.dm.cluman.security;

import com.codeabovelab.dm.common.security.acl.AclImpl;
import com.codeabovelab.dm.common.security.acl.AclSource;
import com.codeabovelab.dm.common.security.acl.AclUtils;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import org.springframework.security.acls.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class ConfigurableAclService extends AbstractAclService {

    @Data
    public static class Builder {
        private PermissionGrantingStrategy permissionGrantingStrategy;
        private final Map<String, AclSource> acls = new HashMap<>();

        public Builder putAcl(AclSource acl) {
            String id = AclUtils.toId(acl.getObjectIdentity());
            return putAcl(id, acl);
        }

        public Builder putAcl(String id, AclSource acl) {
            this.acls.put(id, acl);
            return this;
        }

        public Builder acls(Map<String, AclSource> acls) {
            setAcls(acls);
            return this;
        }

        public void setAcls(Map<String, AclSource> acls) {
            this.acls.clear();
            this.acls.putAll(acls);
        }

        public Builder permissionGrantingStrategy(PermissionGrantingStrategy permissionGrantingStrategy) {
            setPermissionGrantingStrategy(permissionGrantingStrategy);
            return this;
        }

        public ConfigurableAclService build() {
            return new ConfigurableAclService(this);
        }
    }

    private final PermissionGrantingStrategy premissionGrantingStrategy;
    private final Map<String, AclSource> acls;

    private ConfigurableAclService(Builder builder) {
        this.premissionGrantingStrategy = builder.permissionGrantingStrategy;
        this.acls = ImmutableMap.copyOf(builder.acls);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Acl readAclById(ObjectIdentity object, List<Sid> sids) throws NotFoundException {
        AclSource acl = getAclSource(object);
        return new AclImpl(this.premissionGrantingStrategy, acl);
    }

    @Override
    public AclSource getAclSource(ObjectIdentity object) {
        String id = AclUtils.toId(object);
        AclSource acl = acls.get(id);
        if(acl == null) {
            // if we do not have object acl, then we use per type acl
            acl = acls.get(AclUtils.toTypeId(object));
        }
        if(acl == null) {
            throw new NotFoundException("Acl not found for: " + id);
        }
        return acl;
    }
}
