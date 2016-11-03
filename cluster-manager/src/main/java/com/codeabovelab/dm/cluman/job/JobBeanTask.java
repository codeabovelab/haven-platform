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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.Assert;

import java.util.concurrent.ConcurrentMap;

/**
 * Bean wrapper which create and run specified bean.
 */
class JobBeanTask implements Runnable {

    private final BeanFactory beanFactory;
    private final String beanName;

    public JobBeanTask(BeanFactory beanFactory, String beanName) {
        this.beanFactory = beanFactory;
        this.beanName = beanName;
    }

    @Override
    public void run() {
        Runnable bean = beanFactory.getBean(beanName, Runnable.class);
        bean.run();
        copyOutParameters(bean);
    }

    private void copyOutParameters(Object bean) {
        JobBeanIntrospector.Metadata metadata = JobBeanIntrospector.getMetadata(bean.getClass());
        Assert.notNull(metadata, "Job bean without metadata. How?");
        JobContext ctx = JobContext.getCurrent();
        ConcurrentMap<String, Object> result = ctx.getResult();
        for(JobBeanIntrospector.PropertyMetadata prop: metadata.getProps().values()) {
            if(!prop.isOut()) {
                continue;
            }
            String name = prop.getName();
            Object value = prop.getProperty().get(bean);
            result.put(name, value);
        }
    }
}
