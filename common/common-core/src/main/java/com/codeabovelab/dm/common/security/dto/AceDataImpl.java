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
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.AuditableAccessControlEntry;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.util.Assert;

/**
 * ACE DTO implementation
 */
public class AceDataImpl implements AceData {
    public static class Builder implements AceData {
        private Long id;
        private Permission permission;
        private Sid sid;
        private boolean granting = true;
        private boolean auditFailure = false;
        private boolean auditSuccess = false;

        @Override
        public Long getId() {
            return id;
        }

        public Builder id(Long id) {
            setId(id);
            return this;
        }

        public void setId(Long id) {
            this.id = id;
        }

        @Override
        public Permission getPermission() {
            return permission;
        }

        public Builder permission(Permission permission) {
            setPermission(permission);
            return this;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public void setPermission(Permission permission) {
            this.permission = PermissionData.from(permission);
        }

        @Override
        public Sid getSid() {
            return sid;
        }

        public Builder sid(Sid sid) {
            setSid(sid);
            return this;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public void setSid(Sid sid) {
            this.sid = sid;
        }

        @Override
        public boolean isGranting() {
            return granting;
        }

        public Builder granting(boolean granting) {
            setGranting(granting);
            return this;
        }

        public void setGranting(boolean granting) {
            this.granting = granting;
        }

        @Override
        public boolean isAuditFailure() {
            return auditFailure;
        }

        public Builder auditFailure(boolean auditFailure) {
            setAuditFailure(auditFailure);
            return this;
        }

        public void setAuditFailure(boolean auditFailure) {
            this.auditFailure = auditFailure;
        }

        @Override
        public boolean isAuditSuccess() {
            return auditSuccess;
        }

        public Builder auditSuccess(boolean auditSuccess) {
            setAuditSuccess(auditSuccess);
            return this;
        }

        public void setAuditSuccess(boolean auditSuccess) {
            this.auditSuccess = auditSuccess;
        }

        public AceDataImpl build() {
            return new AceDataImpl(this);
        }

        public Builder from(AccessControlEntry entry) {
            setId((Long) entry.getId());
            setPermission(entry.getPermission());
            setSid(entry.getSid());
            setGranting(entry.isGranting());
            if(entry instanceof AuditableAccessControlEntry) {
                AuditableAccessControlEntry aace = (AuditableAccessControlEntry) entry;
                setAuditFailure(aace.isAuditFailure());
                setAuditSuccess(aace.isAuditSuccess());
            }
            return this;
        }

        public Builder from(AceData ace) {
            setId(ace.getId());
            setPermission(ace.getPermission());
            setSid(ace.getSid());
            setGranting(ace.isGranting());
            setAuditFailure(ace.isAuditFailure());
            setAuditSuccess(ace.isAuditSuccess());
            return this;
        }
    }

    private final Long id;
    private final Permission permission;
    private final Sid sid;
    private final boolean granting;
    private final boolean auditFailure;
    private final boolean auditSuccess;

    @JsonCreator
    public AceDataImpl(Builder b) {
        Assert.notNull(b.sid, "Sid required");
        Assert.notNull(b.permission, "Permission required");
        this.id = b.id;
        this.sid = b.sid;
        this.permission = b.permission;
        this.granting = b.granting;
        this.auditSuccess = b.auditSuccess;
        this.auditFailure = b.auditFailure;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public Permission getPermission() {
        return this.permission;
    }

    @Override
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public Sid getSid() {
        return this.sid;
    }

    @Override
    public boolean isGranting() {
        return this.granting;
    }

    @Override
    public boolean isAuditFailure() {
        return this.auditFailure;
    }

    @Override
    public boolean isAuditSuccess() {
        return this.auditSuccess;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AceDataImpl)) {
            return false;
        }

        AceDataImpl aceData = (AceDataImpl) o;

        if (auditFailure != aceData.auditFailure) {
            return false;
        }
        if (auditSuccess != aceData.auditSuccess) {
            return false;
        }
        if (granting != aceData.granting) {
            return false;
        }
        if (id != null ? !id.equals(aceData.id) : aceData.id != null) {
            return false;
        }
        if (permission != null ? !permission.equals(aceData.permission) : aceData.permission != null) {
            return false;
        }
        if (sid != null ? !sid.equals(aceData.sid) : aceData.sid != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (permission != null ? permission.hashCode() : 0);
        result = 31 * result + (sid != null ? sid.hashCode() : 0);
        result = 31 * result + (granting ? 1 : 0);
        result = 31 * result + (auditFailure ? 1 : 0);
        result = 31 * result + (auditSuccess ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AceDataImpl{" +
                "id=" + id +
                ", permission=" + permission +
                ", sid=" + sid +
                ", granting=" + granting +
                ", auditFailure=" + auditFailure +
                ", auditSuccess=" + auditSuccess +
                '}';
    }
}
