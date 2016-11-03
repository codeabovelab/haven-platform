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

package com.codeabovelab.dm.cluman.job;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.*;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 */
@Component
public class JobScopeSupport implements BeanFactoryPostProcessor, Scope, Ordered {

    public static final String SCOPE_NAME = "dmJobScope";

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

        beanFactory.registerScope(SCOPE_NAME, this);
    }

    @Override
    public Object resolveContextualObject(String key) {
        JobContext context = getContext();
        return new BeanWrapperImpl(context).getPropertyValue(key);
    }

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        JobContext context = getContext();
        Object scopedObject = context.getAttributes().get(name);
        if(scopedObject == null) {
            scopedObject = context.getBean(name, objectFactory);
        }
        return scopedObject;
    }

    /**
     * @see Scope#getConversationId()
     */
    @Override
    public String getConversationId() {
        return getContext().getId();
    }

    /**
     * @see Scope#registerDestructionCallback(String, Runnable)
     */
    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        getContext().registerDestructionCallback(name, callback);
    }

    /**
     * @see Scope#remove(String)
     */
    @Override
    public Object remove(String name) {
        return getContext().removeBean(name);
    }

    JobContext getContext() {
        JobContext context = JobContext.getCurrent();
        if (context == null) {
            throw new IllegalStateException("No context available for " + SCOPE_NAME);
        }
        return context;
    }

}
