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

import com.codeabovelab.dm.common.security.Tenant;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;


/**
 *
 * Tenant entity
 */
@Entity
@Table(name = "tenants")
public class TenantEntity implements Serializable, Tenant {
    @Id
    @GeneratedValue
    private long id;
    private boolean root;
    @ManyToOne
    private TenantEntity parent;
    @OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE)
    private List<TenantEntity> children;
    private String name;

    public long getId() {
        return id;
    }

    public TenantEntity id(long id) {
        setId(id);
        return this;
    }
    
    public void setId(long id) {
        this.id = id;
    }

    public TenantEntity getParent() {
        return parent;
    }

    public TenantEntity parent(TenantEntity parent) {
        setParent(parent);
        return this;
    }
    
    public void setParent(TenantEntity parent) {
        this.parent = parent;
    }

    public List<TenantEntity> getChildren() {
        return children;
    }

    public void setChildren(List<TenantEntity> children) {
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public TenantEntity name(String name) {
        setName(name);
        return this;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public boolean isRoot() {
        return root;
    }

    public TenantEntity root(boolean root) {
        setRoot(root);
        return this;
    }

    public void setRoot(boolean root) {
        this.root = root;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.parent);
        hash = 67 * hash + Objects.hashCode(this.name);
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
        final TenantEntity other = (TenantEntity) obj;
        if (!Objects.equals(this.parent, other.parent)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "TenantEntry{" + "id=" + id + ", parent=" + parent + ", children=" + (children == null? "null" : children.size()) + ", name=" + name + '}';
    }
}
