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

package com.codeabovelab.dm.cluman.source;

import lombok.Data;
import org.springframework.util.Assert;

/**
 */
@Data
public class DeployOptions {

    @Data
    public static class Builder {

        private DeploySourceJob.ConflictResolution containersConflict = DeploySourceJob.ConflictResolution.LEAVE;

        public Builder containersConflict(DeploySourceJob.ConflictResolution containersConflict) {
            setContainersConflict(containersConflict);
            return this;
        }

        public DeployOptions build() {
            return new DeployOptions(this);
        }
    }

    public static final DeployOptions DEFAULT = builder().build();
    private final DeploySourceJob.ConflictResolution containersConflict;

    public DeployOptions(Builder b) {
        this.containersConflict = b.getContainersConflict();
    }

    public static Builder builder() {
        return new Builder();
    }

    public void validate() {
        Assert.notNull(containersConflict, "containersConflict is null");
    }
}
