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

import com.codeabovelab.dm.cluman.security.AclContext;
import com.codeabovelab.dm.cluman.security.SecuredType;
import com.codeabovelab.dm.common.security.acl.AclUtils;
import com.codeabovelab.dm.common.security.dto.ObjectIdentityData;
import com.codeabovelab.dm.common.security.dto.PermissionData;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.security.acls.model.ObjectIdentity;

/**
 */
@Data
public class UiPermission {
    /**
     * String with chars from Action.
     * @see com.codeabovelab.dm.common.security.dto.PermissionData#getExpression()
     * @see com.codeabovelab.dm.common.security.Action
     */
    @ApiModelProperty("String expression like 'CRUDEA'")
    private String expr;

    @ApiModelProperty("Secured object identifier, used for manage security data (like ACL) of this object.")
    private String oid;

    public static UiPermission create() {
        return new UiPermission();
    }

    public static UiPermission create(AclContext ac, ObjectIdentityData oid) {
        UiPermission up = new UiPermission();
        up.oid(oid);
        up.permission(ac.getPermission(oid));
        return up;
    }


    public UiPermission permission(PermissionData permission) {
        setExpr(permission.getExpression());
        return this;
    }

    public UiPermission oid(ObjectIdentity oid) {
        setOid(AclUtils.toId(oid));
        return this;
    }

    public static void inject(WithUiPermission target, AclContext ac, ObjectIdentityData oid) {
        target.setPermission(UiPermission.create(ac, oid));
    }
}
