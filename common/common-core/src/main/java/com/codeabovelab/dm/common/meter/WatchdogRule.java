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

package com.codeabovelab.dm.common.meter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * annotation which defined limits intersection of which cause is alarm
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
public @interface WatchdogRule {
    /**
     * spring el expression witch use current meters (it can be used on proxied method or injected meter) as root objects.
     * If is annotated on method, and method annotated with many meters (for example timer and histogram) then for
     * reference to specified meter we can use short class name of meter.
     * @return
     */
    String expression();

    /**
     * period for checking of rule
     * @return
     */
    long period() default 1L;

    /**
     * type of period value
     * @return
     */
    TimeUnit unit() default TimeUnit.MINUTES;

    /**
     * Name of metric in metric registry. <p/>
     * It value can be empty if annotation placed on method or field with appropriate annotation from metric framework.
     * @return
     */
    String metric() default "";

    /**
     * false (default) mean than 'metric()' value will be prepended with class name.
     * For details see
     * @return
     */
    boolean absolute() default false;
}
