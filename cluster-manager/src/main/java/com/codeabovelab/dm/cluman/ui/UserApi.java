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

import com.codeabovelab.dm.cluman.security.AbstractAclService;
import com.codeabovelab.dm.cluman.security.AuthoritiesService;
import com.codeabovelab.dm.cluman.security.ProvidersAclService;
import com.codeabovelab.dm.cluman.ui.model.UiRole;
import com.codeabovelab.dm.cluman.ui.model.UiRoleUpdate;
import com.codeabovelab.dm.cluman.ui.model.UiUser;
import com.codeabovelab.dm.cluman.ui.model.UiUserUpdate;
import com.codeabovelab.dm.cluman.users.UserRegistration;
import com.codeabovelab.dm.cluman.users.UsersStorage;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.codeabovelab.dm.common.security.Authorities;
import com.codeabovelab.dm.common.security.ExtendedUserDetails;
import com.codeabovelab.dm.common.security.ExtendedUserDetailsImpl;
import com.codeabovelab.dm.common.security.UserIdentifiersDetailsService;
import com.codeabovelab.dm.common.security.acl.AclSource;
import com.codeabovelab.dm.common.security.acl.AclUtils;
import com.codeabovelab.dm.common.security.dto.ObjectIdentityData;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 */
@RestController
@Secured(Authorities.ADMIN_ROLE)
@RequestMapping(value = "/ui/api/users", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserApi {

    private final UserIdentifiersDetailsService usersService;
    private final UsersStorage usersStorage;
    private final AuthoritiesService authoritiesService;
    private final PasswordEncoder passwordEncoder;
    private final AbstractAclService aclService;
    private final ProvidersAclService providersAclService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public Collection<UiUser> getUsers() {
        Collection<ExtendedUserDetails> users = usersService.getUsers();
        return users.stream().map(UiUser::fromDetails).collect(Collectors.toList());
    }

    @RequestMapping(value = "/{user}", method = RequestMethod.GET)
    public UiUser getUser(@PathVariable("user") String username) {
        ExtendedUserDetails user = getUserDetails(username);
        return UiUser.fromDetails(user);
    }

    private ExtendedUserDetails getUserDetails(String username) {
        ExtendedUserDetails user;
        try {
            user = usersService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            user = null;
        }
        if(user == null) {
            throw new HttpException(HttpStatus.NOT_FOUND, "Can not find user with name: " + username);
        }
        return user;
    }

    @PreAuthorize("#username == authentication.name || hasRole('ADMIN')")
    @RequestMapping(value = "/{user}", method = RequestMethod.POST)
    public UiUser setUser(@PathVariable("user") String username, @RequestBody UiUserUpdate user) {
        user.setUser(username);
        String password = user.getPassword();
        // we must encode password
        if(password != null && !UiUser.PWD_STUB.equals(password)) {
            String encodedPwd = passwordEncoder.encode(password);
            user.setPassword(encodedPwd);
        }
        final ExtendedUserDetails source;
        {
            // we load user because it can be defined in different sources,
            // but must stored into userStorage
            ExtendedUserDetails eud = null;
            try {
                eud = usersService.loadUserByUsername(username);
            } catch (UsernameNotFoundException e) {
                //is a usual case
            }
            source = eud;
        }
        UserRegistration reg = usersStorage.update(username, (ur) -> {
            ExtendedUserDetails details = ur.getDetails();
            if(details == null && source != null) {
                // if details is null than user Storage does not have this user before
                // and we can transfer our user into it
                details = source;
            }
            ExtendedUserDetailsImpl.Builder builder = ExtendedUserDetailsImpl.builder(details);
            user.toBuilder(builder);
            ur.setDetails(builder);
        });
        return UiUser.fromDetails(reg.getDetails());
    }

    @RequestMapping(value = "/{user}", method = RequestMethod.DELETE)
    public void deleteUser(@PathVariable("user") String username) {
        usersStorage.remove(username);
    }

    @Secured(Authorities.USER_ROLE)
    @RequestMapping(value = "/current/", method = RequestMethod.GET)
    public UiUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        return UiUser.fromDetails(userDetails);
    }

    @RequestMapping(value = "/{user}/roles/", method = RequestMethod.GET)
    public List<UiRole> getUserRoles(@PathVariable("user") String username) {
        ExtendedUserDetails details = getUserDetails(username);
        List<UiRole> roles = details.getAuthorities().stream().map(UiRole::fromAuthority).collect(Collectors.toList());
        roles.sort(null);
        return roles;
    }

    @RequestMapping(value = "/{user}/roles/", method = RequestMethod.POST)
    public List<UiRole> updateUserRoles(@PathVariable("user") String username, @RequestBody List<UiRoleUpdate> updatedRoles) {
        UserRegistration ur = usersStorage.get(username);
        ExtendedAssert.notFound(ur, "Can not find user: " + username);
        if(!updatedRoles.isEmpty()) {
            ur.update((r) -> {
                ExtendedUserDetailsImpl.Builder builder = ExtendedUserDetailsImpl.builder(ur.getDetails());
                UiUserUpdate.updateRoles(updatedRoles, builder);
                r.setDetails(builder);
            });
        }
        ExtendedUserDetails details = ur.getDetails();
        List<UiRole> roles = details.getAuthorities().stream().map(UiRole::fromAuthority).collect(Collectors.toList());
        roles.sort(null);
        return roles;
    }

    @RequestMapping(path = "/{user}/acls/", method = RequestMethod.GET)
    public Map<ObjectIdentityData, AclSource> getUserAcls(@PathVariable("user") String username) {
        Map<ObjectIdentityData, AclSource> map = new HashMap<>();
        providersAclService.getAcls((a) -> {
            if(!AclUtils.isContainsUser(a, username)) {
                return;
            }
            // we must serialize as our object, it allow save it as correct string
            map.put(a.getObjectIdentity(), a);
        });
        return map;
    }

}
