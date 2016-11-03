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

import com.codeabovelab.dm.security.jpa.FilterFactory;
import com.codeabovelab.dm.security.jpa.FilteredJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PostFilter;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * filter factory for ACL <p/>
 * TODO create specific filter which do basic filtration by ACL
 */
public class SecurityAclFilterFactory implements FilterFactory {
    
    private static final Logger LOG = LoggerFactory.getLogger(SecurityAclFilterFactory.class);
    
    @Override
    public <T, ID extends Serializable> Specification<T> create(FilteredJpaRepository<T, ID> repository) {
        try {
            final Method method = ExposeInvocationInterceptor.currentInvocation().getMethod();
            final PostFilter annotation = method.getAnnotation(PostFilter.class);
            if(annotation != null) {
                //System.out.println(" TODO post filter annotation:" + annotation.value());
            }
        } catch(IllegalStateException e) {
            LOG.warn("", e);
        }
        return null;
    }
    
}
