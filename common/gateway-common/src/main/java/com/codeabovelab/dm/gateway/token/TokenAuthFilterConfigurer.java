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

import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Configurer of TokenAuthFilter
 * @param <H>
 */
public class TokenAuthFilterConfigurer<H extends HttpSecurityBuilder<H>> extends
        AbstractAuthenticationFilterConfigurer<H,TokenAuthFilterConfigurer<H>,AuthenticationTokenFilter> {

    private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource;

    public TokenAuthFilterConfigurer(AuthenticationTokenFilter authenticationFilter) {
        super(authenticationFilter, null);
    }

    public TokenAuthFilterConfigurer(RequestMatcher requestMatcher, AuthenticationProvider authenticationProvider) {
        this(new AuthenticationTokenFilter(requestMatcher, authenticationProvider));
    }                                                                  //new RequestHeaderRequestMatcher(AuthenticationTokenFilter.X_AUTH_TOKEN)

    public AuthenticationDetailsSource<HttpServletRequest, ?> getAuthenticationDetailsSource() {
        return authenticationDetailsSource;
    }

    public void setAuthenticationDetailsSource(AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
        this.authenticationDetailsSource = authenticationDetailsSource;
    }

    @Override protected RequestMatcher createLoginProcessingUrlMatcher(String loginProcessingUrl) {
        return null;
    }

    @Override
    public void configure(H http) throws Exception {

        AuthenticationTokenFilter af = getAuthenticationFilter();
        if(authenticationDetailsSource != null) {
            af.setAuthenticationDetailsSource(authenticationDetailsSource);
        }
        af.setAuthenticationManager(http.getSharedObject(AuthenticationManager.class));
        af.setAuthenticationSuccessHandler(new AuthenticationStubSuccessHandler());
        SessionAuthenticationStrategy sessionAuthenticationStrategy = http.getSharedObject(SessionAuthenticationStrategy.class);
        if(sessionAuthenticationStrategy != null) {
            af.setSessionAuthenticationStrategy(sessionAuthenticationStrategy);
        }
        AuthenticationTokenFilter filter = postProcess(af);
        filter.setContinueChainAfterSuccessfulAuthentication(true);
        http.addFilterBefore(filter, BasicAuthenticationFilter.class);
    }

    @Override
    public void init(H http) throws Exception {
    }

    public static class AuthenticationStubSuccessHandler implements AuthenticationSuccessHandler {
        @Override public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                      Authentication authentication) throws IOException, ServletException {

        }
    }

}
