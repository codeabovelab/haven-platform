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

package com.codeabovelab.dm.common.security.acl;

import com.codeabovelab.dm.common.security.TenantGrantedAuthoritySid;
import com.codeabovelab.dm.common.security.TenantPrincipalSid;
import org.springframework.security.access.hierarchicalroles.NullRoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * implementation of {@link SidRetrievalStrategy} which produce tenant-owned SIDs
 */
public class TenantSidRetrievalStrategy implements SidRetrievalStrategy {

    private RoleHierarchy roleHierarchy = new NullRoleHierarchy();

    public TenantSidRetrievalStrategy() {
    }

    public TenantSidRetrievalStrategy(RoleHierarchy roleHierarchy) {
        Assert.notNull(roleHierarchy, "RoleHierarchy must not be null");
        this.roleHierarchy = roleHierarchy;
    }

    @Override
    public List<Sid> getSids(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = roleHierarchy.getReachableGrantedAuthorities(authentication.getAuthorities());
        List<Sid> sids = new ArrayList<>(authorities.size() + 1);

        sids.add(new TenantPrincipalSid(authentication));

        for (GrantedAuthority authority : authorities) {
            sids.add(new TenantGrantedAuthoritySid(authority));
        }

        return sids;
    }

}
