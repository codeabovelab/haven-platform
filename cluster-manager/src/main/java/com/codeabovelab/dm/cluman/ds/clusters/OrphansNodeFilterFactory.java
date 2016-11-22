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

package com.codeabovelab.dm.cluman.ds.clusters;

import com.codeabovelab.dm.cluman.cluster.filter.Filter;
import com.codeabovelab.dm.cluman.cluster.filter.FilterFactory;
import com.codeabovelab.dm.cluman.ds.nodes.NodeRegistration;
import com.codeabovelab.dm.cluman.model.DiscoveryStorage;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import com.codeabovelab.dm.cluman.security.TempAuth;
import org.springframework.util.StringUtils;

/**
 */
class OrphansNodeFilterFactory implements FilterFactory.Factory {

    private static final String PROTO = "nodes-orphan";
    public static final String FILTER = PROTO + ":";

    private final DiscoveryStorage ds;

    OrphansNodeFilterFactory(DiscoveryStorage ds) {
        this.ds = ds;
    }

    @Override
    public Filter create(String expr) {
        return (o) -> {
            String cluster = ((NodeRegistration) o).getNodeInfo().getCluster();
            if(!StringUtils.hasText(cluster)) {
                return true;
            }
            //also we want see nodes which cluster has been deleted
            try(TempAuth ta = TempAuth.asSystem()) {
                NodesGroup group = ds.getCluster(cluster);
                return group == null || !group.getFeatures().contains(NodesGroup.Feature.SWARM);
            }
        };
    }

    @Override
    public String getProtocol() {
        return PROTO;
    }
}
