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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.*;
import org.springframework.stereotype.Component;

/**
 */
@Component
public class JobScope extends AbstractJobScope {
    public static final String SCOPE_NAME = "dmJobScope";

    @Override
    ScopeBeans getContextOrNull() {
        return getBeans();
    }

    static ScopeBeans getBeans() {
        return JobContext.getCurrent().getScopeBeans();
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        beanFactory.registerScope(SCOPE_NAME, this);
    }
}
