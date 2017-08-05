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

import com.codeabovelab.dm.cluman.model.EventWithTime;
import com.codeabovelab.dm.cluman.model.NodeMetrics;
import com.codeabovelab.dm.cluman.model.WithCluster;
import lombok.Data;
import org.springframework.util.Assert;

import java.beans.ConstructorProperties;
import java.time.ZonedDateTime;

/**
 */
@Data
public class NodeHealthEvent implements EventWithTime, WithCluster {
    private final String name;
    private final String cluster;
    private final NodeMetrics health;

    @ConstructorProperties({"name", "cluster", "health"})
    public NodeHealthEvent(String name, String cluster, NodeMetrics health) {
        Assert.notNull(name, "name is null");
        this.name = name;
        this.cluster = cluster;//cluster can be null
        Assert.notNull(health, "health is null");
        this.health = health;
    }

    @Override
    public long getTimeInMilliseconds() {
        ZonedDateTime time = health.getTime();
        if(time == null) {
            return Long.MIN_VALUE;
        }
        return time.toEpochSecond();
    }
}
