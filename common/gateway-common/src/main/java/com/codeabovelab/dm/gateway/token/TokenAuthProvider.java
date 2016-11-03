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

package com.codeabovelab.dm.gateway.token;

import com.codeabovelab.dm.common.security.SecurityUtils;
import com.codeabovelab.dm.common.security.token.TokenData;
import com.codeabovelab.dm.common.security.token.TokenException;
import com.codeabovelab.dm.common.security.token.TokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * Implementation of credentials provider for PreAuthenticatedAuthenticationToken
 */
public class TokenAuthProvider implements AuthenticationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(TokenAuthProvider.class);

    private final TokenValidator tokenValidator;
    private final UserDetailsService userDetailsService;

    @Autowired
    public TokenAuthProvider(TokenValidator tokenValidator, UserDetailsService userDetailsService) {
        this.tokenValidator = tokenValidator;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        final TokenData tokenData = fetchToken(authentication);
        if (tokenData != null) {
            final UserDetails userDetails = userDetailsService.loadUserByUsername(tokenData.getUserName());
            LOG.debug("Token {} is valid; userDetails is {}", tokenData, userDetails);
            return SecurityUtils.createSuccessAuthentication(authentication, userDetails);
        } else {
            throw new UsernameNotFoundException("User not found" + authentication.getCredentials());
        }
    }

    protected TokenData fetchToken(Authentication authentication) {
        String principal = (String) authentication.getPrincipal();
        if (principal == null) {
            LOG.warn("principal wasn't passed");
            return null;
        }
        return tokenValidator.verifyToken(principal);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(PreAuthenticatedAuthenticationToken.class);
    }
}
