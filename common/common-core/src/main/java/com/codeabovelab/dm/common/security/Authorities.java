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

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.function.Function;

/**
 * list of default granted authorities
 */
public final class Authorities {

    private Authorities() {
    }
    
    public static final String ADMIN_ROLE = "ROLE_ADMIN";
    public static final String USER_ROLE = "ROLE_USER";

    public static final TenantGrantedAuthority ADMIN = fromName(ADMIN_ROLE);
    public static final TenantGrantedAuthority USER = fromName(USER_ROLE);

    /**
     * Make authority from its name.
     * @param name
     * @return
     */
    public static TenantGrantedAuthority fromName(String name) {
        return fromName(name, MultiTenancySupport.NO_TENANT);
    }

    /**
     * Make authority from its name.
     * @param name
     * @param tenant
     * @return
     */
    public static TenantGrantedAuthority fromName(String name, String tenant) {
        name = name.toUpperCase();
        if(!name.startsWith("ROLE_")) {
            name = "ROLE_" + name;
        }
        return new GrantedAuthorityImpl(name, tenant);
    }

    /**
     * Apply specified function to each authority. When a function return 'true' then loop will be broken and return 'true'.
     * In any other cases return false.
     * @param userDetails
     * @param authorityChecker
     * @return
     */
    public static boolean checkAuthorities(UserDetails userDetails, Function<GrantedAuthority, Boolean> authorityChecker) {
        if(userDetails == null) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        if(authorities == null) {
            return false;
        }
        for(GrantedAuthority authority: authorities) {
            Boolean res = authorityChecker.apply(authority);
            if(res != null && res) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return 'true' if user has any of specified authorities.
     * @param userDetails
     * @param authoritiesNames
     * @return
     */
    public static boolean hasAnyOfAuthorities(UserDetails userDetails, String ... authoritiesNames) {
        Set<String> set = new HashSet<>(Arrays.asList(authoritiesNames));
        return checkAuthorities(userDetails, new AnyAuthorityChecker(set));
    }

    /**
     * Return name of admin authority of specified type.
     * @param type
     * @return
     */
    public static String adminOf(String type) {
        return "ROLE_" + type.toUpperCase() + "_ADMIN";
    }

    /**
     * Return name of user authority of specified type.
     * @param type
     * @return
     */
    public static String userOf(String type) {
        return "ROLE_" + type.toUpperCase() + "_USER";
    }

    private static class AnyAuthorityChecker implements Function<GrantedAuthority, Boolean> {
        private final Set<String> set;

        public AnyAuthorityChecker(Set<String> set) {
            this.set = set;
        }

        @Override
        public Boolean apply(GrantedAuthority grantedAuthority) {
            return set.contains(grantedAuthority.getAuthority());
        }
    }
}
