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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.GrantedAuthority;

import java.beans.ConstructorProperties;
import java.util.Objects;

/**
 * implementation of granted authority with tenant support
 *
 */
public class GrantedAuthorityImpl implements TenantGrantedAuthority {
    private final String tenantId;
    private final String authority;

    @JsonCreator
    @ConstructorProperties({"authority", "tenantId"})
    public GrantedAuthorityImpl(@JsonProperty("authority") String authority,
                                @JsonProperty("tenant") String tenant) {
        this.tenantId = tenant;
        this.authority = authority;
    }

    @Override
    public String getTenant() {
        return tenantId;
    }

    @Override
    public String getAuthority() {
        return authority;
    }

    @JsonIgnore
    @Override
    public String getAttribute() {
        return authority;
    }

    @Override
    public String toString() {
        return "GrantedAuthorityImpl{" + "tenantId=" + tenantId + ", authority='" + authority + '\'' + '}';
    }

    /**
     * create instance with data from specified authority
     * @param authority
     * @return
     */
    public static GrantedAuthorityImpl from(GrantedAuthority authority) {
        return new GrantedAuthorityImpl(authority.getAuthority(), MultiTenancySupport.getTenant(authority));
    }

    public static GrantedAuthority convert(GrantedAuthority authority) {
        if(authority instanceof GrantedAuthorityImpl) {
            return authority;
        }
        return from(authority);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GrantedAuthorityImpl that = (GrantedAuthorityImpl) o;
        return Objects.equals(tenantId, that.tenantId) &&
                Objects.equals(authority, that.authority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, authority);
    }
}
