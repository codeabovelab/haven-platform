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

package com.codeabovelab.dm.common.gateway;

import java.util.Set;

/**
 * Connection info.
 */
public interface ConnectionInfo {

    /**
     * Set of the most basic types supported by underlying connection protocol.
     * @see com.codeabovelab.dm.common.gateway.TextMessage
     * @return
     */
    Set<Class<?>> getSupportedMessages();
}
