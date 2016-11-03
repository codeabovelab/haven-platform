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

package com.codeabovelab.dm.users.commands;

import com.codeabovelab.dm.common.security.Authorities;
import org.springframework.security.access.annotation.Secured;

import java.lang.annotation.*;

/**
 * Annotations is equals to @Secured(Authorities.ADMIN_ROLE)
 * it's required Admin role from user which want to execute command which has this annotaion
 * Important it's related to not only executing command in gateway
 * but for inter-module communication too
 *
 */
@Target({ ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Secured(Authorities.ADMIN_ROLE)
public @interface Admin {
}