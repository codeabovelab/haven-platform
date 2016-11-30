/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.cluman.job;

import com.codeabovelab.dm.common.utils.SafeCloseable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * Scope which is actual for each iteration of job. For simple jobs it equal with {@link JobScope }, differences is
 * appeared only on scheduled jobs with repeatable context.
 */
@Component
public class JobScopeIteration extends AbstractJobScope {
    public static final String SCOPE_NAME = "dmJobScopeIteration";
    private static final ThreadLocal<ScopeBeans> TL = new ThreadLocal<>();

    @Override
    ScopeBeans getContextOrNull() {
        return TL.get();
    }

    static ScopeBeans getBeans() {
        return TL.get();
    }

    public static SafeCloseable open(JobContext jobContext) {
        final ScopeBeans old = TL.get();
        if(old != null) {
            return () -> {};
        }
        ScopeBeans beans = new ScopeBeans(jobContext, jobContext.getId() + "-iter#" + jobContext.getIteration());
        TL.set(beans);
        return () -> {
            TL.remove();
        };
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        beanFactory.registerScope(SCOPE_NAME, this);
    }

}
