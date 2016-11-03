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

package com.codeabovelab.dm.cluman.model;

/**
 * Container can be depended fom other containers or resource (resource specified by uri, and may be external,
 * predefined or provided by other container)
 */
public class ContainerDependency {
    /**
     * Uri of resource, like 'tcp://nginx:80', 'https://nginx:443/'  or 'mysql:mike@myserver:33060/testDB', note that system may
     * do uri check, and resolve dependency only if uri checker gave non-error code.
     */
    private String uri;
    private String container;
    /**
     * Only applied for 'container' deps, used when specified container must be started after this. <p/>
     * Default: false.
     */
    private boolean reverse;
    /**
     * Soft dependency, by default 'true'. When dependency is absent, or its check is failed - system
     * try to run container.
     */
    private boolean soft = true;
    /**
     * Maximum timeout to wait dependency. It applicable for checking uri dependencies, or started containers.
     * If container is not presented system immediate fail (or skip when <code>soft == true</code>).
     */
    private long wait = 0;
    /**
     * System must check dependency. Uri is passed to specific handler which try to connect, for containers system
     * require status and try to check at least one published ports. It slow cluster start.
     */
    private boolean check = false;
}
