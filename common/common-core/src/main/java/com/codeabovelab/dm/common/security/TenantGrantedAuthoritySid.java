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
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.core.GrantedAuthority;

import java.util.Objects;

/**
 * a {@link GrantedAuthoritySid } extension which implement {@link com.codeabovelab.dm.common.security.OwnedByTenant }
 */
@JsonTypeName("GRANTED_AUTHORITY")
public class TenantGrantedAuthoritySid extends GrantedAuthoritySid implements TenantSid {

    private final String tenant;

    @JsonCreator
    public TenantGrantedAuthoritySid(
            @JsonProperty("grantedAuthority") String grantedAuthority,
            @JsonProperty("tenant") String tenant) {
        super(grantedAuthority);
        this.tenant = tenant;
    }

    public TenantGrantedAuthoritySid(GrantedAuthority grantedAuthority) {
        super(grantedAuthority);
        this.tenant = MultiTenancySupport.getTenant(grantedAuthority);
    }
    
    @Override
    public String getTenant() {
        return tenant;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.getGrantedAuthority());
        hash = 71 * hash + Objects.hashCode(this.tenant);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final TenantGrantedAuthoritySid other = (TenantGrantedAuthoritySid)obj;
        if(!Objects.equals(this.getGrantedAuthority(), other.getGrantedAuthority())) {
            return false;
        }
        if(!Objects.equals(this.tenant, other.tenant)) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        //Do not change below code, it must matches scheme from config file.
        return getGrantedAuthority() + ":" + tenant;
    }

    public static TenantGrantedAuthoritySid from(GrantedAuthoritySid sid) {
        return new TenantGrantedAuthoritySid(sid.getGrantedAuthority(), MultiTenancySupport.getTenant(sid));
    }

    public static TenantGrantedAuthoritySid from(GrantedAuthority ga) {
        return new TenantGrantedAuthoritySid(ga.getAuthority(), MultiTenancySupport.getTenant(ga));
    }
}
