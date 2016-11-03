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
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import org.springframework.util.MimeType;

/**
 * Defines platform extensions that will be registered with {@link ObjectMapper}
 */
public class DmJacksonModule extends Module {

    /**
     * Method that returns a display that can be used by Jackson
     * for informational purposes, as well as in associating extensions with
     * module that provides them.
     */
    @Override
    public String getModuleName() {
        return getClass().getName();
    }

    /**
     * Method that returns version of this module. Can be used by Jackson for
     * informational purposes.
     */
    @Override
    public Version version() {
        return new Version(1, 0, 0, null, null, null);
    }

    /**
     * Method called by {@link ObjectMapper} when module is registered.
     * It is called to let module register functionality it provides,
     * using callback methods passed-in context object exposes.
     */
    @Override
    public void setupModule(SetupContext setupContext) {
        SimpleSerializers serializers = new SimpleSerializers();
        addSerializers(serializers);
        setupContext.addSerializers(serializers);

        SimpleDeserializers deserializers = new SimpleDeserializers();
        addDeserializers(deserializers);
        setupContext.addDeserializers(deserializers);
        setupContext.addBeanDeserializerModifier(new KeeperBeanDeserializerModifier());
    }

    @SuppressWarnings("unchecked")
    private void addDeserializers(SimpleDeserializers deserializers) {
        deserializers.addDeserializer(MimeType.class, new MimeTypeDeserializer());
        deserializers.addDeserializer(Keeper.class, (JsonDeserializer) new KeeperDeserializer());
    }

    private void addSerializers(SimpleSerializers serializers) {
        serializers.addSerializer(MimeType.class, new MimeTypeSerializer());
        serializers.addSerializer(Keeper.class, new KeeperSerializer());
    }

}
