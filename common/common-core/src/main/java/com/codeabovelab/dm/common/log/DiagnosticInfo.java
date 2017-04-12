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

package com.codeabovelab.dm.common.log;

import lombok.AllArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


/**
 * utility which collect info for {@link org.slf4j.MDC}
 */
@Component
@AllArgsConstructor
public class DiagnosticInfo {

    public static final String KEY_USER = "KEY_USER";
    public static final String REQUEST_UUID = "REQUEST_UUID";

    private final UUIDGenerator uuidGenerator;

    public String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null) {
            return null;
        }
        return authentication.getName();
    }

    /**
     * do inject diagnostic info to MDC context
     * @return
     * @param requestUid
     */
    public final AutoCloseable injectToContext(String requestUid, String description) {
        if (requestUid == null) {
            requestUid = uuidGenerator.generate();
        }
        MDC.put(KEY_USER, getCurrentUser());
        MDC.put(REQUEST_UUID, requestUid);
        return () -> {
            MDC.remove(KEY_USER);
            MDC.remove(REQUEST_UUID);
        };
    }
}
