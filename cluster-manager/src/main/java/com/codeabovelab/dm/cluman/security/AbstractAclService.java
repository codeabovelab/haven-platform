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

import com.codeabovelab.dm.common.security.acl.AclSource;
import org.springframework.security.acls.model.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public abstract class AbstractAclService implements AclService {

    @Override
    public List<ObjectIdentity> findChildren(ObjectIdentity parentIdentity) {
        return Collections.emptyList();
    }

    @Override
    public Acl readAclById(ObjectIdentity object) throws NotFoundException {
        return readAclById(object, Collections.emptyList());
    }

    @Override
    public Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> objects) throws NotFoundException {
        return readAclsById(objects, Collections.emptyList());
    }

    @Override
    public Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> objects, List<Sid> sids) throws NotFoundException {
        Map<ObjectIdentity, Acl> map = new HashMap<>();
        for(ObjectIdentity object: objects) {
            Acl acl = readAclById(object, sids);
            map.put(object, acl);
        }
        return map;
    }

    /**
     * used for publish permissions to ui
     * @param oid
     * @return
     */
    public abstract AclSource getAclSource(ObjectIdentity oid);
}
