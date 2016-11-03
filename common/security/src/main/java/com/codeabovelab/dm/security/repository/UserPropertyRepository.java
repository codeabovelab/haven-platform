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

import com.codeabovelab.dm.security.entity.UserProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Do not use this repository directly.
 * Properties must be used only through
 * {@link com.codeabovelab.dm.security.user.UserPropertiesService}
 */
public interface UserPropertyRepository extends JpaRepository<UserProperty, Long> {
    @Query("select p from UserProperty p where p.userAuthDetails.id = ?1")
    List<UserProperty> getUserProperties(long userId);

    @Query("select p from UserProperty p where p.userAuthDetails.username = ?1")
    List<UserProperty> getPropertiesByUsername(String username);

    @Query("select p.userAuthDetails.id from UserProperty p where p.name = ?1 and p.data = ?2")
    List<Long> getUsersIdsByProperty(String name, String value);
}
