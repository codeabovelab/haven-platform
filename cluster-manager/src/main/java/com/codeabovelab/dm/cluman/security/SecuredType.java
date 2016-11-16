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

package com.codeabovelab.dm.cluman.security;

import com.codeabovelab.dm.common.security.Authorities;
import com.codeabovelab.dm.common.security.dto.ObjectIdentityData;
import org.springframework.security.acls.model.ObjectIdentity;

/**
 * Constants for known secured object types. <p/>
 * In some cases system can use class name instead of this.
 */
public enum SecuredType {
    CLUSTER,
    NODE,
    CONTAINER,
    /**
     * image loaded on concrete node or cluster
     */
    LOCAL_IMAGE,
    /**
     * image placed on repository
     */
    REMOTE_IMAGE,
    NETWORK;
    /**
     * It can not be calculated, because it is used for annotation.
     */
    public static final String CLUSTER_ADMIN = "ROLE_CLUSTER_ADMIN";
    private final String adminRole;
    private final String userRole;

    SecuredType() {
        this.adminRole = Authorities.adminOf(name());
        this.userRole = Authorities.userOf(name());
    }

    /**
     * Make {@link ObjectIdentity } for specified id and current type
     * @param id if null then act like {@link #typeId()}
     * @return ObjectIdentity for specified id and type, or only type if id is null
     */
    public ObjectIdentityData id(String id) {
        if(id == null) {
            return typeId();
        }
        return new ObjectIdentityData(name(), id);
    }

    /**
     * Make {@link ObjectIdentity } for current type
     * @see com.codeabovelab.dm.common.security.acl.AclUtils#toTypeId(ObjectIdentity)
     * @return
     */
    public ObjectIdentityData typeId() {
        return new ObjectIdentityData(name(), "");
    }

    public String admin() {
        return adminRole;
    }

    public String user() {
        return userRole;
    }
}
