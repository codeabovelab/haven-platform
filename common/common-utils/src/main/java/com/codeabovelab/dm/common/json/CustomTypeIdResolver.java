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

package com.codeabovelab.dm.common.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;

/**
 */
public class CustomTypeIdResolver extends TypeIdResolverBase {

    public CustomTypeIdResolver() {
        super(TypeFactory.unknownType(), null);
    }

    @Override
    public String idFromValue(Object value) {
        return idFromValueAndType(value, null);
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        Class<?> type = suggestedType;
        if(value != null && (suggestedType == null || Object.class.equals(type))) {
            type = value.getClass();
        }
        return type != null? type.getName() : null;
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {
        try {
            Class<?> type = Class.forName(id);
            return context.constructType(type);
        } catch (ClassNotFoundException e) {
            if(!(context instanceof DeserializationContext)) {
                throw new RuntimeException(e);
            }
            //see magic from ClassNameIdResolver._typeFromId()
            return ((DeserializationContext) context).handleUnknownTypeId(_baseType, id, this,
              "Class '" + id + "' not found.");
        }
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CLASS;
    }
}
