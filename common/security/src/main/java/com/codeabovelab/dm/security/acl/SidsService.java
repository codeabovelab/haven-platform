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

package com.codeabovelab.dm.security.acl;

import com.codeabovelab.dm.common.security.MultiTenancySupport;
import com.codeabovelab.dm.common.security.TenantGrantedAuthoritySid;
import com.codeabovelab.dm.common.security.TenantPrincipalSid;
import com.codeabovelab.dm.security.entity.SidEntity;
import com.codeabovelab.dm.security.repository.SidsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.transaction.Transactional;

/**
 * service which do SID processing and translation between Sid and SidEntity
 */
@Component
@Transactional
class SidsService {
    private final SidsRepository sidsRepository;

    @Lazy
    @Autowired
    public SidsService(SidsRepository sidsRepository) {
        this.sidsRepository = sidsRepository;
    }
    
    
    Sid toSid(SidEntity sidEntity) {
        final boolean principal = sidEntity.isPrincipal();
        validate(sidEntity);
        final String tenant = sidEntity.getTenant();
        final String tenantId = tenant == null ? MultiTenancySupport.NO_TENANT : tenant;
        if(principal) {
            return new TenantPrincipalSid(sidEntity.getSid(), tenantId);
        } else {
            return new TenantGrantedAuthoritySid(sidEntity.getSid(), tenantId);
        }
    }
    
    SidEntity getOrCreate(Sid sid) {
        Assert.notNull(sid, "Sid is null");
        
        final boolean principal;
        final String sidText;
        if(sid instanceof PrincipalSid) {
            principal = true;
            sidText = ((PrincipalSid)sid).getPrincipal();
        } else if(sid instanceof GrantedAuthoritySid) {
            principal = false;
            sidText = ((GrantedAuthoritySid)sid).getGrantedAuthority();
        } else {
            throw new IllegalArgumentException("Unsupported sid " + sid.getClass());
        }
        final String tenantId = MultiTenancySupport.getTenant(sid);
        if(principal && tenantId == MultiTenancySupport.NO_TENANT) {
            throw new IllegalArgumentException("PrincipalSid must have valid tenantId, but " + sid + " has tenantId=" + tenantId);
        }
        SidEntity entity = sidsRepository.findByTenantIdAndSidAndPrincipal(tenantId, sidText, principal);
        if(entity == null) {
            entity = new SidEntity();
            entity.setPrincipal(principal);
            entity.setSid(sidText);
            entity.setTenant(tenantId);
            validate(entity);
            entity = sidsRepository.save(entity);
        }
        return entity;
    }

    private void validate(SidEntity entity) throws IllegalArgumentException {
        if(entity.getTenant() == null && entity.isPrincipal()) {
            throw new IllegalArgumentException("SidEntity with principal=true and tenant=null is not allowed: " + entity);
        }
    }
}
