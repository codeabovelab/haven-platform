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

import com.codeabovelab.dm.common.security.dto.ObjectIdentityData;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableList;
import lombok.Data;
import org.springframework.security.acls.model.*;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable source for acl
 */
@Data
public class AclSource {

    @Data
    public static class Builder {
        private final List<AceSource> entries = new ArrayList<>();
        private ObjectIdentity objectIdentity;
        private TenantSid owner;
        private AclSource parentAcl;
        private boolean entriesInheriting;

        public Builder from(AclSource acl) {
            this.setEntries(acl.getEntries());
            this.setObjectIdentity(acl.getObjectIdentity());
            this.setOwner(acl.getOwner());
            this.setParentAcl(acl.getParentAcl());
            this.setEntriesInheriting(acl.isEntriesInheriting());
            return this;
        }

        public Builder objectIdentity(ObjectIdentity objectIdentity) {
            setObjectIdentity(objectIdentity);
            return this;
        }

        /* we must use any Id except NONE, because then it skip default value */
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = ObjectIdentityData.class)
        public void setObjectIdentity(ObjectIdentity objectIdentity) {
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
            this.entries.add(entry);
            return this;
        }

        public Builder entries(List<AceSource> entries) {
            setEntries(entries);
            return this;
        }

        public void setEntries(List<AceSource> entries) {
            this.entries.clear();
            if(entries != null) {
                this.entries.addAll(entries);
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

    private final List<AceSource> entries;
    private final ObjectIdentity objectIdentity;
    private final TenantSid owner;
    private final AclSource parentAcl;
    private final boolean entriesInheriting;

    @JsonCreator
    protected AclSource(Builder b) {
        Assert.notNull(b.objectIdentity, "Object Identity required");
        Assert.notNull(b.owner, "Owner required");

        this.owner = b.owner;
        this.objectIdentity = b.objectIdentity;
        this.parentAcl = b.parentAcl;
        this.entriesInheriting = b.entriesInheriting;

        this.entries = ImmutableList.copyOf(b.entries);
    }

    public static Builder builder() {
        return new Builder();
    }
}
