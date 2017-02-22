/*
 * Copyright 2017 Code Above Lab LLC
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
import com.codeabovelab.dm.cluman.model.NodeInfoImpl;
import com.codeabovelab.dm.cluman.model.NodeMetrics;
import com.codeabovelab.dm.common.kv.mapping.KvMapAdapter;

/**
 */
class NodesKvMapAdapterImpl implements KvMapAdapter<NodeRegistrationImpl> {
    private NodeStorage nodeStorage;

    public NodesKvMapAdapterImpl(NodeStorage nodeStorage) {
        this.nodeStorage = nodeStorage;
    }

    @Override
    public Object get(String key, NodeRegistrationImpl source) {
        return NodeInfoImpl.builder(source.getNodeInfo());
    }

    @Override
    public NodeRegistrationImpl set(String key, NodeRegistrationImpl source, Object value) {
        NodeInfo ni = (NodeInfo) value;
        if (source == null) {
            NodeInfoImpl.Builder nib = NodeInfoImpl.builder(ni);
            nib.setName(key);
            source = nodeStorage.newRegistration(nib);
        } else {
            source.updateNodeInfo(b -> {
                NodeMetrics om = b.getHealth();
                b.from(ni);
                //name may be reset in 'from'
                b.name(key);
                b.health(om);
            });
        }
        return source;
    }

    @Override
    public Class<?> getType(NodeRegistrationImpl source) {
        return NodeInfoImpl.Builder.class;
    }
}
