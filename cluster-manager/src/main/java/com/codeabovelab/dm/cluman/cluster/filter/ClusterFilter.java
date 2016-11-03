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

package com.codeabovelab.dm.cluman.cluster.filter;

import com.codeabovelab.dm.cluman.model.WithCluster;

/**
 * Filter which pass objects with `pattern.equals(this.cluster)`.
 * @see com.codeabovelab.dm.cluman.model.WithCluster
 */
public class ClusterFilter implements Filter {

    public static final String PROTO = "cluster";
    private final String pattern;
    private final String expr;

    public ClusterFilter(String pattern) {
        this.pattern = pattern;
        this.expr = PROTO + ":" + pattern;
    }

    @Override
    public boolean test(Object o) {
        String cluster = null;
        if(o instanceof WithCluster) {
            cluster = ((WithCluster) o).getCluster();
        }
        //here we can add some other ways to extract name of cluster
        return pattern.equals(cluster);
    }

    @Override
    public String getExpression() {
        return expr;
    }
}
