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

package com.codeabovelab.dm.cluman.cluster.docker.model;

import com.codeabovelab.dm.common.utils.Sugar;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.NullNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.util.*;

@JsonSerialize(using = ExposedPorts.Serializer.class)
@JsonDeserialize(using = ExposedPorts.Deserializer.class)
@EqualsAndHashCode
@ToString
@Getter
public class ExposedPorts {

    private final List<ExposedPort> exposedPorts;

    public ExposedPorts(ExposedPort... exposedPorts) {
        this.exposedPorts = Sugar.immutableList(Arrays.asList(exposedPorts));
    }

    public ExposedPorts(Collection<ExposedPort> exposedPorts) {
        this.exposedPorts = Sugar.immutableList(exposedPorts);
    }

    public static class Serializer extends JsonSerializer<ExposedPorts> {

        @Override
        public void serialize(ExposedPorts exposedPorts, JsonGenerator jsonGen, SerializerProvider serProvider)
                throws IOException {

            jsonGen.writeStartObject();
            for (ExposedPort exposedPort : exposedPorts.getExposedPorts()) {
                jsonGen.writeFieldName(exposedPort.toString());
                jsonGen.writeStartObject();
                jsonGen.writeEndObject();
            }
            jsonGen.writeEndObject();
        }

    }

    public static class Deserializer extends JsonDeserializer<ExposedPorts> {
        @Override
        public ExposedPorts deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException {

            List<ExposedPort> exposedPorts = new ArrayList<>();
            ObjectCodec oc = jsonParser.getCodec();
            JsonNode node = oc.readTree(jsonParser);
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {

                Map.Entry<String, JsonNode> field = it.next();
                if (!field.getValue().equals(NullNode.getInstance())) {
                    exposedPorts.add(ExposedPort.parse(field.getKey()));
                }
            }
            return new ExposedPorts(exposedPorts.toArray(new ExposedPort[0]));
        }
    }
}