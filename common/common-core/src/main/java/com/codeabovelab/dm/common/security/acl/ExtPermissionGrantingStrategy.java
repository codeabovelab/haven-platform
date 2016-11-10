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

package com.codeabovelab.dm.common.security.acl;

import com.codeabovelab.dm.common.security.dto.PermissionData;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.acls.model.Sid;

import java.util.List;

/**
 */
public interface ExtPermissionGrantingStrategy extends PermissionGrantingStrategy {
    /**
     * Collecting ACE entries and default permissions to single permission fo specified sids.
     * @param acl
     * @param sids
     * @return collected permission
     */
    PermissionData getPermission(Acl acl, List<Sid> sids);
}
