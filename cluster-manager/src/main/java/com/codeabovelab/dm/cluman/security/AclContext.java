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

package com.codeabovelab.dm.cluman.security;

import com.codeabovelab.dm.common.security.Authorities;
import com.codeabovelab.dm.common.security.MultiTenancySupport;
import com.codeabovelab.dm.common.security.TenantGrantedAuthoritySid;
import com.codeabovelab.dm.common.security.acl.ExtPermissionGrantingStrategy;
import com.codeabovelab.dm.common.security.dto.PermissionData;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.model.*;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

/**
 */
public class AclContext {
    private final AclService aclService;
    private final ExtPermissionGrantingStrategy pgs;
    private final List<Sid> sids;

    AclContext(AclService aclService, ExtPermissionGrantingStrategy pgs, List<Sid> sids) {
        this.aclService = aclService;
        this.pgs = pgs;
        this.sids = sids;
    }

    /**
     * Check access for specified object
     * @param o
     * @param perms
     * @return
     */
    public boolean isGranted(ObjectIdentity o, Permission ... perms) {
        Assert.notNull(o, "Secured object is null");
        if (isAdminFor(o)) {
            return true;
        }
        try {
            Acl acl = aclService.readAclById(o);
            return acl.isGranted(Arrays.asList(perms), sids, false);
        } catch (NotFoundException e) {
            return false;
        }
    }

    public void assertGranted(ObjectIdentity oid, Permission ... perms) {
        boolean granted = isGranted(oid, perms);
        if(!granted) {
            throw new AccessDeniedException("Access to " + oid + " with " + Arrays.toString(perms)  + " is denied.");
        }
    }

    private boolean isAdminFor(ObjectIdentity o) {
        final String role = Authorities.adminOf(o.getType());
        final String objectTenant = MultiTenancySupport.getTenant(o);
        for(Sid sid: sids) {
            if(!(sid instanceof TenantGrantedAuthoritySid)) {
                continue;
            }
            TenantGrantedAuthoritySid authoritySid = (TenantGrantedAuthoritySid) sid;
            //TODO we need retrieve tenant hierarchy
//            String sidTenant = authoritySid.getTenant();
//            if(sidTenant != null && !Objects.equals(sidTenant, objectTenant)) {
//                continue;
//            }
            String authority = authoritySid.getGrantedAuthority();
            if(Authorities.ADMIN_ROLE.equals(authority)) {
                //grant by admin authority
                return true;
            }
            if(role.equals(authority)) {
                //grant by type admin authority
                return true;
            }
        }
        return false;
    }

    public PermissionData getPermission(ObjectIdentity oid) {
        Assert.notNull(oid, "Secured object is null");
        if(isAdminFor(oid)) {
            return PermissionData.ALL;
        }
        try {
            Acl realAcl = aclService.readAclById(oid);
            return pgs.getPermission(realAcl, sids);
        } catch (NotFoundException e) {
            return PermissionData.NONE;
        }
    }
}
