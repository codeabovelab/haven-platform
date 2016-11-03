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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.ObjectIdentity;

import java.io.Serializable;

/**
 * an implementation based on {@link ObjectIdentityImpl} and add some metadata for properly serialization <p/>
 * note that original implementation use simple 'equals' and 'hashcode', which does not allow correct
 * comparsion with other implementations of ObjectIdentity interface.
 */
public final class ObjectIdentityData extends ObjectIdentityImpl {

    @JsonCreator
    public ObjectIdentityData(@JsonProperty("type") String type,
                              @JsonProperty("identifier") Serializable identifier) {
        super(type, identifier);
    }

    @Override
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public Serializable getIdentifier() {
        return super.getIdentifier();
    }

    public static ObjectIdentityData from(ObjectIdentity objectIdentity) {
        if(objectIdentity == null || objectIdentity instanceof ObjectIdentityData) {
            return (ObjectIdentityData) objectIdentity;
        }
        return new ObjectIdentityData(objectIdentity.getType(), objectIdentity.getIdentifier());
    }
}
