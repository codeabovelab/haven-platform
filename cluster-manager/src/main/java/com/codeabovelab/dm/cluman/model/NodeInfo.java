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
 */
public interface NodeInfo extends Node, Labels, WithCluster {
    /**
     * Flag which show that node now is power up and online. It not
     * meant that node is health or not.
     * @return
     */
    boolean isOn();

    /**
     * Real cluster which own this node. <p/>
     * Note that it may be null, also, over time it may not reflect actual state. So this value
     * is actual only when this object was created.
     * @return name of real cluster or null.
     */
    @Override
    String getCluster();

    /**
     * May be null, also see {@link NodeMetrics#getTime()}, because it may be outdated.
     * @return
     */
    NodeMetrics getHealth();
}
