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

package com.codeabovelab.dm.security.repository;

import com.codeabovelab.dm.security.entity.ObjectIdentityEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ObjectIdentitiesRepository extends PagingAndSortingRepository<ObjectIdentityEntity, Long> {
    
    ObjectIdentityEntity findByObjectIdAndObjectClassClassName(Long objectId, String className);
    
    List<ObjectIdentityEntity> findByParentObjectId(Long parent);

    @Query("select oi.id from ObjectIdentityEntity oi where oi.objectId = :objectId and oi.objectClass.className = :type")
    Long findIdByObjectIdAndObjectClassClassName(@Param("objectId") Long objectId, @Param("type") String type);
}
