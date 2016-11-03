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

package com.codeabovelab.dm.platform.configuration;

import com.codeabovelab.dm.common.security.AdminRoleVoter;
import com.codeabovelab.dm.platform.security.CachedPasswordEncoder;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.concurrent.TimeUnit;

@Configuration
public class SecurityApplicationConfiguration {

    @Value("${dm.bcrypt.strength:8}")
    private Integer bcryptStrength;

    @Value("${dm.passwordEncoder.expireAfterAccess:60}")
    private long expireAfterAccess;

    @Value("${dm.passwordEncoder.expireAfterWrite:-1}")
    private long expireAfterWrite;

    @Bean(name = "annotationValidator")
    public LocalValidatorFactoryBean getLocalValidatorFactoryBean() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public PasswordEncoder getPasswordEncoder() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(bcryptStrength);
        if(expireAfterAccess >= 0 || expireAfterWrite >= 0) {
            CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
            if(expireAfterAccess >= 0) {
                builder.expireAfterAccess(expireAfterAccess, TimeUnit.SECONDS);
            }
            if(expireAfterWrite >= 0) {
                builder.expireAfterAccess(expireAfterWrite, TimeUnit.SECONDS);
            }
            passwordEncoder = new CachedPasswordEncoder(passwordEncoder, builder
            );
        }
        return passwordEncoder;
    }

    @Bean
    public RoleVoter getRoleVoter() {
        return new AdminRoleVoter();
    }

}
