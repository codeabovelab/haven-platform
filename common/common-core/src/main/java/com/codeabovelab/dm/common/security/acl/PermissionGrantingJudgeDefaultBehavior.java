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

import com.codeabovelab.dm.common.security.Authorities;
import com.codeabovelab.dm.common.security.MultiTenancySupport;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.model.Sid;

/**
 * implementation of PermissionGrantingJudge which provide defaulBehavior for strategy
 */
public class PermissionGrantingJudgeDefaultBehavior implements PermissionGrantingJudge {

    private final TenantsService tenantsService;

    public PermissionGrantingJudgeDefaultBehavior(TenantsService tenantsService) {
        this.tenantsService = tenantsService;
    }

    @Override
    public boolean allow(PermissionGrantingContext context) {
        boolean allow = false;
        final Sid currSid = context.getCurrentSid();
        //by ADMIN authority
        if(currSid instanceof GrantedAuthoritySid && Authorities.ADMIN_ROLE.equals(((GrantedAuthoritySid)currSid).getGrantedAuthority())) {
            final String tenantId = MultiTenancySupport.getTenant(currSid);
            // if role.tenantId == (ROOT or owner.tenantId) or role not tenant the check principal and owner tenants
            allow = isRootTenant(tenantId) ||
                    (tenantId != MultiTenancySupport.NO_TENANT?
                    tenantId.equals(context.getOwnerTenant()) :
                    context.getCurrentTenants().contains(context.getOwnerTenant()));
        }
        if(!allow) {
            allow = isAllowByOwner(context);
        }
        if(!allow && !context.isHasAces()) {
            allow = isAllowByTenant(context);
        }
        return allow;
    }

    private boolean isRootTenant(String tenant) {
        return tenantsService.isRoot(tenant);
    }

    private boolean isAllowByTenant(PermissionGrantingContext context) {
        // if SID owned by 'ownerTenantId' or it supertenants then he can read
        if(context.getCurrentTenants().contains(context.getOwnerTenant()) && 
           context.getPermission().getMask() == BasePermission.READ.getMask()) {
            return true;
        }
        return false;
    }

    private boolean isAllowByOwner(PermissionGrantingContext context) {
        // if is owner then allow all permissions
        return context.getOwnerSid().equals(context.getCurrentSid());
    }
}
