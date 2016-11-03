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
import com.codeabovelab.dm.common.security.GrantedAuthorityImpl;
import com.codeabovelab.dm.common.security.MultiTenancySupport;
import com.google.common.collect.ImmutableMap;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 */
public class RoleHierarchyImpl implements RoleHierarchy, AuthoritiesService {


    public static class Builder {
        private final Map<String, Set<String>> map = new HashMap<>();

        public RoleHierarchyImpl build() {
            return new RoleHierarchyImpl(this);
        }

        /**
         * Add 'role -> role1 -> role2' relation.
         * @param roles
         * @return
         */
        public Builder chain(String ... roles) {
            Assert.notEmpty(roles, "chain is empty or null");
            Assert.isTrue(roles.length > 1, "chain must have more than one role");
            for(int i = 0; i < roles.length; ++i) {
                String parent = roles[i];
                String child = roles[i + 1];
                getSet(parent).add(child);
            }
            return this;
        }

        /**
         * Add 'role -> {child1, child2}' relation.
         * @param role
         * @param childs
         * @return
         */
        public Builder childs(String role, String ... childs) {
            getSet(role).addAll(Arrays.asList(childs));
            return this;
        }

        private Set<String> getSet(String parent) {
            return map.computeIfAbsent(parent, (s) -> new HashSet<>());
        }
    }

    private final Map<String, Set<String>> map;
    private final Collection<GrantedAuthority> allAuthorities;

    private RoleHierarchyImpl(Builder builder) {
        Map<String, Set<String>> src = builder.map;

        //gather all defined authorities
        Set<String> all = new HashSet<>();
        all.addAll(src.keySet());
        src.values().stream().forEach(all::addAll);

        // store authorities
        this.allAuthorities = Collections.unmodifiableList(all.stream().map(Authorities::fromName).collect(Collectors.toList()));

        //build map 'authority' -> 'all its childs'
        Map<String, Set<String>> dst = new HashMap<>();
        for(String authority: all) {
            Set<String> dstchilds = new HashSet<>();
            addChilds(src, authority, dstchilds);
            dst.put(authority, Collections.unmodifiableSet(dstchilds));
        }
        this.map = Collections.unmodifiableMap(dst);
    }

    private static void addChilds(Map<String, Set<String>> src, String authority, Set<String> dest) {
        Set<String> childs = src.get(authority);
        if(childs == null) {
            return;
        }
        for(String child: childs) {
            if(dest.add(child)) {
                addChilds(src, child, dest);
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Collection<? extends GrantedAuthority> getReachableGrantedAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<GrantedAuthority> res = new HashSet<>();
        res.addAll(authorities);
        for(GrantedAuthority authority: authorities) {
            String tenant = MultiTenancySupport.getTenant(authority);
            Set<String> childs = map.get(authority.getAuthority());
            if(childs == null) {
                continue;
            }
            childs.stream().map((a) -> new GrantedAuthorityImpl(a, tenant)).forEach(res::add);
        }
        return Collections.unmodifiableSet(res);
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return allAuthorities;
    }
}
