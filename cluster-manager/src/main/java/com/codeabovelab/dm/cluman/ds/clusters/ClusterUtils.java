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

import com.codeabovelab.dm.cluman.model.NodeGroupState;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import org.springframework.util.StringUtils;

import java.util.Set;


/**
 */
public final class ClusterUtils {

    private ClusterUtils() {
    }


    /**
     * Real cluster must contain env name. Like 'envName:clusterName'.
     * @param name
     */
    public static void checkRealClusterName(String name) {
        checkEmpty(name);
    }

    private static void checkEmpty(String name) {
        if(!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Name is null or empty.");
        }
    }

    /**
     * Report 500 http error when cluster in not OK state.
     * @param nodesGroup cluster
     */
    public static void checkClusterState(NodesGroup nodesGroup) {
        NodeGroupState state = nodesGroup.getState();
        ExtendedAssert.error(state.isOk(), "Cluster '" + nodesGroup.getName() + "' is in no OK state: " + state.getMessage());
    }

    public static boolean isDockerBased(NodesGroup nodesGroup) {
        Set<NodesGroup.Feature> features = nodesGroup.getFeatures();
        return features.contains(NodesGroup.Feature.SWARM) ||
          features.contains(NodesGroup.Feature.SWARM_MODE);
    }
}
