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

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


/**
 * utility which collect info for {@link org.slf4j.MDC}
 */
@Component
public class DiagnosticInfo {

    @Autowired
    private UUIDGenerator uuidGenerator;

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
        // AMQP appender sets KEY_HOST and KEY_APPLICATION + KEY_APPLICATION_VERSION fields
//        MDC.put(KEY_HOST, VALUE_HOST);
//        MDC.put(KEY_APPLICATION, AppInfo.getApplicationName());
        if (requestUid == null) {
            requestUid = uuidGenerator.generate();
        }
        MDC.put(AmqpAppender.KEY_USER, getCurrentUser());
        MDC.put(AmqpAppender.REQUEST_UUID, requestUid);
        MDC.put(AmqpAppender.REQUEST_DESCRIPTION, description);
        return () -> {
//                MDC.remove(KEY_HOST);
//                MDC.remove(KEY_APPLICATION);
            MDC.remove(AmqpAppender.KEY_USER);
            MDC.remove(AmqpAppender.REQUEST_UUID);
            MDC.remove(AmqpAppender.REQUEST_DESCRIPTION);
        };
    }
}
