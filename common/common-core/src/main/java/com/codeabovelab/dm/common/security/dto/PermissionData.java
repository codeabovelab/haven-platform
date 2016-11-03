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
import org.springframework.security.acls.domain.AclFormattingUtils;
import org.springframework.security.acls.model.Permission;

/**
 * Permission DTO
 * <p/>
 */
public class PermissionData implements Permission {

    public static class Builder implements Permission {
        private String pattern = THIRTY_TWO_RESERVED_OFF;
        private int mask = 0;

        @Override
        public int getMask() {
            return 0;
        }

        @Override
        public String getPattern() {
            return null;
        }


        public Builder remove(Permission permission) {
            this.mask &= ~permission.getMask();
            this.pattern = AclFormattingUtils.demergePatterns(this.pattern, permission.getPattern());

            return this;
        }

        public Builder clear() {
            this.mask = 0;
            this.pattern = THIRTY_TWO_RESERVED_OFF;

            return this;
        }

        public Builder add(Permission permission) {
            this.mask |= permission.getMask();
            this.pattern = AclFormattingUtils.mergePatterns(this.pattern, permission.getPattern());

            return this;
        }

        public final String toString() {
            return this.getClass() + "[" + getPattern() + "=" + mask + "]";
        }

        public final boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Permission)) {
                return false;
            }
            Permission permission = (Permission) obj;
            return (this.mask == permission.getMask());
        }


        public final int hashCode() {
            return this.mask;
        }

        public PermissionData build() {
            return new PermissionData(pattern, mask);
        }
    }

    private final String pattern;
    private final int mask;

    @JsonCreator
    public PermissionData(@JsonProperty("pattern") String pattern,
                          @JsonProperty("mask") int mask) {
        this.pattern = pattern;
        this.mask = mask;
    }

    public static PermissionData from(Permission permission) {
        if(permission == null  || permission instanceof PermissionData) {
            return (PermissionData) permission;
        }
        return new PermissionData(permission.getPattern(), permission.getMask());
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getPattern() {
        return this.pattern;
    }

    @Override
    public int getMask() {
        return mask;
    }

    public final String toString() {
        return this.getClass().getSimpleName() + "[" + getPattern() + "=" + mask + "]";
    }

    public final boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Permission)) {
            return false;
        }
        Permission permission = (Permission) obj;
        return (this.mask == permission.getMask());
    }


    public final int hashCode() {
        return this.mask;
    }
}
