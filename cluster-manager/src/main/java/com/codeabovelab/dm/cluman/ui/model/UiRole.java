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

package com.codeabovelab.dm.cluman.ui.model;

import com.codeabovelab.dm.common.security.MultiTenancySupport;
import com.codeabovelab.dm.common.utils.Comparables;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

/**
 */
@Data
public class UiRole implements Comparable<UiRole> {
    private String name;
    private String tenant;

    public static UiRole fromAuthority(GrantedAuthority authority) {
        UiRole g = new UiRole();
        g.setName(authority.getAuthority());
        g.setTenant(MultiTenancySupport.getTenant(authority));
        return g;
    }

    @Override
    public int compareTo(UiRole o) {
        int compare = Comparables.compare(this.tenant, o.tenant);
        if(compare == 0) {
            compare = Comparables.compare(this.name, o.name);
        }
        return compare;
    }
}
