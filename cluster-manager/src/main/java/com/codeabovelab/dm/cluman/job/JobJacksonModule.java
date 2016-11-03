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

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 */
@Component
@ConditionalOnBean(JobParameterDescriptionSerializer.class)
public class JobJacksonModule extends Module {

    private JobParameterDescriptionSerializer jobParameterDescriptionSerializer;

    @Autowired(required = false)
    public void setJobParameterDescriptionSerializer(JobParameterDescriptionSerializer jobParameterDescriptionSerializer) {
        this.jobParameterDescriptionSerializer = jobParameterDescriptionSerializer;
    }

    @Override
    public String getModuleName() {
        return getClass().getName();
    }

    @Override
    public Version version() {
        return new Version(1, 0, 0, null, null, null);
    }

    @Override
    public void setupModule(SetupContext setupContext) {
        SimpleSerializers serializers = new SimpleSerializers();
        addSerializers(serializers);
        setupContext.addSerializers(serializers);

        SimpleDeserializers deserializers = new SimpleDeserializers();
        addDeserializers(deserializers);
        setupContext.addDeserializers(deserializers);
    }

    @SuppressWarnings("unchecked")
    private void addDeserializers(SimpleDeserializers deserializers) {
    }

    private void addSerializers(SimpleSerializers serializers) {
        if(jobParameterDescriptionSerializer != null) {
            serializers.addSerializer(JobParameterDescription.class, jobParameterDescriptionSerializer);
        }
    }

}
