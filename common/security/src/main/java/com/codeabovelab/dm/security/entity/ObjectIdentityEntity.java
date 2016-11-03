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
import java.util.List;
import java.util.Objects;

/**
 * acl object identity entity
 */
@Entity
@Table(name = "acl_object_identity", 
        uniqueConstraints = @UniqueConstraint(name = "uc_aclobjectidentity_class__identity",
                columnNames = {"object_id_class", "object_id_identity"}))
public class ObjectIdentityEntity implements Serializable {
    
    @Id
    @GeneratedValue
    private long id;
    
    @ManyToOne
    @JoinColumn(name = "object_id_class")
    private ClassEntity objectClass;
    
    @Column(name = "object_id_identity")
    private long objectId;
    
    @ManyToOne
    @JoinColumn(name = "parent_object")
    private ObjectIdentityEntity parentObject;
    
    @ManyToOne
    @JoinColumn(name = "owner_sid")
    private SidEntity owner;
    
    @Column(name = "entries_inheriting")
    private boolean entriesInheriting;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<EntryEntity> entries;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ClassEntity getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(ClassEntity objectClass) {
        this.objectClass = objectClass;
    }

    public long getObjectId() {
        return objectId;
    }

    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }

    public ObjectIdentityEntity getParentObject() {
        return parentObject;
    }

    public void setParentObject(ObjectIdentityEntity parentObject) {
        this.parentObject = parentObject;
    }

    public SidEntity getOwner() {
        return owner;
    }

    public void setOwner(SidEntity owner) {
        this.owner = owner;
    }

    public boolean isEntriesInheriting() {
        return entriesInheriting;
    }

    public void setEntriesInheriting(boolean entriesInheriting) {
        this.entriesInheriting = entriesInheriting;
    }

    public List<EntryEntity> getEntries() {
        return entries;
    }

    public void setEntries(List<EntryEntity> entries) {
        this.entries = entries;
    }
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.objectClass);
        hash = 59 * hash + (int) (this.objectId ^ (this.objectId >>> 32));
        hash = 59 * hash + Objects.hashCode(this.parentObject);
        hash = 59 * hash + Objects.hashCode(this.owner);
        hash = 59 * hash + (this.entriesInheriting ? 1 : 0);
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
        final ObjectIdentityEntity other = (ObjectIdentityEntity) obj;
        if (!Objects.equals(this.objectClass, other.objectClass)) {
            return false;
        }
        if (this.objectId != other.objectId) {
            return false;
        }
        if (!Objects.equals(this.parentObject, other.parentObject)) {
            return false;
        }
        if (!Objects.equals(this.owner, other.owner)) {
            return false;
        }
        if (this.entriesInheriting != other.entriesInheriting) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ObjectIdentityEntity{" + "id=" + id + 
                ", objectClass=" + objectClass + 
                ", objectId=" + objectId +
                ", parentObject=" + parentObject +
                ", owner=" + owner + 
                ", entriesInheriting=" + entriesInheriting + '}';
    }
}
