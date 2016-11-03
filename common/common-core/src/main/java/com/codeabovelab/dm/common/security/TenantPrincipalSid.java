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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * {@link PrincipalSid} extension which implement {@link com.codeabovelab.dm.common.security.OwnedByTenant }
 */
@JsonTypeName("PRINCIPAL")
public class TenantPrincipalSid extends PrincipalSid implements TenantSid {
    private final String tenantId;

    @JsonCreator
    public TenantPrincipalSid(@JsonProperty("principal") String principal,
                              @JsonProperty("tenant") String tenant) {
        super(principal);
        this.tenantId = tenant;
    }

    public TenantPrincipalSid(Authentication authentication) {
        super(authentication);
        this.tenantId = MultiTenancySupport.getTenant(authentication.getPrincipal());
    }

    @Override
    public String getTenant() {
        return this.tenantId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof TenantPrincipalSid)) return false;
        if (!super.equals(o)) return false;

        final TenantPrincipalSid that = (TenantPrincipalSid) o;

        if (tenantId != null ? !tenantId.equals(that.tenantId) : that.tenantId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (tenantId != null ? tenantId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TenantPrincipalSid[" + getPrincipal() + ":" + tenantId + ']';
    }

    /**
     * Make SID from user details
     * @param userDetails
     * @return
     */
    public static TenantPrincipalSid from(UserDetails userDetails) {
        return new TenantPrincipalSid(userDetails.getUsername(), MultiTenancySupport.getTenant(userDetails));
    }

    public static TenantPrincipalSid from(PrincipalSid sid) {
        return new TenantPrincipalSid(sid.getPrincipal(), MultiTenancySupport.getTenant(sid));
    }
}
