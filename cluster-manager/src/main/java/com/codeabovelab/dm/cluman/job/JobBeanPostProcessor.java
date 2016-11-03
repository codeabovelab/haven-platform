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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import java.util.Map;

/**
 */
@Component
class JobBeanPostProcessor implements BeanPostProcessor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        JobBeanIntrospector.Metadata metadata = JobBeanIntrospector.getMetadata(bean.getClass());
        if(metadata == null) {
            return bean;
        }
        JobContext ctx = JobContext.getCurrent();
        Assert.notNull(ctx, "No JobContext for '" + beanName + "' initialization.");
        Map<String, Object> params = ctx.getAttributes();
        for(JobBeanIntrospector.PropertyMetadata prop : metadata.getProps().values()) {
            if(!prop.isIn()) {
                continue;
            }
            String name = prop.getName();
            if(!params.containsKey(name)) {
                if(prop.isRequired()) {
                    throw new RuntimeException("Property " + name + " in " + beanName + " is required but not present in " + params);
                }
                continue;
            }
            Object value = params.get(name);
            prop.getProperty().set(bean, value);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
