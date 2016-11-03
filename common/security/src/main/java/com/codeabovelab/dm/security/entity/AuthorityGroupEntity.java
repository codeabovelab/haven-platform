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
import java.util.Set;

/**
 * Group of authorities entity.
 */
@Entity
@Table(
        name = "authority_group",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "tenant_id"})
)
public class AuthorityGroupEntity {
    @Id
    @GeneratedValue
    private Long id;
    @JoinColumn(name = "tenant_id")
    @ManyToOne
    private TenantEntity tenant;
    @Column
    private String name;
    @ManyToMany
    @JoinTable(
            name = "authority_group_authorities",
            joinColumns = @JoinColumn(name = "group_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "authority_id", referencedColumnName = "id")
    )
    private Set<Authority> authorities;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_authority_groups",
            joinColumns = @JoinColumn(name = "group_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id")
    )
    private Set<UserAuthDetails> users;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TenantEntity getTenant() {
        return tenant;
    }

    public void setTenant(TenantEntity tenant) {
        this.tenant = tenant;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Authority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Set<Authority> authorities) {
        this.authorities = authorities;
    }

    public Set<UserAuthDetails> getUsers() {
        return users;
    }

    public void setUsers(Set<UserAuthDetails> users) {
        this.users = users;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthorityGroupEntity)) {
            return false;
        }

        AuthorityGroupEntity that = (AuthorityGroupEntity) o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (tenant != null ? !tenant.equals(that.tenant) : that.tenant != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = tenant != null ? tenant.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AuthorityGroupEntity{" +
                "id=" + id +
                ", tenant=" + tenant +
                ", name='" + name + '\'' +
                ", authorities=" + authorities +
                '}';
    }
}
