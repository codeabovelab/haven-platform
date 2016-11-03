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

package com.codeabovelab.dm.security;

import com.codeabovelab.dm.common.security.GrantedAuthorityImpl;
import com.codeabovelab.dm.common.security.MultiTenancySupport;
import com.codeabovelab.dm.common.security.dto.AuthorityGroupData;
import com.codeabovelab.dm.common.security.dto.AuthorityGroupDataImpl;
import com.codeabovelab.dm.security.entity.Authority;
import com.codeabovelab.dm.security.entity.AuthorityGroupEntity;
import com.codeabovelab.dm.security.entity.TenantEntity;
import com.codeabovelab.dm.security.repository.AuthorityRepository;
import org.springframework.security.core.GrantedAuthority;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 */
public final class SecurityUtils {
    private SecurityUtils() {
    }


    /**
     * Convert set of any GrantedAuthority implementation to set of {@link GrantedAuthorityImpl}
     * @param authorities
     * @return new HashSet with {@link GrantedAuthorityImpl}
     */
    public static Set<GrantedAuthority> convert(Set<? extends GrantedAuthority> authorities) {
        Set<GrantedAuthority> set = new HashSet<>();
        for(GrantedAuthority authority: authorities) {
            set.add(GrantedAuthorityImpl.convert(authority));
        }
        return set;
    }

    public static AuthorityGroupData fromEntity(AuthorityGroupEntity entity) {
        AuthorityGroupDataImpl.Builder builder = new AuthorityGroupDataImpl.Builder();
        builder.setName(entity.getName());
        TenantEntity tenant = entity.getTenant();
        if(tenant != null) {
            builder.setTenantId(tenant.getName());
        }
        builder.setAuthorities(convert(entity.getAuthorities()));
        return builder.build();
    }

    public static void toEntity(AuthorityRepository authorityRepository, AuthorityGroupData groupData, AuthorityGroupEntity groupEntity) {
        Set<Authority> authoritiesEntities = groupEntity.getAuthorities();
        //  we need to convert any authority to one type for correct HashSet operations (also we can had use TreeSet).
        Set<GrantedAuthority> newAuthorities = convert(groupData.getAuthorities());
        //we need to delete authorities which is not present in newAuthorities
        if(authoritiesEntities != null) {
            Iterator<Authority> i = authoritiesEntities.iterator();
            while(i.hasNext()) {
                Authority authority = i.next();
                GrantedAuthority oldAuthority = GrantedAuthorityImpl.convert(authority);
                if(!newAuthorities.remove(oldAuthority)) {
                    i.remove();
                }
            }
        } else {
            authoritiesEntities = new HashSet<>();
            groupEntity.setAuthorities(authoritiesEntities);
        }
        for(GrantedAuthority authority: newAuthorities) {
            final String tenantId = MultiTenancySupport.getTenant(authority);
            final String authorityName = authority.getAuthority();
            Authority newAuthority = authorityRepository.findByRoleAndTenant(authorityName, tenantId);
            // we assume that authority must exist, if this is not true then we don't create authority, because we cannot remove it
            if(newAuthority == null) {
                throw new RuntimeException("Can not find authority by name=" + authorityName + " and tenant=" + tenantId);
            }
            authoritiesEntities.add(newAuthority);
        }
    }
}
