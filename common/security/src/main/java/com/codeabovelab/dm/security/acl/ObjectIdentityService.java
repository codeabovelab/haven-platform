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

package com.codeabovelab.dm.security.acl;

import com.codeabovelab.dm.security.entity.ObjectIdentityEntity;
import com.codeabovelab.dm.security.repository.ObjectIdentitiesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.transaction.Transactional;
import java.util.List;

/**
 * service which contains some objectIdentityEntity specific code
 */
@Component
@Transactional
class ObjectIdentityService {
    private final ObjectIdentitiesRepository repository;

    @Autowired
    @Lazy
    ObjectIdentityService(ObjectIdentitiesRepository repository) {
        this.repository = repository;
    }

    ObjectIdentityEntity getByIdentity(ObjectIdentity identity) {
        //we support only object which identified by Long value
        final Long objectId = (Long)identity.getIdentifier();
        final String type = identity.getType();
        validateIdentity(objectId, type);
        return repository.findByObjectIdAndObjectClassClassName(objectId, type);
    }

    List<ObjectIdentityEntity> getByParentIdentity(ObjectIdentity parentIdentity) {
        final Long parentId = getIdByIdentity(parentIdentity);
        final List<ObjectIdentityEntity> childEntites = repository.findByParentObjectId(parentId);
        return childEntites;
    }

    Long getIdByIdentity(ObjectIdentity objectIdentity) {
        final Long objectId = (Long) objectIdentity.getIdentifier();
        final String type = objectIdentity.getType();
        validateIdentity(objectId, type);
        return repository.findIdByObjectIdAndObjectClassClassName(objectId, type);
    }

    ObjectIdentityEntity save(ObjectIdentityEntity entity) {
        return repository.save(entity);
    }

    private void validateIdentity(Long objectId, String type) {
        Assert.notNull(objectId, "objectId is null");
        Assert.notNull(type, "type is null");
    }

    void deleteByIdentity(ObjectIdentity objectIdentity) {
        final Long id = getIdByIdentity(objectIdentity);
        if(id != null) {
            repository.delete(id);
        }
    }
}
