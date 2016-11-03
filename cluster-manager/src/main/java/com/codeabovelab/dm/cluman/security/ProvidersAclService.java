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
import org.springframework.security.acls.model.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.UnaryOperator;

/**
 */
public class ProvidersAclService extends AbstractAclService {

    private final PermissionGrantingStrategy pgs;
    private final ConcurrentMap<String, AclProvider> providers = new ConcurrentHashMap<>();

    public ProvidersAclService(PermissionGrantingStrategy permissionGrantingStrategy) {
        this.pgs = permissionGrantingStrategy;
    }

    public Map<String, AclProvider> getProviders() {
        return providers;
    }

    @Override
    public Acl readAclById(ObjectIdentity oid, List<Sid> list) throws NotFoundException {
        AclSource source = getAclSource(oid);
        return new AclImpl(this.pgs, source);
    }

    @Override
    public AclSource getAclSource(ObjectIdentity oid) {
        AclProvider provider = getAclProvider(oid);
        AclSource source = provider.provide(oid.getIdentifier());
        if(source == null) {
            throw new NotFoundException("Can not find acl for id : " + oid);
        }
        return source;
    }

    private AclProvider getAclProvider(ObjectIdentity oid) {
        AclProvider provider = providers.get(oid.getType());
        if(provider == null) {
            throw new NotFoundException("Can not find AclProvider for type : " + oid.getType());
        }
        return provider;
    }

    public void updateAclSource(ObjectIdentity oid, UnaryOperator<AclSource> modifier) {
        AclProvider provider = getAclProvider(oid);
        provider.update(oid.getIdentifier(), modifier);
    }
}
