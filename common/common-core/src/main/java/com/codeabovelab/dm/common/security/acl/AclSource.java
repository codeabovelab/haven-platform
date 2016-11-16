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

import com.codeabovelab.dm.common.security.MultiTenancySupport;
import com.codeabovelab.dm.common.security.dto.ObjectIdentityData;
import com.codeabovelab.dm.common.utils.Uuids;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;
import lombok.Data;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Immutable source for acl.
 * We use classes instead of ifaces because need correct serialization to json.
 */
@Data
public class AclSource {

    @Data
    public static class Builder {
        private final Map<String, AceSource> entries = new LinkedHashMap<>();
        private ObjectIdentityData objectIdentity;
        private TenantSid owner;
        private AclSource parentAcl;
        private boolean entriesInheriting;

        public Builder from(AclSource acl) {
            if(acl == null) {
                return this;
            }
            this.setEntries(acl.getEntries());
            this.setObjectIdentity(acl.getObjectIdentity());
            this.setOwner(acl.getOwner());
            this.setParentAcl(acl.getParentAcl());
            this.setEntriesInheriting(acl.isEntriesInheriting());
            return this;
        }

        public Builder objectIdentity(ObjectIdentityData objectIdentity) {
            setObjectIdentity(objectIdentity);
            return this;
        }

        public void setObjectIdentity(ObjectIdentityData objectIdentity) {
            this.objectIdentity = objectIdentity;
        }

        public Builder owner(TenantSid owner) {
            setOwner(owner);
            return this;
        }

        public Builder entriesInheriting(boolean entriesInheriting) {
            setEntriesInheriting(entriesInheriting);
            return this;
        }

        public Builder addEntry(AceSource entry) {
            String id = entry.getId();
            if(id == null) {
                // this is new entry
                id = newId();
                entry = AceSource.builder().from(entry).id(id).build();
            }
            this.entries.put(id, entry);
            return this;
        }

        private String newId() {
            while(true) {
                String id = Uuids.longUid();
                if(!this.entries.containsKey(id)) {
                    return id;
                }
            }
        }

        public Builder entriesMap(Map<Long, AceSource> entries) {
            setEntriesMap(entries);
            return this;
        }

        public Builder entries(List<AceSource> entries) {
            setEntries(entries);
            return this;
        }

        public void setEntries(List<AceSource> entries) {
            this.entries.clear();
            if(entries != null) {
                entries.forEach(this::addEntry);
            }
        }

        public void setEntriesMap(Map<Long, AceSource> entries) {
            this.entries.clear();
            if(entries != null) {
                entries.forEach((k, v) -> this.addEntry(v));
            }
        }

        public Builder parentAcl(AclSource parentAcl) {
            setParentAcl(parentAcl);
            return this;
        }

        public AclSource build() {
            return new AclSource(this);
        }
    }

    private final Map<String, AceSource> entriesMap;
    private final ObjectIdentityData objectIdentity;
    private final TenantSid owner;
    private final AclSource parentAcl;
    private final boolean entriesInheriting;

    @JsonCreator
    protected AclSource(Builder b) {
        Assert.notNull(b.objectIdentity, "Object Identity required");
        Assert.notNull(b.owner, "Owner required");
        this.owner = b.owner;
        Assert.notNull(MultiTenancySupport.getTenant(this.owner), "Tenant of owner is null");
        this.objectIdentity = b.objectIdentity;
        this.parentAcl = b.parentAcl;
        this.entriesInheriting = b.entriesInheriting;
        // we must save order of  ACEs
        this.entriesMap = Collections.unmodifiableMap(new LinkedHashMap<>(b.entries));
    }

    @JsonIgnore
    public Map<String, AceSource> getEntriesMap() {
        return entriesMap;
    }

    public List<AceSource> getEntries() {
        return ImmutableList.copyOf(this.entriesMap.values());
    }

    public static Builder builder() {
        return new Builder();
    }
}
