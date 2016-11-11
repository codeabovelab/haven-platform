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

import com.codeabovelab.dm.common.security.MultiTenancySupport;
import org.springframework.security.acls.model.Sid;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * context with some permission granting strategy parameters which used in {@link PermisionGrantingJudge } 
 */
public final class PermissionGrantingContext {
    private final TenantBasedPermissionGrantedStrategy strategy;
    private final Sid ownerSid;
    private final String ownerSidTenant;
    private final Set<String> currentSidTenants = new TreeSet<>();
    private final Set<String> currentSidTenantsWrapped = Collections.unmodifiableSet(currentSidTenants);
    private final String currentDefaultTenant;
    private Sid currentSid;
    private String currentSidTenant;
    private boolean hasAces;
    
    PermissionGrantingContext(TenantBasedPermissionGrantedStrategy strategy, Sid ownerSid, String currentDefaultTenant) {
        this.strategy = strategy;
        this.ownerSid = ownerSid;
        this.ownerSidTenant = this.strategy.getTenantFromSid(this.ownerSid);
        if(this.ownerSidTenant == MultiTenancySupport.NO_TENANT) {
            throw new IllegalArgumentException("ACL.owner.tenantId == MultiTenancySupport.NO_TENANT");
        }
        this.currentDefaultTenant = currentDefaultTenant;
    }

    /**
     * one of SIDs which passed to {@link org.springframework.security.acls.model.Acl#isGranted(java.util.List, java.util.List, boolean) } 
     * @return 
     */
    public Sid getCurrentSid() {
        return currentSid;
    }

    void setCurrentSid(Sid currentSid) {
        this.currentSid = currentSid;
        currentSidTenants.clear();
        this.currentSidTenant = strategy.getTenantFromSid(this.currentSid);
        if(this.currentSidTenant == MultiTenancySupport.NO_TENANT) {
            this.currentSidTenant = this.currentDefaultTenant;
        }
        currentSidTenants.add(currentSidTenant);
        strategy.tenantsService.getChildTenants(currentSidTenant, currentSidTenants);
    }

    /**
     * flag which indicate availability of ACE items in current ACL
     * @return 
     */
    public boolean isHasAces() {
        return hasAces;
    }

    void setHasAces(boolean hasAces) {
        this.hasAces = hasAces;
    }

    /**
     * owner value from current ACL
     * @return 
     */
    public Sid getOwnerSid() {
        return ownerSid;
    }
    
    /**
     * tenant of owner SID
     * @return 
     */
    public String getOwnerTenant() {
        return this.ownerSidTenant;
    }

    /**
     * unmodifiable set of available for currentSid tenants 
     * @return 
     */
    public Set<String> getCurrentTenants() {
        return currentSidTenantsWrapped;
    }
}
