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

package com.codeabovelab.dm.cluman.ds.nodes;

import com.codeabovelab.dm.cluman.security.SecuredType;
import com.codeabovelab.dm.cluman.security.VirtualAclProvider;
import com.codeabovelab.dm.common.security.dto.ObjectIdentityData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.stereotype.Component;

import java.io.Serializable;


/**
 */
@Component("NODE" /*see secured type*/)
public class NodesAclProvider extends VirtualAclProvider {

    @Autowired
    private NodeStorage nodeStorage;

    protected ObjectIdentityData toOid(Serializable id) {
        return SecuredType.NODE.id((String) id);
    }

    @Override
    protected String getCluster(Serializable id) {
        String strId = (String) id;
        NodeRegistration nr = nodeStorage.getNodeRegistrationInternal(strId);
        if (nr == null) {
            throw new NotFoundException("Node '" + id + "' is not registered.");
        }
        return nr.getNodeInfo().getCluster();
    }
}
