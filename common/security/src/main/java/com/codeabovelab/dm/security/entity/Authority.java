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

import com.codeabovelab.dm.common.security.OwnedByTenant;
import org.springframework.security.core.GrantedAuthority;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Represents an authority granted to an {@link org.springframework.security.core.Authentication} object.
 *
 * <p>
 * A <code>GrantedAuthority</code> must either represent itself as a
 * <code>String</code> or be specifically supported by an  {@link
 * org.springframework.security.access.AccessDecisionManager}.
 *
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"role", "tenant_id"}))
public class Authority implements GrantedAuthority, OwnedByTenant {

    @Id
    @GeneratedValue
    private Long id;
    @NotNull
    private String role;
    @ManyToOne
    private TenantEntity tenant;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String getAuthority() {
        return role;
    }

    public TenantEntity getTenantEntity() {
        return tenant;
    }

    public void setTenantEntity(TenantEntity tenant) {
        this.tenant = tenant;
    }

    @Override
    public String getTenant() {
        return tenant == null? null : tenant.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Authority)) return false;

        Authority authority = (Authority) o;

        if (!role.equals(authority.role)) return false;
        if (!Objects.equals(tenant, authority.tenant)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (role != null ? role.hashCode() : 0);
        result = 31 * result + (tenant != null ? tenant.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Authority{" +
                "id=" + id +
                ", role='" + role + '\'' +
                ", tenant=" + tenant +
                '}';
    }
}
