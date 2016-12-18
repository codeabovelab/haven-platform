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

import com.codeabovelab.dm.common.security.Action;
import com.codeabovelab.dm.common.security.TenantGrantedAuthoritySid;
import com.codeabovelab.dm.common.security.TenantPrincipalSid;
import com.codeabovelab.dm.common.security.acl.AceSource;
import com.codeabovelab.dm.common.security.acl.AclSource;
import com.codeabovelab.dm.common.security.acl.AclUtils;
import com.codeabovelab.dm.common.security.acl.TenantSid;
import com.google.common.base.Splitter;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.acls.domain.CumulativePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.Map;

/**
 */
@ConfigurationProperties("dm.security.acl")
@Data
public class PropertyAclServiceConfigurer implements AclServiceConfigurer {

    private static final Splitter ACL_SPLITTER = Splitter.on(',').trimResults();
    private static final Splitter ACE_SPLITTER = Splitter.on(' ').trimResults();
    private Map<String, String> list;

    @Override
    public void configure(ConfigurableAclService.Builder builder) {
        if(list != null) {
            for(Map.Entry<String, String> e: list.entrySet()) {
                String value = e.getValue();
                String id = e.getKey();
                AclSource.Builder asb = parse(id, value);
                builder.putAcl(asb.build());
            }
        }
    }

    /**
     * Parser expressions like value from property:
     * <pre>
     *                                                     values from {@link Action#getLetter()}
     *                                                                             \___
     *                                                                             /   \
     * dm.security.acl.list[CONTAINER@cont234] = system@root, grant sam@root       R    , grant ROLE_USER@root CRUDE
     * dm.security.acl.list[CONTAINER@cont235] = system@root, grant ROLE_USER@root CRUDE
     *                      \_type__/ \_id__/    \____/ \__/  \___/ \_______/ \__/
     *                                         owner^    /      |     |        ^tenant
     *                                            tenant^       |   role or user (role always start with 'ROLE_')
     *                                       'grant' or 'revoke'^
     * </pre>
     * @param id
     * @param value
     * @return
     */
    static AclSource.Builder parse(String id, String value) {
        AclSource.Builder asb = AclSource.builder();
        id = id.replace('@', ':');// property does not allow ':' in key
        asb.setObjectIdentity(AclUtils.fromId(id));

        Iterator<String> it = ACL_SPLITTER.split(value).iterator();
        if(it.hasNext()) {
            asb.setOwner(parseSid(it.next()));
            while (it.hasNext()) {
                asb.addEntry(parseAce(it.next()));
            }
        }

        return asb;
    }

    private static AceSource parseAce(String token) {
        //grant ROLE_USER@root CRUDE
        Iterator<String> it = ACE_SPLITTER.split(token).iterator();
        String grantStr = it.next();
        AceSource.Builder asb = AceSource.builder();
        switch (grantStr) {
            case "grant":
                asb.granting(true);
                break;
            case "revoke":
                asb.granting(false);
                break;
            default:
                throw new IllegalArgumentException("rule: " + token + " must start with 'grant' or 'revoke'");
        }
        try {
            TenantSid sid = parseSid(it.next());
            asb.sid(sid);
        } catch (Exception e) {
            throw new IllegalArgumentException("rule: " + token + " contains invalid sid", e);
        }
        String perms = it.next();
        asb.permission(parsePerms(perms));
        if(it.hasNext()) {
            throw new IllegalArgumentException("Too long rule: " + token + " we expect only three space delimited items");
        }
        return asb.build();
    }

    private static Permission parsePerms(String perms) {

        final int length = perms.length();
        if(length > 32) {
            throw new IllegalArgumentException("Too long permission expression: " + perms + " it must be shortest than 32 chars.");
        }
        Permission perm;
        if(length == 1) {
            perm = parseLetter(perms.charAt(0));
        } else {
            CumulativePermission cp = new CumulativePermission();
            for(int i = 0; i < length; ++i) {
                cp.set(parseLetter(perms.charAt(i)));
            }
            perm = cp;
        }
        return perm;
    }

    private static Action parseLetter(char c) {
        Action perm = Action.fromLetter(c);
        if(perm == null) {
            throw new IllegalArgumentException("Unknown action letter : " + c);
        }
        return perm;
    }

    private static TenantSid parseSid(String token) {
        String[] arr = StringUtils.split(token, "@");
        if(arr == null) {
            throw new IllegalArgumentException("Can not parse sid: " + token + " expect something like 'text@text'");
        }
        if(token.startsWith("ROLE_")) {
            return new TenantGrantedAuthoritySid(arr[0], arr[1]);
        } else {
            return new TenantPrincipalSid(arr[0], arr[1]);
        }
    }
}
