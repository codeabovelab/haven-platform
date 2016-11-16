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

import com.codeabovelab.dm.common.security.Action;
import com.codeabovelab.dm.common.security.Authorities;
import com.codeabovelab.dm.common.security.MultiTenancySupport;
import com.codeabovelab.dm.common.security.dto.PermissionData;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.model.Sid;

/**
 * implementation of PermissionGrantingJudge which provide defaultBehavior for strategy
 */
public class PermissionGrantingJudgeDefaultBehavior implements PermissionGrantingJudge {

    private final TenantsService tenantsService;

    public PermissionGrantingJudgeDefaultBehavior(TenantsService tenantsService) {
        this.tenantsService = tenantsService;
    }

    @Override
    public PermissionData getPermission(PermissionGrantingContext context) {
        PermissionData.Builder pdb = PermissionData.builder();
        final Sid currSid = context.getCurrentSid();
        //by ADMIN authority
        if(currSid instanceof GrantedAuthoritySid && Authorities.ADMIN_ROLE.equals(((GrantedAuthoritySid)currSid).getGrantedAuthority())) {
            final String tenantId = MultiTenancySupport.getTenant(currSid);
            // if role.tenantId == (ROOT or owner.tenantId) or role not tenant the check principal and owner tenants
            if(isRootTenant(tenantId) ||
                (tenantId != MultiTenancySupport.NO_TENANT?
                tenantId.equals(context.getOwnerTenant()) :
                context.getCurrentTenants().contains(context.getOwnerTenant()))) {
                pdb.add(PermissionData.ALL);
            }
        }
        if(PermissionData.ALL.getMask() != pdb.getMask()) {
            if(isAllowByOwner(context)) {
                pdb.add(PermissionData.ALL);
            }
        }
        //below need some discussion
//        if(PermissionData.ALL.getMask() != pdb.getMask() && !context.isHasAces()) {
//            // tenant allow only read
//            if(isAllowByTenant(context)) {
//                pdb.add(Action.READ);
//            }
//        }
        return pdb.build();
    }

    private boolean isRootTenant(String tenant) {
        return tenantsService.isRoot(tenant);
    }

    private boolean isAllowByTenant(PermissionGrantingContext context) {
        // if SID owned by 'ownerTenantId' or it supertenants then he can read
        return context.getCurrentTenants().contains(context.getOwnerTenant());
    }

    private boolean isAllowByOwner(PermissionGrantingContext context) {
        // if is owner then allow all permissions
        return context.getOwnerSid().equals(context.getCurrentSid());
    }
}
