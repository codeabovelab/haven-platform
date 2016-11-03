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

import com.codeabovelab.dm.common.security.MultiTenancySupport;
import com.codeabovelab.dm.common.security.Tenant;
import com.codeabovelab.dm.common.security.acl.TenantsService;

import java.util.Collections;
import java.util.List;

/**
 * Temporary stub implementation for tenant service.
 */
public class StubTenantsService extends TenantsService<Tenant> {

    @Override
    public boolean isRoot(String tenant) {
        return MultiTenancySupport.ROOT_TENANT.equals(tenant);
    }

    @Override
    protected Tenant getTenant(String tenant) {
        return () -> tenant;
    }

    @Override
    protected List<Tenant> getChilds(Tenant tenant) {
        return Collections.emptyList();
    }
}
