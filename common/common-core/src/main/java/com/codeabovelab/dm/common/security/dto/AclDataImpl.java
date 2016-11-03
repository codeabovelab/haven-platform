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

package com.codeabovelab.dm.common.security.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.security.acls.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ACL DTO implementation
 *
 */
public class AclDataImpl implements AclData {

    public static class Builder implements AclData {
        private final List<AceData> entries = new ArrayList<>();
        private Long id;
        private ObjectIdentity objectIdentity;
        private Sid owner;
        private AclData parentAclData;
        private boolean entriesInheriting;

        public Builder from(AclData aclData) {
            this.setId(aclData.getId());
            this.setEntries(aclData.getEntries());
            this.setObjectIdentity(aclData.getObjectIdentity());
            this.setOwner(aclData.getOwner());
            this.setParentAclData(aclData.getParentAclData());
            this.setEntriesInheriting(aclData.isEntriesInheriting());
            return this;
        }

        public Builder from(Acl aclData) {
            if(aclData instanceof MutableAcl) {
                this.setId((Long)((MutableAcl) aclData).getId());
            }

            final List<AccessControlEntry> srcEntries = aclData.getEntries();
            if(srcEntries != null) {
                final int size = srcEntries.size();
                final List<AceData> aceDatas = new ArrayList<>(size);
                for(int i = 0; i < size; ++i) {
                    AccessControlEntry entry = srcEntries.get(i);
                    AceData aceData = AceDataImpl.builder().from(entry).build();
                    aceDatas.add(aceData);
                }
                this.setEntries(aceDatas);
            }

            this.setObjectIdentity(aclData.getObjectIdentity());
            this.setOwner(aclData.getOwner());
            Acl parentAcl = aclData.getParentAcl();
            if(parentAcl != null) {
                this.setParentAclData(AclDataImpl.builder().from(parentAcl).build());
            }
            this.setEntriesInheriting(aclData.isEntriesInheriting());
            return this;
        }

        @Override
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public void setEntries(List<? extends AceData> entries) {
            this.entries.clear();
            this.entries.addAll(entries);
        }

        @Override
        public List<AceData> getEntries() {
            return entries;
        }

        @Override
        public ObjectIdentity getObjectIdentity() {
            return objectIdentity;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public void setObjectIdentity(ObjectIdentity objectIdentity) {
            this.objectIdentity = ObjectIdentityData.from(objectIdentity);
        }

        @Override
        public Sid getOwner() {
            return owner;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public void setOwner(Sid owner) {
            this.owner = owner;
        }

        @Override
        public AclData getParentAclData() {
            return parentAclData;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public void setParentAclData(AclData parentAclData) {
            this.parentAclData = parentAclData;
        }

        @Override
        public boolean isEntriesInheriting() {
            return entriesInheriting;
        }

        public void setEntriesInheriting(boolean entriesInheriting) {
            this.entriesInheriting = entriesInheriting;
        }

        public AclDataImpl build() {
            return new AclDataImpl(this);
        }
    }

    private final Long id;
    private final List<AceData> entries;
    private final ObjectIdentity objectIdentity;
    private final Sid owner;
    private final AclData parentAclData;
    private final boolean entriesInheriting;

    @JsonCreator
    public AclDataImpl(Builder b) {
        this.id = b.id;
        this.entries = Collections.unmodifiableList(new ArrayList<>(b.entries));
        this.objectIdentity = b.objectIdentity;
        this.owner = b.owner;
        this.parentAclData = b.parentAclData;
        this.entriesInheriting = b.entriesInheriting;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public List<AceData> getEntries() {
        return this.entries;
    }

    @Override
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public ObjectIdentity getObjectIdentity() {
        return this.objectIdentity;
    }

    @Override
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public Sid getOwner() {
        return this.owner;
    }

    @Override
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public AclData getParentAclData() {
        return this.parentAclData;
    }

    @Override
    public boolean isEntriesInheriting() {
        return this.entriesInheriting;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AclDataImpl)) {
            return false;
        }

        AclDataImpl aclData = (AclDataImpl) o;

        if (entriesInheriting != aclData.entriesInheriting) {
            return false;
        }
        if (entries != null ? !entries.equals(aclData.entries) : aclData.entries != null) {
            return false;
        }
        if (objectIdentity != null ? !objectIdentity.equals(aclData.objectIdentity) : aclData.objectIdentity != null) {
            return false;
        }
        if (owner != null ? !owner.equals(aclData.owner) : aclData.owner != null) {
            return false;
        }
        if (parentAclData != null ? !parentAclData.equals(aclData.parentAclData) : aclData.parentAclData != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = entries != null ? entries.hashCode() : 0;
        result = 31 * result + (objectIdentity != null ? objectIdentity.hashCode() : 0);
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (parentAclData != null ? parentAclData.hashCode() : 0);
        result = 31 * result + (entriesInheriting ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AclDataImpl{" +
                "entries=" + entries +
                ", objectIdentity=" + objectIdentity +
                ", owner=" + owner +
                ", parentAclData=" + parentAclData +
                ", entriesInheriting=" + entriesInheriting +
                '}';
    }
}
