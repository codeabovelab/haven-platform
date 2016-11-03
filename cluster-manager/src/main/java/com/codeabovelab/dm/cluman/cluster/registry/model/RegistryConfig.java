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

package com.codeabovelab.dm.cluman.cluster.registry.model;


import com.codeabovelab.dm.cluman.cluster.registry.RegistryType;
import com.codeabovelab.dm.cluman.cluster.registry.aws.AwsRegistryConfig;
import com.codeabovelab.dm.common.kv.mapping.KvMapping;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.MoreObjects;
import lombok.Data;

/**
 * Configuration for registry service
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.EXISTING_PROPERTY,
  property = "registryType",
  defaultImpl = PrivateRegistryConfig.class,
  visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsRegistryConfig.class, name = "AWS"),
  @JsonSubTypes.Type(value = PrivateRegistryConfig.class, name = "PRIVATE"),
  @JsonSubTypes.Type(value = HubRegistryConfig.class, name = "DOCKER_HUB"),
})
@Data
public abstract class RegistryConfig implements Cloneable {

    /**
     * Unique identifier for registry. It must not be changed after adding registry.
     * <b>Also, it used as part of image name.<b/>
     */
    @KvMapping
    private String name;
    @KvMapping
    private boolean disabled;
    @KvMapping
    private boolean readOnly;
    private String errorMessage;
    @KvMapping
    private RegistryType registryType;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
          // we do not show password in to string because it can appear in logs
                .add("registryType", registryType)
                .add("disabled", disabled)
                .add("errorMessage", errorMessage)
                .add("name", name)
                .omitNullValues()
                .toString();
    }

    public RegistryConfig clone() {
        try {
            return (RegistryConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void cleanCredentials();
}
