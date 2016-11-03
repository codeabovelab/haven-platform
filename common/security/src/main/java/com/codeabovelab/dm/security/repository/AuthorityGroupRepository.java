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

import com.codeabovelab.dm.common.security.dto.AuthorityGroupId;
import com.codeabovelab.dm.security.entity.AuthorityGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

/**
 */
public interface AuthorityGroupRepository extends JpaRepository<AuthorityGroupEntity, Long> {
    AuthorityGroupEntity findByNameAndTenantId(String name, Long tenantId);

    @Query("select new com.codeabovelab.dm.common.security.dto.AuthorityGroupIdImpl(g.name, g.tenant.id)" +
           " from AuthorityGroupEntity g inner join g.users as u where u.username = :userName")
    Set<AuthorityGroupId> findGroupsByUserName(@Param("userName") String userName);
}
