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

import com.codeabovelab.dm.common.security.token.TokenException;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * implementation of servlet.Filter for fetching token information
 */
public class AuthenticationTokenFilter extends AbstractAuthenticationProcessingFilter {

    public final static String X_AUTH_TOKEN = "X-Auth-Token";
    public final static String TOKEN = "token";

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationTokenFilter.class);

    private boolean continueChainAfterSuccessfulAuthentication;

    private final AuthenticationProvider authenticationProvider;

    public AuthenticationTokenFilter(RequestMatcher requiresAuthenticationRequestMatcher,
                                     AuthenticationProvider authenticationProvider) {
        super(requiresAuthenticationRequestMatcher);
        this.authenticationProvider = authenticationProvider;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws AuthenticationException, IOException, ServletException {
        String token = httpRequest.getHeader(X_AUTH_TOKEN);
        if(token == null) {
            token = httpRequest.getParameter(TOKEN);
        }
        LOG.debug("Trying to authenticate user by auth token method. Token: {}", token);
        try {
            return processTokenAuthentication(token, getDetails(httpRequest));
        } catch (TokenException e) {
            throw new BadCredentialsException(e.getMessage(), e.getCause());
        } catch (Exception e) {
            throw new UsernameNotFoundException(e.getMessage(), e.getCause());
        }
    }

    private Authentication processTokenAuthentication(String token, Object details) {
        final PreAuthenticatedAuthenticationToken authenticationToken = new PreAuthenticatedAuthenticationToken(token, null);
        authenticationToken.setDetails(details);
        return authenticationProvider.authenticate(authenticationToken);
    }

    protected Object getDetails(HttpServletRequest request) {
        return authenticationDetailsSource.buildDetails(request);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult) throws IOException, ServletException {
        super.successfulAuthentication(request, response, chain, authResult);
        if (continueChainAfterSuccessfulAuthentication) {
            chain.doFilter(request, response);
        }
    }

    public void setContinueChainAfterSuccessfulAuthentication(boolean continueChainAfterSuccessfulAuthentication) {
        this.continueChainAfterSuccessfulAuthentication = continueChainAfterSuccessfulAuthentication;
    }
}
