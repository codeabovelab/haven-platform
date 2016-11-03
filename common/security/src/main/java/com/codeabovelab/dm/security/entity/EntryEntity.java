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

package com.codeabovelab.dm.security.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * acl entry entity
 */
@Entity
@Table(name = "acl_entry", 
        uniqueConstraints = @UniqueConstraint(name = "uc_aclentry_objectidentity_order",
                columnNames = {"acl_object_identity","ace_order"}))
public class EntryEntity implements Serializable {
    @Id
    @GeneratedValue
    private long id;
    
    @ManyToOne
    @JoinColumn(name = "acl_object_identity")
    private ObjectIdentityEntity objectIdentity;
    
    @Column(name = "ace_order", nullable = false)
    private int order;
    
    @ManyToOne
    @JoinColumn(name = "sid")
    private SidEntity sid;
    
    @Column(name = "mask", nullable = false)
    private int mask;
    
    @Column(name = "granting", nullable = false)
    private boolean granting;
    
    @Column(name = "audit_success", nullable = false)
    private boolean auditSuccess;
    
    @Column(name = "audit_failure", nullable = false)
    private boolean auditFailure;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ObjectIdentityEntity getObjectIdentity() {
        return objectIdentity;
    }

    public void setObjectIdentity(ObjectIdentityEntity objectIdentity) {
        this.objectIdentity = objectIdentity;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public SidEntity getSid() {
        return sid;
    }

    public void setSid(SidEntity sid) {
        this.sid = sid;
    }

    public int getMask() {
        return mask;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    public boolean isGranting() {
        return granting;
    }

    public void setGranting(boolean granting) {
        this.granting = granting;
    }

    public boolean isAuditSuccess() {
        return auditSuccess;
    }

    public void setAuditSuccess(boolean auditSuccess) {
        this.auditSuccess = auditSuccess;
    }

    public boolean isAuditFailure() {
        return auditFailure;
    }

    public void setAuditFailure(boolean auditFailure) {
        this.auditFailure = auditFailure;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.objectIdentity);
        hash = 89 * hash + this.order;
        hash = 89 * hash + Objects.hashCode(this.sid);
        hash = 89 * hash + this.mask;
        hash = 89 * hash + (this.granting ? 1 : 0);
        hash = 89 * hash + (this.auditSuccess ? 1 : 0);
        hash = 89 * hash + (this.auditFailure ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EntryEntity other = (EntryEntity) obj;
        if (!Objects.equals(this.objectIdentity, other.objectIdentity)) {
            return false;
        }
        if (this.order != other.order) {
            return false;
        }
        if (!Objects.equals(this.sid, other.sid)) {
            return false;
        }
        if (this.mask != other.mask) {
            return false;
        }
        if (this.granting != other.granting) {
            return false;
        }
        if (this.auditSuccess != other.auditSuccess) {
            return false;
        }
        if (this.auditFailure != other.auditFailure) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "EntryEntity{" + "id=" + id + 
                ", objectIdentity=" + objectIdentity + 
                ", order=" + order + 
                ", sid=" + sid + 
                ", mask=" + mask + 
                ", granting=" + granting + 
                ", auditSuccess=" + auditSuccess + 
                ", auditFailure=" + auditFailure + '}';
    }
    
    
}
