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

package com.codeabovelab.dm.cluman.security;

import com.codeabovelab.dm.cluman.ds.clusters.ClusterAclProvider;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.common.security.*;
import com.codeabovelab.dm.common.security.acl.AceSource;
import com.codeabovelab.dm.common.security.acl.AclSource;
import com.codeabovelab.dm.common.security.dto.ObjectIdentityData;
import com.codeabovelab.dm.common.security.dto.PermissionData;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 */
public abstract class VirtualAclProvider implements AclProvider {

    private static final TenantPrincipalSid PRINCIPAL_SYS = TenantPrincipalSid.from(SecurityUtils.USER_SYSTEM);
    private static final TenantGrantedAuthoritySid GA_USER = TenantGrantedAuthoritySid.from(Authorities.USER);

    @Autowired
    private NodeStorage nodeStorage;

    @Autowired
    private ClusterAclProvider clusterAclProvider;

    @Override
    public AclSource provide(Serializable id) {
        String cluster = getCluster(id);
        if(cluster == null) {
            // when node is unbound to any cluster we grant all to any user
            return makeAcl(id);
        }
        AclSource clusterAcl = clusterAclProvider.provide(cluster);
        AclSource.Builder aclsb = AclSource.builder().objectIdentity(toOid(id))
          .owner(clusterAcl.getOwner());
        clusterAcl.getEntries().forEach(ace -> {
            boolean alterInside = ace.getPermission().has(Action.ALTER_INSIDE);
            boolean read = ace.getPermission().has(Action.READ);
            if(!read && !alterInside) {
                return;
            }
            boolean granting = ace.isGranting();
            PermissionData.Builder pdb = PermissionData.builder();
            if(read) {
                pdb.add(Action.READ);
            }
            if(alterInside) {
                // cluster 'alter' mean that user can do anything with underline objects
                pdb.add(PermissionData.ALL);
                if(!granting && !read) {
                    // if read is not specified before and it is 'revoke' entry, then we must remove READ
                    pdb.remove(Action.READ);
                }
            }
            aclsb.addEntry(AceSource.builder()
              .id(ace.getId())
              .sid(ace.getSid())
              .granting(granting)
              .permission(pdb)
              .build());
        });
        return aclsb.build();
    }

    protected abstract String getCluster(Serializable id);

    protected abstract ObjectIdentityData toOid(Serializable id);

    private AclSource makeAcl(Serializable id) {
        return AclSource.builder().objectIdentity(toOid(id))
          .owner(PRINCIPAL_SYS)
          .addEntry(AceSource.builder()
            .id(GA_USER.toString())
            .sid(GA_USER)
            .granting(true)
            .permission(PermissionData.ALL)
            .build())
          .build();
    }

    @Override
    public void update(Serializable id, AclModifier operator) {
        //not support yet
    }

    @Override
    public void list(Consumer<AclSource> consumer) {
        // we do not list acls because its is 'virtual' ans can not be modified
    }
}
