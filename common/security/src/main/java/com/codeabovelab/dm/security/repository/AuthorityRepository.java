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

import com.codeabovelab.dm.security.entity.Authority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

/**
 * repository of {@link Authority}
 *
 */
public interface AuthorityRepository extends JpaRepository<Authority, Long> {

    @Query("select a from Authority a where a.role = :role and ((:tenantId is null and a.tenant is null) or a.tenant.name = :tenantId)")
    Authority findByRoleAndTenant(@Param("role") String role, @Param("tenant") String tenantId);

    /**
     * Find additional user authorities. Currently it Authorities obtained through AuthoritiesGroup.
     * @param userId
     * @return
     */
    @Query("select sa from AuthorityGroupEntity sag inner join sag.authorities as sa inner join sag.users as su where su.id = :userId")
    Set<Authority> findAdditionalUserAuthorities(@Param("userId") Long userId);
}
