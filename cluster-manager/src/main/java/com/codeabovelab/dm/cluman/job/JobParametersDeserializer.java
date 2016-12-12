/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.cluman.job;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.ContainerDeserializerBase;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.util.Assert;

import java.io.IOException;

/**
 */
class JobParametersDeserializer extends JsonDeserializer {

    private static JobsManager jobsManager;

    static void setJobsManager(JobsManager jobsManager) {
        // the ugly hack, because we need access to jobs types in runtime
        JobParametersDeserializer.jobsManager = jobsManager;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctx) throws IOException, JsonProcessingException {
        if(jobsManager == null) {
            throw new IllegalStateException("This deserializer need a jobsManager instance, see 'JobParametersDeserializer.setJobsManager'");
        }
        final JsonStreamContext jsc = p.getParsingContext();
        String paramName = null;
        JsonStreamContext parent = jsc;
        while(parent != null) {
            paramName = parent.getCurrentName();
            if(paramName != null) {
                break;
            }
            parent = parent.getParent();
        }
        if(parent == null) {
            throw new NullPointerException("Something wrong: we can not find our parent object, " +
              "may be you use this deserializer on custom object?");
        }
        JobParameters.Builder r = (JobParameters.Builder) parent.getParent().getCurrentValue();
        String jobType = r.getType();
        JobDescription desc = jobsManager.getDescription(jobType);
        JobParameterDescription param = desc.getParameters().get(paramName);
        TypeFactory typeFactory = ctx.getTypeFactory();
        JavaType type;
        if(param == null) {
            type = typeFactory.constructType(Object.class);
        } else {
            type = typeFactory.constructType(param.getType());
        }
        JsonDeserializer<Object> deser = ctx.findNonContextualValueDeserializer(type);
        if(deser instanceof ContainerDeserializerBase) {
            Assert.notNull(((ContainerDeserializerBase)deser).getContentDeserializer() == null,
              "No content deserializer for '" + jobType + "." + paramName + "' job parameter ");
        }
        return deser.deserialize(p, ctx);
    }
}
