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

import com.codeabovelab.dm.security.entity.UserAuthDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * <b>Note that this entity does not contains all user roles and other data, for retrieving correct UserDetails you
 * need to use {@link org.springframework.security.core.userdetails.UserDetailsService common service}.<b/>
 */
public interface UserRepository extends JpaRepository<UserAuthDetails, Long> {

    UserAuthDetails findByUsername(String username);

    UserAuthDetails findByEmail(String userEmail);

    UserAuthDetails findByMobile(Long mobile);

    @Query("select count(u) from UserAuthDetails u join u.authorities a where a.role = :role and ((:tenantId is null and a.tenant is null) or a.tenant.id = :tenantId)")
    int findUsersCountByRole(@Param("role") String role, @Param("tenantId") Long tenantId);
}
