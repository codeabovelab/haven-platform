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

package com.codeabovelab.dm.common.security.acl;

import com.google.common.collect.ImmutableList;
import org.springframework.security.acls.model.*;
import org.springframework.util.Assert;

import java.util.List;

/**
 */
public class AclImpl implements Acl {

    private final List<AccessControlEntry> entries;
    private final ObjectIdentity objectIdentity;
    private final Sid owner;
    private final Acl parentAcl;
    private final boolean entriesInheriting;
    private transient PermissionGrantingStrategy permissionGrantingStrategy;

    public AclImpl(PermissionGrantingStrategy permissionGrantingStrategy, AclSource b) {

        this.owner = b.getOwner();
        Assert.notNull(this.owner, "Owner required");
        this.objectIdentity = b.getObjectIdentity();
        Assert.notNull(this.objectIdentity, "Object Identity required");
        AclSource parentAcl = b.getParentAcl();
        if(parentAcl != null) {
            this.parentAcl = new AclImpl(permissionGrantingStrategy, parentAcl);
        } else {
            this.parentAcl = null;
        }
        this.entriesInheriting = b.isEntriesInheriting();
        this.permissionGrantingStrategy = permissionGrantingStrategy;
        Assert.notNull(this.permissionGrantingStrategy, "permissionGrantingStrategy required");

        ImmutableList.Builder<AccessControlEntry> entriesBuilder = ImmutableList.builder();
        AclUtils.buildEntries(this, b.getEntries(), entriesBuilder::add);
        this.entries = entriesBuilder.build();
    }

    @Override
    public List<AccessControlEntry> getEntries() {
        return entries;
    }

    @Override
    public ObjectIdentity getObjectIdentity() {
        return objectIdentity;
    }

    @Override
    public Sid getOwner() {
        return owner;
    }

    @Override
    public Acl getParentAcl() {
        return parentAcl;
    }

    @Override
    public boolean isEntriesInheriting() {
        return entriesInheriting;
    }

    @Override
    public boolean isGranted(List<Permission> permission, List<Sid> sids, boolean administrativeMode) throws NotFoundException, UnloadedSidException {
        Assert.notEmpty(permission, "Permissions required");
        Assert.notEmpty(sids, "SIDs required");

        if (!this.isSidLoaded(sids)) {
            throw new UnloadedSidException("ACL was not loaded for one or more SID");
        }

        return permissionGrantingStrategy.isGranted(this, permission, sids, administrativeMode);
    }

    @Override
    public boolean isSidLoaded(List<Sid> sids) {
        return true;
    }
}
