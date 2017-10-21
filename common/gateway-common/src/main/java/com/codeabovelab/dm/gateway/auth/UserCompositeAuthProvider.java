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

package com.codeabovelab.dm.gateway.auth;

import com.codeabovelab.dm.common.security.*;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Authentication provider which can authenticate by {@link com.codeabovelab.dm.common.security.UserCompositeAuthenticationToken }
 */
@Component
@AllArgsConstructor
public class UserCompositeAuthProvider implements AuthenticationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserCompositeAuthProvider.class);

    private final UserIdentifiersDetailsService service;
    private final PasswordEncoder passwordEncoder;
    private final SuccessAuthProcessor authProcessor;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        UserCompositeAuthenticationToken token = convert(authentication);
        UserDetails userDetails = service.loadUserByIdentifiers(token.getPrincipal());

        String presentedPassword = authentication.getCredentials().toString();
        if (!passwordEncoder.matches(presentedPassword, userDetails.getPassword())) {
            LOGGER.debug("Authentication failed: password does not match stored value for principal " + token.getPrincipal());
            throw new BadCredentialsException("Bad credentials");
        }

        return authProcessor.createSuccessAuth(authentication, userDetails);
    }

    private UserCompositeAuthenticationToken convert(Authentication authentication) {
        UserCompositeAuthenticationToken auth;
        if(authentication instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
            try {
                auth = new UserCompositeAuthenticationToken(UserCompositePrincipal.builder().username(token.getName()).build(), token.getCredentials());
            } catch (RuntimeException e) {
                AuthenticationException ae;
                if(e instanceof AuthenticationException) {
                    ae = (AuthenticationException) e;
                } else {
                    ae = new BadCredentialsException("", e);
                }
                throw ae;
            }
        } else if(authentication instanceof UserCompositeAuthenticationToken) {
            auth = (UserCompositeAuthenticationToken)authentication;
        } else {
            throw new BadCredentialsException("Unsupported token type");
        }
        return auth;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        // we assume for using classes inherited from 'UserCompositeAuthenticationToken' tokens we also override this method
        return UserCompositeAuthenticationToken.class.equals(authentication) ||
              UsernamePasswordAuthenticationToken.class.equals(authentication);
    }
}
