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

package com.codeabovelab.dm.cluman.ui;

import com.codeabovelab.dm.cluman.ui.model.UITokenData;
import com.codeabovelab.dm.cluman.ui.model.UiUserCredentials;
import com.codeabovelab.dm.common.security.token.*;
import com.codeabovelab.dm.gateway.token.AuthenticationTokenFilter;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/ui/token/", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TokenApi {

    private final TokenService tokenService;
    private final AuthenticationProvider authenticationProvider;
    private final TokenValidatorSettings tokenValidatorSettings;

    @ApiOperation("Use header name: " + AuthenticationTokenFilter.X_AUTH_TOKEN)
    @RequestMapping(value = "login", method = RequestMethod.POST)
    public UITokenData getToken(@RequestBody UiUserCredentials credentials) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(credentials.getUsername(), credentials.getPassword());
        final Authentication authenticate = authenticationProvider.authenticate(authentication);
        if (authenticate != null && authenticate.isAuthenticated()) {
            return createToken(credentials.getUsername());
        } else {
            throw new BadCredentialsException("Invalid login and password");
        }
    }

    @RequestMapping(value = "refresh", method = RequestMethod.PUT)
    public UITokenData refresh() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName(); //get logged in username
        return createToken(name);
    }

    @ApiOperation("Token should be passed it via " + AuthenticationTokenFilter.X_AUTH_TOKEN)
    @RequestMapping(value = "info", method = RequestMethod.GET)
    public UITokenData info(@RequestHeader(value= AuthenticationTokenFilter.X_AUTH_TOKEN) String token) {
        Assert.notNull(token, "token is null pass it via " + AuthenticationTokenFilter.X_AUTH_TOKEN);
        TokenData tokendata = tokenService.getToken(token);

        return fillFields(tokendata);
    }

    private UITokenData createToken(String name) {
        TokenConfiguration tokenConfiguration = new TokenConfiguration();
        tokenConfiguration.setUserName(name);
        TokenData token = tokenService.createToken(tokenConfiguration);

        return fillFields(token);
    }

    private UITokenData fillFields(TokenData token) {
        Instant instant = Instant.ofEpochMilli(token.getCreationTime());
        return UITokenData.builder()
                .creationTime(LocalDateTime.ofInstant(instant, ZoneOffset.UTC))
                .expireAtTime(LocalDateTime.ofInstant(instant.plusSeconds(tokenValidatorSettings.getExpireAfterInSec()), ZoneOffset.UTC))
                .key(token.getKey())
                .userName(token.getUserName())
                .build();
    }

}