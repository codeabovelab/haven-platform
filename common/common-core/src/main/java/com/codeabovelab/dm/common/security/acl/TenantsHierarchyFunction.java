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

import java.util.Collection;

/**
 * service which provide mapping between tenant -> childTenants
 */
public interface TenantsHierarchyFunction {
    
    /**
     * add to specified collection child tenants
     * @param tenantId
     * @param childTenants target collection in which will be added child tenants
     */
    void getChildTenants(Long tenantId, Collection<Long> childTenants);
}
