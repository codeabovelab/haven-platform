/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.common.security.acl;

import com.codeabovelab.dm.common.security.OwnedByTenant;
import com.codeabovelab.dm.common.security.TenantGrantedAuthoritySid;
import com.codeabovelab.dm.common.security.TenantPrincipalSid;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;

/**
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = TenantPrincipalSid.class)
@JsonSubTypes({@JsonSubTypes.Type(TenantPrincipalSid.class), @JsonSubTypes.Type(TenantGrantedAuthoritySid.class)})
public interface TenantSid extends Sid, OwnedByTenant {
    static TenantSid from(Sid sid) {
        if(sid == null) {
            return null;
        }
        if(sid instanceof TenantSid) {
            return (TenantSid) sid;
        } else if(sid instanceof PrincipalSid) {
            return TenantPrincipalSid.from((PrincipalSid) sid);
        } else if(sid instanceof GrantedAuthoritySid) {
            return TenantGrantedAuthoritySid.from((GrantedAuthoritySid) sid);
        } else {
            throw new IllegalArgumentException("Unsupported sid type: " + sid.getClass());
        }
    }
}
