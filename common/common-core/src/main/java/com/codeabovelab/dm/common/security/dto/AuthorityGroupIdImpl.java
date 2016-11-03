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

package com.codeabovelab.dm.common.security.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 */
public class AuthorityGroupIdImpl implements AuthorityGroupId {
    @NotNull
    private final String name;
    private final String tenant;

    /**
     *
     * @param name
     * @param tenant allow null
     */
    @JsonCreator
    public AuthorityGroupIdImpl(@JsonProperty("name") String name, @JsonProperty("tenant") String tenant) {
        this.name = name;
        this.tenant = tenant;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTenant() {
        return tenant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorityGroupIdImpl that = (AuthorityGroupIdImpl) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(tenant, that.tenant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, tenant);
    }

    @Override
    public String toString() {
        return "AuthorityGroupIdImpl{" +
                "name='" + name + '\'' +
                ", tenantId=" + tenant +
                '}';
    }
}
