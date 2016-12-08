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

package com.codeabovelab.dm.cluman.security;

import com.codeabovelab.dm.common.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

/**
 * Utility for temporary credentials. Used in cases when non privileged code must run parts with high privileges.
 */
public final class TempAuth implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(TempAuth.class);
    private final Authentication newAuth;
    private final SecurityContext context;
    private final SecurityContext oldContext;
    private AccessContextHolder aclHolder;

    TempAuth(Authentication newAuth) {
        Assert.notNull(newAuth, "Authentication is null");
        this.newAuth = newAuth;
        this.oldContext = SecurityContextHolder.getContext();
        this.context = SecurityContextHolder.createEmptyContext();
    }

    /**
     * Do <code>open(SecurityUtils.AUTH_SYSTEM)</code>
     * @return
     */
    public static TempAuth asSystem() {
        return open(SecurityUtils.AUTH_SYSTEM);
    }

    public static TempAuth open(Authentication authentication) {
        TempAuth tempAuth = new TempAuth(authentication);
        tempAuth.init();
        return tempAuth;
    }

    private void init() {
        SecurityContextHolder.setContext(context);
        context.setAuthentication(newAuth);
        AccessContextFactory acf = AccessContextFactory.getInstanceOrNull();
        if(acf != null) {
            // we must open new context only when has factory
            this.aclHolder = acf.open();
        }
    }

    @Override
    public void close() {
        SecurityContext currContext = SecurityContextHolder.getContext();
        if(currContext != context) {
            LOG.warn("Current context \"{}\" not equal with expected: \"{}\"", currContext, context);
        }
        SecurityContextHolder.setContext(oldContext);
        if(aclHolder != null) {
            aclHolder.close();
        }
    }
}
