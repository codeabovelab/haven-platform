/*
 * Copyright 2017 Code Above Lab LLC
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

package com.codeabovelab.dm.agent.security;

import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import io.undertow.servlet.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.undertow.UndertowDeploymentInfoCustomizer;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 */
@Configuration
public class AuthConfiguration {

    @Value("${dm.auth.adminPassword}")
    private String encodedPass;

    @Bean
    EmbeddedServletContainerCustomizer enableAuthUndertowContainerCustomizer() {
        return container -> {
            if(!(container instanceof UndertowEmbeddedServletContainerFactory)) {
                return;
            }
            UndertowEmbeddedServletContainerFactory factory = (UndertowEmbeddedServletContainerFactory) container;
            factory.addDeploymentInfoCustomizers(enableAuthUDICustomizer());
        };
    }

    private UndertowDeploymentInfoCustomizer enableAuthUDICustomizer() {
        return (DeploymentInfo di) -> {
            if(StringUtils.isEmpty(encodedPass)) {
                return;
            }
            SecurityConstraint sc = new SecurityConstraint();
            sc.setEmptyRoleSemantic(SecurityInfo.EmptyRoleSemantic.AUTHENTICATE);
            // empty web resource interpret as default
            sc.addWebResourceCollection(new WebResourceCollection());
            di.addSecurityConstraints(sc);
            di.setSecurityDisabled(false);
            di.setAuthenticationMode(AuthenticationMode.PRO_ACTIVE);
            di.setLoginConfig(new LoginConfig(HttpServletRequest.BASIC_AUTH, "Haven Agent"));
            di.setIdentityManager(new IdentityManagerImpl(encodedPass));
        };
    }

    private static class IdentityManagerImpl implements IdentityManager {

        private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        private final String encodedPass;

        IdentityManagerImpl(String encodedPass) {
            this.encodedPass = encodedPass;
        }

        @Override
        public Account verify(Account account) {
            return account;
        }

        @Override
        public Account verify(String id, Credential credential) {
            if(!(credential instanceof PasswordCredential)) {
                return null;
            }
            PasswordCredential pc = (PasswordCredential) credential;
            char[] pwdArr = pc.getPassword();
            if(pwdArr != null && passwordEncoder.matches(new String(pwdArr), encodedPass)) {
                return new AccountImpl(id);
            }
            return null;
        }

        @Override
        public Account verify(Credential credential) {
            return null;
        }

    }


    private static class AccountImpl implements Account {
        private final UserPrincipal principal;

        AccountImpl(String name) {
            this.principal = new UserPrincipal(name);
        }

        @Override
        public Principal getPrincipal() {
            return principal;
        }

        @Override
        public Set<String> getRoles() {
            return Collections.emptySet();
        }
    }

    private static class UserPrincipal implements Principal {

        private final String name;

        private UserPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof UserPrincipal)) {
                return false;
            }
            UserPrincipal that = (UserPrincipal) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
