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

package com.codeabovelab.dm.cluman.job;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import springfox.documentation.schema.DefaultGenericTypeNamingStrategy;
import springfox.documentation.schema.TypeNameExtractor;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.contexts.ModelContext;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 */
@Component
@ConditionalOnBean(TypeNameExtractor.class)
@AllArgsConstructor(onConstructor = @__(@Autowired))
class JobParameterDescriptionSerializer extends JsonSerializer<JobParameterDescription> {

    private final TypeNameExtractor typeNameExtractor;

    @Override
    public void serialize(JobParameterDescription value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("name", value.getName());
        gen.writeBooleanField("required", value.isRequired());
        gen.writeStringField("type", toString(value.getType()));
        gen.writeEndObject();
    }

    private String toString(Type type) {
        ModelContext modelContext = ModelContext.returnValue("com.codeabovelab.dm", type, DocumentationType.SPRING_WEB, null,
                new DefaultGenericTypeNamingStrategy(), null);
        return typeNameExtractor.typeName(modelContext);
    }
}
