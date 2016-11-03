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

package com.codeabovelab.dm.cluman.ui.model;

import com.codeabovelab.dm.common.security.ExtendedUserDetails;
import com.codeabovelab.dm.common.security.ExtendedUserDetailsImpl;
import com.codeabovelab.dm.common.security.MultiTenancySupport;
import com.codeabovelab.dm.common.utils.Sugar;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
@Data
public class UiUser {
    /**
     * Stub for any non null password
     */
    public static final String PWD_STUB = "********";
    private String user;
    private String title;
    private String email;
    private String tenant;
    private String password;
    private List<UiRole> roles;
    private List<UiPermission> permissions;
    private Boolean accountNonExpired;
    private Boolean accountNonLocked;
    private Boolean credentialsNonExpired;
    private Boolean enabled;

    public static UiUser fromDetails(UserDetails details) {
        UiUser user = new UiUser();
        String username = details.getUsername();
        user.setUser(username);
        if(details instanceof ExtendedUserDetails) {
            ExtendedUserDetails eud = (ExtendedUserDetails) details;
            user.setTitle(eud.getTitle());
            user.setTenant(eud.getTenant());
            user.setEmail(eud.getEmail());
        }
        user.setPassword(details.getPassword() == null? null : PWD_STUB);
        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();
        List<UiRole> roles = authorities.stream().map(UiRole::fromAuthority).collect(Collectors.toList());
        roles.sort(null);
        user.setRoles(roles);
        user.setTenant(MultiTenancySupport.getTenant(details));
        return user;
    }

    public void toBuilder(ExtendedUserDetailsImpl.Builder builder) {
        Sugar.setIfNotNull(builder::setTenant, getTenant());
        Sugar.setIfNotNull(builder::setEmail, getEmail());
        Sugar.setIfNotNull(builder::setTitle, getTitle());
        String password = getPassword();
        if(password != null && !PWD_STUB.equals(password)) {
            builder.setPassword(password);
        }
        Sugar.setIfNotNull(builder::setUsername, getUser());
        Sugar.setIfNotNull(builder::setAccountNonExpired, getAccountNonExpired());
        Sugar.setIfNotNull(builder::setCredentialsNonExpired, getCredentialsNonExpired());
        Sugar.setIfNotNull(builder::setAccountNonLocked, getAccountNonLocked());
        Sugar.setIfNotNull(builder::setEnabled, getEnabled());
    }
}
