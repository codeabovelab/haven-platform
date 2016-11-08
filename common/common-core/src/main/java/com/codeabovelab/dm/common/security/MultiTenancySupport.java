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

package com.codeabovelab.dm.common.security;

import com.codeabovelab.dm.common.security.acl.TenantSid;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Some constants methods and constants for multi tenancy support
 */
public class MultiTenancySupport {
    /**
     * uses in cases where tenantId retrieved from null or incorrect objects
     */
    public static final String NO_TENANT = null;
    public static final String ANONYMOUS_TENANT = "anonymous_tenant";
    public static final String ROOT_TENANT = "root";

    /**
     * retrieve tenantId from object if it instance of {@link OwnedByTenant}, otherwise return {@link  #NO_TENANT}
     * @param object
     * @return 
     */
    public static String getTenant(Object object) {
        if(object instanceof OwnedByTenant) {
            return ((OwnedByTenant)object).getTenant();
        }
        return NO_TENANT;
    }

    /**
     * Fix null tenant for principals and validate.
     * @param sid
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T extends TenantSid> T fixTenant(T sid) {
        if(sid == null) {
            return sid;
        }
        final String tenant = sid.getTenant();
        if(sid instanceof GrantedAuthoritySid && tenant == null) {
            return sid;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ExtendedUserDetails eud = (ExtendedUserDetails) auth.getPrincipal();
        final String authTenant = eud.getTenant();
        if(authTenant.equals(tenant)) {
            return sid;
        }
        if(tenant == null) {
            return (T) TenantPrincipalSid.from((PrincipalSid) sid);
        }
        if(!ROOT_TENANT.equals(authTenant)) {
            // we must check tenancy access through TenantHierarchy, but now we does not have any full tenancy support
            throw new IllegalArgumentException("Sid " + sid + " has incorrect tenant: " + tenant + " it allow only for root tenant.");
        }
        return sid;
    }
}
