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

import com.codeabovelab.dm.common.utils.Keeper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.type.TypeBindings;
import org.springframework.util.Assert;

import java.io.IOException;

/**
 */
class KeeperDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {

    private final BeanProperty property;

    public KeeperDeserializer() {
        this.property = null;
    }

    private KeeperDeserializer(BeanProperty property) {
        this.property = property;
    }

    /**
     * When we use plain deserializer we must return plain value. Setting into keeper will done in
     * external code {@link com.codeabovelab.dm.common.json.KeeperBeanDeserializerModifier.CustomGetterBeanProperty }
     * @param p
     * @param ctxt
     * @return
     * @throws IOException
     */
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Object value = getInternalValue(p, ctxt);
        return value;
    }

    private Object getInternalValue(JsonParser p, DeserializationContext ctxt) throws IOException {
        JavaType type = ctxt.getContextualType();
        if(type == null) {
            if(property == null) {
                throw new IllegalStateException("can not deserialize value with null property");
            } else {
                type = property.getType();
            }
        }
        type = resolve(type);
        return p.readValueAs(type.getRawClass());
    }

    private JavaType resolve(final JavaType type) {
        Assert.notNull(type, "type can't be null");
        JavaType tmp = type;
        while(Keeper.class.equals(tmp.getRawClass())) {
            TypeBindings bindings = tmp.getBindings();
            Assert.isTrue(bindings.size() == 1, "Bindings must have one parameter type: " + type);
            tmp = bindings.getBoundType(0);
        }
        return tmp;
    }

    /**
     * When we used intovalue, then we must return keeper
     * @param p
     * @param ctxt
     * @param intoValue
     * @return
     * @throws IOException
     * @throws JsonProcessingException
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object deserialize(JsonParser p, DeserializationContext ctxt, Object intoValue) throws IOException {
        Object value = getInternalValue(p, ctxt);
        ((Keeper)intoValue).accept(value);
        return intoValue;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        return new KeeperDeserializer(property);
    }
}
