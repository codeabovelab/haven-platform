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
 * acl class entity
 */
@Entity
@Table(name = "acl_class")
public class ClassEntity implements Serializable {
    @Id
    @GeneratedValue
    private long id;
    
    @Column(name = "class", unique = true, length = 100)
    private String className;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 47 * hash + Objects.hashCode(this.className);
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
        final ClassEntity other = (ClassEntity) obj;
        if (!Objects.equals(this.className, other.className)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ClassEntity{" + "id=" + id + ", className=" + className + '}';
    }
}
