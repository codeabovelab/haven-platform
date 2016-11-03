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

package com.codeabovelab.dm.cluman.cluster.docker.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import static com.google.common.base.Preconditions.checkNotNull;

public class Ulimit {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Soft")
    private Integer soft;

    @JsonProperty("Hard")
    private Integer hard;

    public Ulimit() {

    }

    public Ulimit(String name, int soft, int hard) {
        checkNotNull(name, "Name is null");

        this.name = name;
        this.soft = soft;
        this.hard = hard;
    }

    public String getName() {
        return name;
    }

    public Integer getSoft() {
        return soft;
    }

    public Integer getHard() {
        return hard;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Ulimit) {
            Ulimit other = (Ulimit) obj;
            return new EqualsBuilder().append(name, other.getName()).append(soft, other.getSoft())
                    .append(hard, other.getHard()).isEquals();
        } else
            return super.equals(obj);

    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).append(soft).append(hard).toHashCode();
    }

    @Override
    public String toString() {
        return "Ulimit{" +
                "name='" + name + '\'' +
                ", soft=" + soft +
                ", hard=" + hard +
                '}';
    }
}