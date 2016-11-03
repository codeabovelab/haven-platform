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

import com.codeabovelab.dm.common.security.TenantPrincipalSid;
import com.codeabovelab.dm.security.entity.ClassEntity;
import com.codeabovelab.dm.security.entity.EntryEntity;
import com.codeabovelab.dm.security.entity.ObjectIdentityEntity;
import com.codeabovelab.dm.security.entity.SidEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * jpa-based AclService
 *
 */
class MutableJpaAclService implements AclService, MutableAclService {

    @Autowired
    private ObjectIdentityService objectIdentityService;
    @Autowired
    private LookupStrategy lookupStrategy;
    @Autowired
    private SidsService sidsService;
    @Autowired
    @Lazy
    private ClassesService classesService;

    MutableJpaAclService() {
    }

    @Override
    public List<ObjectIdentity> findChildren(ObjectIdentity parentIdentity) {
        final List<ObjectIdentityEntity> childEntites = objectIdentityService.getByParentIdentity(parentIdentity);
        List<ObjectIdentity> childIdentities = new ArrayList<>(childEntites.size());
        childIdentities.addAll(childEntites.stream().map(Utils::toIdentity).collect(Collectors.toList()));
        return childIdentities;
    }

    @Override
    public Acl readAclById(ObjectIdentity object) throws NotFoundException {
        return readAclById(object, null);
    }

    @Override
    public Acl readAclById(ObjectIdentity object, List<Sid> sids) throws NotFoundException {
        final Map<ObjectIdentity, Acl> map = readAclsById(Collections.singletonList(object), sids);
        Assert.isTrue(map.containsKey(object), "There should have been an Acl entry for ObjectIdentity " + object);
        return map.get(object);
    }

    @Override
    public Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> objects) throws NotFoundException {
        return readAclsById(objects, null);
    }

    @Override
    public Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> objects, List<Sid> sids) throws NotFoundException {
        final Map<ObjectIdentity, Acl> map = lookupStrategy.readAclsById(objects, sids);
        //above code copied from JdbcAclService
        // Check every requested object identity was found (throw NotFoundException if needed)
        for (ObjectIdentity oid : objects) {
            if (!map.containsKey(oid)) {
                throw new NotFoundException("Unable to find ACL information for object identity '" + oid + "'");
            }
        }
        return map;
    }

    @Override
    public MutableAcl createAcl(ObjectIdentity objectIdentity) throws AlreadyExistsException {
        Assert.notNull(objectIdentity, "Object Identity required");

        // Check this object identity hasn't already been persisted
        if (objectIdentityService.getIdByIdentity(objectIdentity) != null) {
            throw new AlreadyExistsException("Object identity '" + objectIdentity + "' already exists");
        }

        // Need to retrieve the current principal, in order to know who "owns" this ACL (can be changed later on)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        TenantPrincipalSid sid = new TenantPrincipalSid(auth);

        // Create the acl_object_identity row
        createObjectIdentity(objectIdentity, sid);

        Acl acl = readAclById(objectIdentity);
        Assert.isInstanceOf(MutableAcl.class, acl, "MutableAcl should be been returned");

        return (MutableAcl) acl;
    }

    @Override
    public void deleteAcl(ObjectIdentity objectIdentity, boolean deleteChildren) throws ChildrenExistException {
        Assert.notNull(objectIdentity, "Object Identity required");
        Assert.notNull(objectIdentity.getIdentifier(), "Object Identity doesn't provide an identifier");

        if (deleteChildren) {
            List<ObjectIdentity> children = findChildren(objectIdentity);
            if (children != null) {
                for (ObjectIdentity child : children) {
                    deleteAcl(child, true);
                }
            }
        }
        
        objectIdentityService.deleteByIdentity(objectIdentity);
    }

    @Override
    public MutableAcl updateAcl(MutableAcl acl) throws NotFoundException {
        Assert.notNull(acl.getId(), "Object Identity doesn't provide an identifier");
        
        final ObjectIdentity objectIdentity = acl.getObjectIdentity();

        final ObjectIdentityEntity entity = objectIdentityService.getByIdentity(objectIdentity);
        
        entity.setEntriesInheriting(acl.isEntriesInheriting());
        entity.setOwner(sidsService.getOrCreate(acl.getOwner()));
        
        updateEntries(entity, acl);
        
        final Acl parentAcl = acl.getParentAcl();
        if(parentAcl != null) {
            final ObjectIdentityEntity parentEntity = objectIdentityService.getByIdentity(parentAcl.getObjectIdentity());
            Assert.notNull(parentEntity, "parent entity for parentAcl is null");
            entity.setParentObject(parentEntity);
        }
        
        objectIdentityService.save(entity);

        return (MutableAcl) readAclById(objectIdentity);
    }

    private void updateEntries(ObjectIdentityEntity identityEntity, MutableAcl acl) {
        List<EntryEntity> identityEntries = identityEntity.getEntries();
        final List<AccessControlEntry> aclEntries = acl.getEntries();
        if(identityEntries == null) {
            if(aclEntries.isEmpty()) {
                return;
            }
            identityEntries = new ArrayList<>();
            identityEntity.setEntries(identityEntries);
        } else {
            identityEntries.clear();
        }
        for(int i = 0; i < aclEntries.size(); ++i) {
            AccessControlEntry entry = aclEntries.get(i);
            EntryEntity entryEntity = new EntryEntity();
            entryEntity.setGranting(entry.isGranting());
            entryEntity.setMask(entry.getPermission().getMask());
            entryEntity.setObjectIdentity(identityEntity);
            entryEntity.setOrder(i);
            entryEntity.setSid(sidsService.getOrCreate(entry.getSid()));
            identityEntries.add(entryEntity);
        }
    }

    private void createObjectIdentity(ObjectIdentity object, TenantPrincipalSid owner) {
        //may, have been better to moving this code to ObjectIdentityService
        SidEntity sidEntity = sidsService.getOrCreate(owner);
        ClassEntity classEntity = classesService.getOrCreate(object.getType());
        ObjectIdentityEntity entity = new ObjectIdentityEntity();
        entity.setObjectClass(classEntity);
        entity.setObjectId((long) object.getIdentifier());
        entity.setOwner(sidEntity);
        entity.setEntriesInheriting(true);
        objectIdentityService.save(entity);
    }
}
