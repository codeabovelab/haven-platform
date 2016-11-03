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

package com.codeabovelab.dm.security.acl;

import com.codeabovelab.dm.common.security.Authorities;
import com.codeabovelab.dm.common.security.UserIdentifiersDetailsService;
import com.codeabovelab.dm.common.security.acl.PermissionGrantingJudgeDefaultBehavior;
import com.codeabovelab.dm.common.security.acl.TenantBasedPermissionGrantedStrategy;
import com.codeabovelab.dm.common.security.acl.TenantsService;
import com.codeabovelab.dm.security.user.UserPropertiesService;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.*;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.*;

/**
 * spring configuration for acl
 */
@Configuration
@ComponentScan(lazyInit = true, basePackageClasses = {AclConfiguration.class, UserPropertiesService.class})
public class AclConfiguration {

    @Bean
    AclCache createAclCache(PermissionGrantingStrategy permissionGrantingStrategy, 
            AclAuthorizationStrategy aclAuthorizationStrategy) {
        final ConcurrentMapCache aclCache = new ConcurrentMapCache("aclCache");
        return new SpringCacheBasedAclCache(aclCache, permissionGrantingStrategy, aclAuthorizationStrategy);
    }

    @Bean
    AclAuthorizationStrategy createAclAuthorizationStrategy(SidRetrievalStrategy sidRetrievalStrategy) {
        final CustomAclAuthorizationStrategyImpl authStrategy = new CustomAclAuthorizationStrategyImpl(Authorities.ADMIN);
        authStrategy.setSidRetrievalStrategy(sidRetrievalStrategy);
        return authStrategy;
    }
   
    @Bean
    LookupStrategy createLookupStrategy(ObjectIdentityService objectIdentityService, 
            AclCache aclCache, 
            AclAuthorizationStrategy aclAuthorizationStrategy, 
            PermissionGrantingStrategy permissionGrantingStrategy,
            SidsService sidsService) {
        return new JpaLookupStrategy(objectIdentityService, 
                aclAuthorizationStrategy,
                permissionGrantingStrategy,
                sidsService);
    }
    
    @Bean
    MutableAclService mutableAclService() {
        return new MutableJpaAclService();
    }

    @Bean
    AclService aclService(MutableAclService mutableAclService) {
        return mutableAclService;
    }
    
    @Bean
    PermissionEvaluator createPermissionEvaluator(AclService aclService, SidRetrievalStrategy sidRetrievalStrategy) {
        final CompositePermissionEvaluator.Builder cpeb = CompositePermissionEvaluator.builder();
        final AclPermissionEvaluator aclEvaluator = new AclPermissionEvaluator(aclService);
        aclEvaluator.setSidRetrievalStrategy(sidRetrievalStrategy);
        cpeb.defaulEvaluator(aclEvaluator);
        return cpeb.build();
    }

    @Bean
    ObjectIdentityRetrievalStrategy createIdentityRetrievalStrategy() {
        return new ObjectIdentityRetrievalStrategyImpl();
    }

    @Bean
    UserIdentifiersDetailsService createUserDetailsService() {
        return new UserDetailsServiceImpl();
    }
}
