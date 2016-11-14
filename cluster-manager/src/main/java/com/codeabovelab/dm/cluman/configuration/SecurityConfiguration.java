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

package com.codeabovelab.dm.cluman.configuration;

import com.codeabovelab.dm.cluman.security.*;
import com.codeabovelab.dm.common.security.*;
import com.codeabovelab.dm.common.security.acl.*;
import com.codeabovelab.dm.gateway.auth.ConfigurableUserDetailService;
import com.codeabovelab.dm.platform.security.Base64Encryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.acls.domain.*;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.encrypt.BouncyCastleAesGcmBytesEncryptor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 */
@Configuration
@EnableConfigurationProperties(ConfigurableUserDetailService.Config.class)
public class SecurityConfiguration {


    //TODO we need obtain pass from safe way, like file stored as 'root:root 700' access rights
    @Bean
    TextEncryptor textEncryptor(@Value("${dm.security.cipher.password}") String password,
                                @Value("${dm.security.cipher.salt}") String salt) {
        // on wrong configuration system will pass prop expressions '${prop}' as value, we need to detect this
        Assert.isTrue(StringUtils.hasText(password) && !password.startsWith("${"), "'dm.security.cipher.password' is invalid.");
        Assert.isTrue(StringUtils.hasText(salt) && !salt.startsWith("${"), "'dm.security.cipher.salt' is invalid.");
        //we use bouncycastle because standard  java does not support keys bigger 128bits
        // but spring also does not provide any way to change key size
        // see also: https://github.com/spring-projects/spring-security/issues/2917
        BytesEncryptor encryptor = new BouncyCastleAesGcmBytesEncryptor(password, salt);
        return new Base64Encryptor(encryptor);
    }

    @Bean
    UserIdentifiersDetailsService userAuthenticationManager(ConfigurableUserDetailService.Config config) {
        return new ConfigurableUserDetailService(config);
    }

    @Bean
    AclContextFactory aclContextFactory(AclService aclService, ExtPermissionGrantingStrategy pgs, SidRetrievalStrategy sidRetrievalStrategy) {
        return new StandardAclContextFactory(aclService, pgs, sidRetrievalStrategy);
    }

    @Bean
    AuditLogger createAuditLogger() {
        return new ConsoleAuditLogger();
    }

    @Bean
    RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.builder()
          .childs(Authorities.ADMIN_ROLE, /*SecuredType.CLUSTER.admin(),*/ Authorities.USER_ROLE)
          /*.childs(SecuredType.CLUSTER.admin(), Authorities.USER_ROLE)
          .childs(SecuredType.CLUSTER.admin(), SecuredType.CONTAINER.admin(),
             SecuredType.LOCAL_IMAGE.admin(),
             SecuredType.NETWORK.admin(),
             SecuredType.NODE.admin()
          )*/
          .build();
    }

    @Bean
    SidRetrievalStrategy sidRetrievalStrategy(RoleHierarchy roleHierarchy) {
        return new TenantSidRetrievalStrategy(roleHierarchy);
    }

    @Bean
    TenantsService tenantsService() {
        return new StubTenantsService();
    }

    @Bean
    ExtPermissionGrantingStrategy createPermissionGrantingStrategy(TenantsService tenantsService) {
        PermissionGrantingJudgeDefaultBehavior behavior = new PermissionGrantingJudgeDefaultBehavior(tenantsService);
        return new TenantBasedPermissionGrantedStrategy(behavior);
    }

    @EnableConfigurationProperties(PropertyAclServiceConfigurer.class)
    @Configuration
    public static class AclSeviceConfiguration {

        @Autowired(required = false)
        private List<AclServiceConfigurer> configurers;

        @Autowired(required = false)
        private Map<String, AclProvider> providers;

        @Bean
        ConfigurableAclService configurableAclService(PermissionGrantingStrategy permissionGrantingStrategy) {
            ConfigurableAclService.Builder b = ConfigurableAclService.builder();
            if(configurers != null) {
                for(AclServiceConfigurer configurer: configurers) {
                    configurer.configure(b);
                }
            }
            return b.permissionGrantingStrategy(permissionGrantingStrategy).build();
        }

        @Primary
        @Bean
        AclService providersAclService(PermissionGrantingStrategy permissionGrantingStrategy) {
            ProvidersAclService service = new ProvidersAclService(permissionGrantingStrategy);
            if(providers != null) {
                service.getProviders().putAll(providers);
            }
            return service;
        }

        @Bean
        SuccessAuthProcessor successAuthProcessor() {
            return (a, ud) -> {
                Set<GrantedAuthority> authorities = new HashSet<>(ud.getAuthorities());
                authorities.add(Authorities.USER);
                // we add GA for user, because we do not implement ACL tuning for this,
                // and anyway check cluster and node access
                authorities.add(Authorities.fromName(SecuredType.LOCAL_IMAGE.admin()));
                authorities.add(Authorities.fromName(SecuredType.REMOTE_IMAGE.admin()));
                authorities.add(Authorities.fromName(SecuredType.NETWORK.admin()));

                final UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(
                  ud, ud.getPassword(), authorities);
                result.setDetails(a.getDetails());
                return result;
            };
        }
    }

}
