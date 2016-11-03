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

import com.codeabovelab.dm.common.security.acl.AccessControlEntryImpl;
import com.codeabovelab.dm.common.security.acl.MutableAclImpl;
import com.codeabovelab.dm.security.entity.EntryEntity;
import com.codeabovelab.dm.security.entity.ObjectIdentityEntity;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.DefaultPermissionFactory;
import org.springframework.security.acls.domain.PermissionFactory;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * jpa-based acl LookupStrategy implementation with tenancy supporting
 */
class JpaLookupStrategy implements LookupStrategy {
    
    private final SidsService sidsService; 
    private final ObjectIdentityService objectIdentityService; 
    private final AclAuthorizationStrategy aclAuthorizationStrategy;
    private final PermissionGrantingStrategy permissionGrantingStrategy;
    private final PermissionFactory permissionFactory = new DefaultPermissionFactory();

    public JpaLookupStrategy(ObjectIdentityService objectIdentityService, 
            AclAuthorizationStrategy aclAuthorizationStrategy, 
            PermissionGrantingStrategy permissionGrantingStrategy,
            SidsService sidsService) {
        this.objectIdentityService = objectIdentityService;
        this.aclAuthorizationStrategy = aclAuthorizationStrategy;
        this.permissionGrantingStrategy = permissionGrantingStrategy;
        this.sidsService = sidsService;
    }
    
    @Override
    public Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> objects, List<Sid> sids) {
        final Map<ObjectIdentity, Acl> map = new HashMap<>();
        for(ObjectIdentity identity: objects) {
            ObjectIdentityEntity entity = objectIdentityService.getByIdentity(identity);
            toAcl(map, entity);
        }
        return map;
    }

    private Acl toAcl(Map<ObjectIdentity, Acl> acls, ObjectIdentityEntity entity) {
        if(entity == null) {
            return null;
        }
        ObjectIdentity objectIdentity = Utils.toIdentity(entity);
        Acl acl = acls.get(objectIdentity);
        if(acl != null) {
            return acl;
        }
        Acl parent = toAcl(acls, entity.getParentObject());
        Sid owner = sidsService.toSid(entity.getOwner());
        MutableAclImpl.Builder aclImpl = new MutableAclImpl.Builder()
                .objectIdentity(objectIdentity)
                .id(entity.getId())
                .aclAuthorizationStrategy(aclAuthorizationStrategy)
                .permissionGrantingStrategy(permissionGrantingStrategy)
                .parentAcl(parent)
                .entriesInheriting(entity.isEntriesInheriting())
                .owner(owner);
        readAce(entity, aclImpl);
        acl = aclImpl.build();
        acls.put(objectIdentity, acl);
        return acl;
    }

    private void readAce(ObjectIdentityEntity identityEntity, MutableAclImpl.Builder acl) {
        final List<EntryEntity> identityEntries = identityEntity.getEntries();
        if(identityEntries == null) {
            return;
        }
        for(int i = 0; i < identityEntries.size(); ++i) {
            EntryEntity entryEntity = identityEntries.get(i);
            final Permission permission = permissionFactory.buildFromMask(entryEntity.getMask());
            final Sid sid = sidsService.toSid(entryEntity.getSid());
            acl.addAce(new AccessControlEntryImpl.Builder()
                    .id(entryEntity.getId())
                    .sid(sid)
                    .permission(permission)
                    .granting(entryEntity.isGranting()));
        }
    }

    
}
