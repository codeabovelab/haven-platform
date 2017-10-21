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

import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.model.*;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * implementation of Acl (based on AclImpl code) which allow initial building without reflection hacks
 */
public class MutableAclImpl implements Acl, MutableAcl, AuditableAcl, OwnershipAcl {
    public static class Builder {

        private Acl parentAcl;
        private transient AclAuthorizationStrategy aclAuthorizationStrategy;
        private transient PermissionGrantingStrategy permissionGrantingStrategy;
        private final List<Object> aces = new ArrayList<>();
        private ObjectIdentity objectIdentity;
        private Serializable id;
        private Sid owner; 
        private List<Sid> loadedSids = null; 
        private boolean entriesInheriting = true;
        
        public Builder() {
        }

        public Acl getParentAcl() {
            return parentAcl;
        }

        public Builder parentAcl(Acl parentAcl) {
            setParentAcl(parentAcl);
            return this;
        }
        
        public void setParentAcl(Acl parentAcl) {
            this.parentAcl = parentAcl;
        }

        public AclAuthorizationStrategy getAclAuthorizationStrategy() {
            return aclAuthorizationStrategy;
        }

        public Builder aclAuthorizationStrategy(AclAuthorizationStrategy aclAuthorizationStrategy) {
            setAclAuthorizationStrategy(aclAuthorizationStrategy);
            return this;
        }
        
        public void setAclAuthorizationStrategy(AclAuthorizationStrategy aclAuthorizationStrategy) {
            this.aclAuthorizationStrategy = aclAuthorizationStrategy;
        }

        public PermissionGrantingStrategy getPermissionGrantingStrategy() {
            return permissionGrantingStrategy;
        }

        public Builder permissionGrantingStrategy(PermissionGrantingStrategy permissionGrantingStrategy) {
            setPermissionGrantingStrategy(permissionGrantingStrategy);
            return this;
        }
        
        public void setPermissionGrantingStrategy(PermissionGrantingStrategy permissionGrantingStrategy) {
            this.permissionGrantingStrategy = permissionGrantingStrategy;
        }

        public ObjectIdentity getObjectIdentity() {
            return objectIdentity;
        }

        public Builder objectIdentity(ObjectIdentity objectIdentity) {
            setObjectIdentity(objectIdentity);
            return this;
        }
        
        public void setObjectIdentity(ObjectIdentity objectIdentity) {
            this.objectIdentity = objectIdentity;
        }

        public Serializable getId() {
            return id;
        }

        public Builder id(Serializable id) {
            setId(id);
            return this;
        }
        
        public void setId(Serializable id) {
            this.id = id;
        }

        public Sid getOwner() {
            return owner;
        }

        public Builder owner(Sid owner) {
            setOwner(owner);
            return this;
        }
        
        public void setOwner(Sid owner) {
            this.owner = owner;
        }

        public List<Sid> getLoadedSids() {
            return loadedSids;
        }

        public Builder loadedSids(List<Sid> loadedSids) {
            setLoadedSids(loadedSids);
            return this;
        }
        
        public void setLoadedSids(List<Sid> loadedSids) {
            this.loadedSids = loadedSids;
        }

        public boolean isEntriesInheriting() {
            return entriesInheriting;
        }

        public Builder entriesInheriting(boolean entriesInheriting) {
            setEntriesInheriting(entriesInheriting);
            return this;
        }
        
        public void setEntriesInheriting(boolean entriesInheriting) {
            this.entriesInheriting = entriesInheriting;
        }
        
        public Builder addAce(AccessControlEntry ace) {
            aces.add(ace);
            return this;
        }
        
        public Builder addAce(AccessControlEntryImpl.Builder ace) {
            aces.add(ace);
            return this;
        }
        
        public MutableAclImpl build() {
            return new MutableAclImpl(this);
        }
    }



    private Acl parentAcl;
    private transient AclAuthorizationStrategy aclAuthorizationStrategy;
    private transient PermissionGrantingStrategy permissionGrantingStrategy;
    private final List<AccessControlEntry> aces = new ArrayList<>();
    private ObjectIdentity objectIdentity;
    private Serializable id;
    private Sid owner; // OwnershipAcl
    private List<Sid> loadedSids = null; // includes all SIDs the WHERE clause covered, even if there was no ACE for a SID
    private boolean entriesInheriting = true;

    /**
     */
    private MutableAclImpl(Builder b) {
        Assert.notNull(b.objectIdentity, "Object Identity required");
        Assert.notNull(b.id, "Id required");
        Assert.notNull(b.aclAuthorizationStrategy, "AclAuthorizationStrategy required");
        Assert.notNull(b.owner, "Owner required");

        this.objectIdentity = b.objectIdentity;
        this.id = b.id;
        this.aclAuthorizationStrategy = b.aclAuthorizationStrategy;
        this.parentAcl = b.parentAcl; // may be null
        this.loadedSids = b.loadedSids; // may be null
        this.entriesInheriting = b.entriesInheriting;
        this.owner = b.owner;
        this.permissionGrantingStrategy = b.permissionGrantingStrategy;
        
        AclUtils.buildEntries(this, b.aces, this.aces::add);
    }

    /**
     * Private no-argument constructor for use by reflection-based persistence
     * tools along with field-level access.
     */
    @SuppressWarnings("unused")
    private MutableAclImpl() {}

    //~ Methods ========================================================================================================

    @Override
    public void deleteAce(int aceIndex) throws NotFoundException {
        aclAuthorizationStrategy.securityCheck(this, AclAuthorizationStrategy.CHANGE_GENERAL);
        verifyAceIndexExists(aceIndex);

        synchronized (aces) {
            this.aces.remove(aceIndex);
        }
    }

    private void verifyAceIndexExists(int aceIndex) {
        if (aceIndex < 0) {
            throw new NotFoundException("aceIndex must be greater than or equal to zero");
        }
        if (aceIndex >= this.aces.size()) {
            throw new NotFoundException("aceIndex must refer to an index of the AccessControlEntry list. " +
                    "List size is " + aces.size() + ", index was " + aceIndex);
        }
    }

    @Override
    public void insertAce(int atIndexLocation, Permission permission, Sid sid, boolean granting) throws NotFoundException {
        aclAuthorizationStrategy.securityCheck(this, AclAuthorizationStrategy.CHANGE_GENERAL);
        Assert.notNull(permission, "Permission required");
        Assert.notNull(sid, "Sid required");
        if (atIndexLocation < 0) {
            throw new NotFoundException("atIndexLocation must be greater than or equal to zero");
        }
        if (atIndexLocation > this.aces.size()) {
            throw new NotFoundException("atIndexLocation must be less than or equal to the size of the AccessControlEntry collection");
        }

        AccessControlEntryImpl ace = new AccessControlEntryImpl.Builder()
                .acl(this)
                .sid(TenantSid.from(sid))
                .permission(permission)
                .granting(granting).build();

        synchronized (aces) {
            this.aces.add(atIndexLocation, ace);
        }
    }

    @Override
    public List<AccessControlEntry> getEntries() {
        // Can safely return AccessControlEntry directly, as they're immutable outside the ACL package
        synchronized(aces) {
            return new ArrayList<>(aces);
        }
    }

    @Override
    public Serializable getId() {
        return this.id;
    }

    @Override
    public ObjectIdentity getObjectIdentity() {
        return objectIdentity;
    }

    @Override
    public boolean isEntriesInheriting() {
        return entriesInheriting;
    }

    /**
     * Delegates to the {@link PermissionGrantingStrategy}.
     *
     * @throws UnloadedSidException if the passed SIDs are unknown to this ACL because the ACL was only loaded for a
     *         subset of SIDs
     * @see DefaultPermissionGrantingStrategy
     */
    @Override
    public boolean isGranted(List<Permission> permission, List<Sid> sids, boolean administrativeMode)
            throws NotFoundException, UnloadedSidException {
        Assert.notEmpty(permission, "Permissions required");
        Assert.notEmpty(sids, "SIDs required");

        if (!this.isSidLoaded(sids)) {
            throw new UnloadedSidException("ACL was not loaded for one or more SID");
        }

        return permissionGrantingStrategy.isGranted(this, permission, sids, administrativeMode);
    }

    @Override
    public boolean isSidLoaded(List<Sid> sids) {
        return AclUtils.isSidLoaded(loadedSids, sids);
    }

    @Override
    public void setEntriesInheriting(boolean entriesInheriting) {
        aclAuthorizationStrategy.securityCheck(this, AclAuthorizationStrategy.CHANGE_GENERAL);
        this.entriesInheriting = entriesInheriting;
    }

    @Override
    public void setOwner(Sid newOwner) {
        aclAuthorizationStrategy.securityCheck(this, AclAuthorizationStrategy.CHANGE_OWNERSHIP);
        Assert.notNull(newOwner, "Owner required");
        this.owner = newOwner;
    }

    @Override
    public Sid getOwner() {
        return this.owner;
    }

    @Override
    public void setParent(Acl newParent) {
        aclAuthorizationStrategy.securityCheck(this, AclAuthorizationStrategy.CHANGE_GENERAL);
        Assert.isTrue(newParent == null || !newParent.equals(this), "Cannot be the parent of yourself");
        this.parentAcl = newParent;
    }

    @Override
    public Acl getParentAcl() {
        return parentAcl;
    }

    @Override
    public void updateAce(int aceIndex, Permission permission)
        throws NotFoundException {
        aclAuthorizationStrategy.securityCheck(this, AclAuthorizationStrategy.CHANGE_GENERAL);
        verifyAceIndexExists(aceIndex);

        synchronized (aces) {
            AccessControlEntryImpl ace = (AccessControlEntryImpl) aces.get(aceIndex);
            aces.set(aceIndex, new AccessControlEntryImpl.Builder().from(ace).permission(permission).build());
        }
    }

    @Override
    public void updateAuditing(int aceIndex, boolean auditSuccess, boolean auditFailure) {
        aclAuthorizationStrategy.securityCheck(this, AclAuthorizationStrategy.CHANGE_AUDITING);
        verifyAceIndexExists(aceIndex);

        synchronized (aces) {
            AccessControlEntryImpl ace = (AccessControlEntryImpl) aces.get(aceIndex);
            aces.set(aceIndex, new AccessControlEntryImpl.Builder().from(ace)
                    .auditSuccess(auditSuccess)
                    .auditFailure(auditFailure)
                    .build());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MutableAclImpl)) {
            return false;
        }

        MutableAclImpl that = (MutableAclImpl) o;

        if (entriesInheriting != that.entriesInheriting) {
            return false;
        }
        if (aces != null ? !aces.equals(that.aces) : that.aces != null) {
            return false;
        }
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (loadedSids != null ? !loadedSids.equals(that.loadedSids) : that.loadedSids != null) {
            return false;
        }
        if (objectIdentity != null ? !objectIdentity.equals(that.objectIdentity) : that.objectIdentity != null) {
            return false;
        }
        if (owner != null ? !owner.equals(that.owner) : that.owner != null) {
            return false;
        }
        if (parentAcl != null ? !parentAcl.equals(that.parentAcl) : that.parentAcl != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = parentAcl != null ? parentAcl.hashCode() : 0;
        result = 31 * result + (aces != null ? aces.hashCode() : 0);
        result = 31 * result + (objectIdentity != null ? objectIdentity.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (loadedSids != null ? loadedSids.hashCode() : 0);
        result = 31 * result + (entriesInheriting ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AclImpl[");
        sb.append("id: ").append(this.id).append("; ");
        sb.append("objectIdentity: ").append(this.objectIdentity).append("; ");
        sb.append("owner: ").append(this.owner).append("; ");

        int count = 0;

        for (AccessControlEntry ace : aces) {
            count++;

            if (count == 1) {
                sb.append("\n");
            }

            sb.append(ace).append("\n");
        }

        if (count == 0) {
            sb.append("no ACEs; ");
        }

        sb.append("inheriting: ").append(this.entriesInheriting).append("; ");
        sb.append("parent: ").append((this.parentAcl == null) ? "Null" : this.parentAcl.getObjectIdentity().toString());
        sb.append("; ");
        sb.append("aclAuthorizationStrategy: ").append(this.aclAuthorizationStrategy).append("; ");
        sb.append("permissionGrantingStrategy: ").append(this.permissionGrantingStrategy);
        sb.append("]");

        return sb.toString();
    }
}
