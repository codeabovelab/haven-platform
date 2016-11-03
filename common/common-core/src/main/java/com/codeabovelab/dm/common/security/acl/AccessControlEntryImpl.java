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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.acls.model.*;
import org.springframework.util.Assert;


/**
 * AccessControlEntry implementation. <p/> 
 * it created because original implementation design does not allow modification from custom Acl implementations
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AccessControlEntryImpl extends AceSource implements AccessControlEntry, AuditableAccessControlEntry {

    public static class Builder extends AceSource.AbstractBuilder<Builder> {
        private Acl acl;

        @Override
        public Acl getAcl() {
            return acl;
        }

        public Builder acl(Acl acl) {
            setAcl(acl);
            return this;
        }
        
        public void setAcl(Acl acl) {
            this.acl = acl;
        }

        /**
         * copy field values from specified entity
         * @param entry
         * @return
         */
        public Builder from(AccessControlEntry entry) {
            super.from(entry);
            this.acl = entry.getAcl();
            return this;
        }
        
        public AccessControlEntryImpl build() {
            return new AccessControlEntryImpl(this);
        }
    }
    
    private final Acl acl;

    private AccessControlEntryImpl(Builder b) {
        super(b);
        Assert.notNull(b.acl, "Acl required");
        this.acl = b.acl;
    }

    @Override
    public Acl getAcl() {
        return acl;
    }
}
