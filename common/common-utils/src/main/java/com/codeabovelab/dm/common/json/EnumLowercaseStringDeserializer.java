/*
 * Copyright 2017 Code Above Lab LLC
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import org.springframework.util.Assert;

import java.io.IOException;

/**
 */
class EnumLowercaseStringDeserializer extends JsonDeserializer<Enum<?>> implements ContextualDeserializer {

    private final BeanProperty property;

    public EnumLowercaseStringDeserializer() {
        this.property = null;
    }

    private EnumLowercaseStringDeserializer(BeanProperty property) {
        this.property = property;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Enum<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String name = p.getValueAsString();
        JavaType type;
        if(property != null) {
            type = property.getType();
        } else {
            type = ctxt.getContextualType();
        }
        Assert.notNull(type, "Type of current property is null.");
        Class clazz = (Class) type.getRawClass();
        Assert.isTrue(clazz.isEnum(), "The " + clazz + " is not an enum type.");
        return Enum.valueOf(clazz, name.toUpperCase());
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        return new EnumLowercaseStringDeserializer(property);
    }
}
