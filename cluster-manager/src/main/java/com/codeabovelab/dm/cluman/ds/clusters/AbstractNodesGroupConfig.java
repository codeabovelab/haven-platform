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

import com.codeabovelab.dm.common.kv.mapping.KvMapping;
import com.codeabovelab.dm.common.security.acl.AclSource;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
  property = "groupType"
)
@JsonSubTypes({
  @JsonSubTypes.Type(value = DefaultNodesGroupConfig.class, name = "DEFAULT"),
  @JsonSubTypes.Type(value = SwarmNodesGroupConfig.class, name = "SWARM")
})
@Data
public abstract class AbstractNodesGroupConfig<T extends AbstractNodesGroupConfig<T>> implements Cloneable, NodesGroupConfig {

    @KvMapping
    private String name;
    @KvMapping
    private String imageFilter;
    @KvMapping
    private String title;
    @KvMapping
    private String description;
    @KvMapping
    private AclSource acl;

    @Override
    @SuppressWarnings("unchecked")
    public T clone() {
        try {
            return (T) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
