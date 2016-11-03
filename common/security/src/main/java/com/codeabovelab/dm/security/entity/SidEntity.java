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
 * acl Sid entity for using in jpa queries
 */
@Entity
@Table(name = "acl_sid",
        uniqueConstraints = @UniqueConstraint(name = "uc_aclsid_tenant_sid_principal",
                columnNames = {"tenant", "sid", "principal"}))
public class SidEntity implements Serializable {
    @Id
    @GeneratedValue
    private long id;
    
    private String tenant;
    
    private boolean principal;
    
    private String sid;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public boolean isPrincipal() {
        return principal;
    }

    public void setPrincipal(boolean principal) {
        this.principal = principal;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + Objects.hashCode(this.tenant);
        hash = 41 * hash + (this.principal ? 1 : 0);
        hash = 41 * hash + Objects.hashCode(this.sid);
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
        final SidEntity other = (SidEntity) obj;
        if (Objects.equals(this.tenant, other.tenant)) {
            return false;
        }
        if (this.principal != other.principal) {
            return false;
        }
        if (!Objects.equals(this.sid, other.sid)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "SidEntity{" + "id=" + id + ", tenant=" + tenant + ", principal=" + principal + ", sid=" + sid + '}';
    }
}
