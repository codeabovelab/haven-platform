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

package com.codeabovelab.dm.cluman.ds.nodes;

import com.codeabovelab.dm.cluman.model.NodeInfo;
import com.codeabovelab.dm.common.mb.Subscriptions;
import org.springframework.security.acls.model.ObjectIdentity;

/**
 * Node registration in key value store, default implementation act as proxy, therefore may be changed in another thread.
 */
public interface NodeRegistration {
    NodeInfo getNodeInfo();

    /**
     * Name of cluster, can be null.
     * @return name or null
     */
    String getCluster();

    /**
     * Time for node registration in seconds.
     * @return seconds or negative value when is not applicable.
     */
    int getTtl();

    Subscriptions<NodeHealthEvent> getHealthSubscriptions();

    ObjectIdentity getOid();
}
