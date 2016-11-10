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

import com.codeabovelab.dm.common.security.acl.ExtPermissionGrantingStrategy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 */
public class StandardAclContextFactory implements AclContextFactory {

    private final AclService aclService;
    private final ExtPermissionGrantingStrategy pgs;
    private final SidRetrievalStrategy sidStrategy;

    public StandardAclContextFactory(AclService aclService, ExtPermissionGrantingStrategy pgs, SidRetrievalStrategy sidStrategy) {
        this.aclService = aclService;
        this.pgs = pgs;
        this.sidStrategy = sidStrategy;
    }

    @Override
    public AclContext getContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Sid> sids;
        if(authentication == null) {
            throw new AccessDeniedException("No credentials in context.");
        } else {
            sids = sidStrategy.getSids(authentication);
        }
        return new AclContext(aclService, pgs, sids);
    }
}
