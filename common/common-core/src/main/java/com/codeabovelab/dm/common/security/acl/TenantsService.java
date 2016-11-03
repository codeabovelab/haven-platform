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

package com.codeabovelab.dm.common.security.acl;

import com.codeabovelab.dm.common.security.MultiTenancySupport;
import com.codeabovelab.dm.common.security.Tenant;

import java.util.Collection;
import java.util.List;

/**
 */
public abstract class TenantsService<T extends Tenant> {
    public abstract boolean isRoot(String tenant);

    /**
     * add to specified collection child tenants
     * @param tenantName
     * @param childTenants target collection in which will be added child tenants
     */
    public void getChildTenants(String tenantName, Collection<String> childTenants) {
        if(tenantName == MultiTenancySupport.NO_TENANT) {
            return;
        }

        final T tenant = getTenant(tenantName);
        if(tenant == null) {
            return;
        }
        load(tenant, childTenants);
    }

    protected abstract T getTenant(String tenant);
    protected abstract List<T> getChilds(T tenant);

    void load(T tenant, Collection<String> childTenants) {
        List<T> childs = getChilds(tenant);
        if(childs == null) {
            return;
        }
        for(T child: childs) {
            childTenants.add(child.getName());
            load(child, childTenants);
        }
    }

}
