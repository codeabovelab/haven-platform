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

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Job bean annotation. Create bean in {@link JobScopeSupport#SCOPE_NAME scope }
 * Note that bean must implement {@link Runnable} and can use {@link JobParam } annotation for fields and setters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
@Scope(value = JobScopeSupport.SCOPE_NAME)
public @interface JobBean {
    /**
     * Type identifier of job, default use class name.
     * @return
     */
    String value() default "";

    /**
     * Job bean support repeating, it mean that context of it bean is alive between schedule iteration.
     * It not required for scheduling, because any bean can be scheduled job.
     * @return
     */
    boolean repeatable() default false;

}
