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

package com.codeabovelab.dm.cluman.ds.clusters;

import com.codeabovelab.dm.cluman.model.DiscoveryStorage;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import com.codeabovelab.dm.cluman.security.AclModifier;
import com.codeabovelab.dm.cluman.security.AclProvider;
import com.codeabovelab.dm.common.security.acl.AclSource;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 */
@Component("CLUSTER" /*See SecuredType*/)
public class ClusterAclProvider implements AclProvider {
    // we user ObjectFactory for prevent circular dependency
    private final ObjectFactory<DiscoveryStorage> discoveryStorage;

    @Autowired
    public ClusterAclProvider(ObjectFactory<DiscoveryStorage> discoveryStorage) {
        this.discoveryStorage = discoveryStorage;
    }

    @Override
    public AclSource provide(Serializable id) {
        DiscoveryStorage ds = discoveryStorage.getObject();
        NodesGroup cluster = ds.getCluster((String) id);
        return cluster == null? null : cluster.getAcl();
    }

    @Override
    public void update(Serializable id, AclModifier operator) {
        DiscoveryStorage ds = discoveryStorage.getObject();
        NodesGroup cluster = ds.getCluster((String) id);
        if(cluster == null) {
            throw new NotFoundException("can not found object for id: " + id);
        }
        cluster.updateAcl(operator);
    }
}
