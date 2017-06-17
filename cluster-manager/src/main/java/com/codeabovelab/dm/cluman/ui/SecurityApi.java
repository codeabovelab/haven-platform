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

package com.codeabovelab.dm.cluman.ui;

import com.codeabovelab.dm.cluman.model.NotFoundException;
import com.codeabovelab.dm.cluman.security.AbstractAclService;
import com.codeabovelab.dm.cluman.security.AuthoritiesService;
import com.codeabovelab.dm.cluman.security.ProvidersAclService;
import com.codeabovelab.dm.cluman.security.SecuredType;
import com.codeabovelab.dm.cluman.ui.model.UIResult;
import com.codeabovelab.dm.cluman.ui.model.UiAclUpdate;
import com.codeabovelab.dm.cluman.ui.model.UiRole;
import com.codeabovelab.dm.cluman.users.UsersStorage;
import com.codeabovelab.dm.common.security.Authorities;
import com.codeabovelab.dm.common.security.MultiTenancySupport;
import com.codeabovelab.dm.common.security.UserIdentifiersDetailsService;
import com.codeabovelab.dm.common.security.acl.AceSource;
import com.codeabovelab.dm.common.security.acl.AclSource;
import com.codeabovelab.dm.common.security.dto.ObjectIdentityData;
import com.codeabovelab.dm.common.utils.Sugar;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 */
@RestController
@Secured(Authorities.ADMIN_ROLE)
@RequestMapping(value = "/ui/api", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SecurityApi {

    private final UserIdentifiersDetailsService usersService;
    private final UsersStorage usersStorage;
    private final AuthoritiesService authoritiesService;
    private final PasswordEncoder passwordEncoder;
    private final AbstractAclService aclService;
    private final ProvidersAclService providersAclService;


    @Secured(Authorities.USER_ROLE)
    @RequestMapping(value = "/roles/", method = RequestMethod.GET)
    public Collection<UiRole> getGroups() {
        Collection<GrantedAuthority> authorities = authoritiesService.getAuthorities();
        return authorities.stream().map(UiRole::fromAuthority).collect(Collectors.toList());
    }

    @RequestMapping(path = "/acl/", method = RequestMethod.GET)
    public List<String> getSecuredTypes() {
        return Arrays.stream(SecuredType.values()).map(SecuredType::name).collect(Collectors.toList());
    }

    @ApiOperation("Batch update of ACLs. Not that, due to we can not guarantee consistency of batch update, we return " +
      "map with result of updating each ACL.")
    @RequestMapping(path = "/acl/", method = RequestMethod.POST)
    public Map<String, UIResult> setAcls(@RequestBody Map<ObjectIdentityData, UiAclUpdate> acls) {
        // we save order of calls
        Map<String, UIResult> results = new LinkedHashMap<>();
        acls.forEach((oid, aclSource) -> {
            try {
                providersAclService.updateAclSource(oid, as -> updateAcl(aclSource, as));
            } catch (org.springframework.security.acls.model.NotFoundException e) {
                throw new NotFoundException(e);
            }
        });
        return results;
    }

    @RequestMapping(path = "/acl/{type}/{id}", method = RequestMethod.GET)
    public AclSource getAcl(@PathVariable("type") String type, @PathVariable("id") String id) {
        SecuredType securedType = SecuredType.valueOf(type);
        ObjectIdentity oid = securedType.id(id);
        try {
            return aclService.getAclSource(oid);
        } catch (org.springframework.security.acls.model.NotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    @RequestMapping(path = "/acl/{type}/{id}", method = RequestMethod.POST)
    public void setAcl(@PathVariable("type") String type, @PathVariable("id") String id, @RequestBody UiAclUpdate aclSource) {
        SecuredType securedType = SecuredType.valueOf(type);
        ObjectIdentity oid = securedType.id(id);
        try {
            providersAclService.updateAclSource(oid, as -> updateAcl(aclSource, as));
        } catch (org.springframework.security.acls.model.NotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    private boolean updateAcl(UiAclUpdate aclSource, AclSource.Builder as) {
        Sugar.setIfNotNull(as::setOwner, MultiTenancySupport.fixTenant(aclSource.getOwner()));
        List<UiAclUpdate.UiAceUpdate> list = aclSource.getEntries();
        Map<String, AceSource> existed = as.getEntries();
        if(list.isEmpty()) {
            return false;
        }
        for (UiAclUpdate.UiAceUpdate entry : list) {
            String aceId = entry.getId();
            AceSource ace = aceId == null ? null : existed.get(aceId);
            if (ace == null) {
                // add new
                AceSource.Builder b = AceSource.builder();
                // note that id may be null, it is normal
                b.setId(aceId);
                Sugar.setIfNotNull(b::setAuditFailure, entry.getAuditFailure());
                Sugar.setIfNotNull(b::setAuditSuccess, entry.getAuditSuccess());
                b.setSid(entry.getSid());
                b.setGranting(entry.getGranting());
                b.setPermission(entry.getPermission());
                as.addEntry(b.build());
                continue;
            }
            if (entry.isDelete()) {
                existed.remove(aceId);
                continue;
            }
            // modify existed
            AceSource.Builder b = AceSource.builder().from(ace);
            b.setId(aceId);
            Sugar.setIfNotNull(b::setAuditFailure, entry.getAuditFailure());
            Sugar.setIfNotNull(b::setAuditSuccess, entry.getAuditSuccess());
            Sugar.setIfNotNull(b::setSid, entry.getSid());
            Sugar.setIfNotNull(b::setGranting, entry.getGranting());
            Sugar.setIfNotNull(b::setPermission, entry.getPermission());
            as.addEntry(b.build());
        }
        return true;
    }
}
