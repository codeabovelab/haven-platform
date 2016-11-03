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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.NullNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@JsonSerialize(using = Binds.Serializer.class)
@JsonDeserialize(using = Binds.Deserializer.class)
public class Binds {

    private List<Bind> binds;

    public Binds(Bind... binds) {
        this.binds = Arrays.asList(binds);
    }

    public Binds(List<Bind> binds) {
        this.binds = binds;
    }

    public List<Bind> getBinds() {
        return binds;
    }

    public static class Serializer extends JsonSerializer<Binds> {

        @Override
        public void serialize(Binds binds, JsonGenerator jsonGen, SerializerProvider serProvider) throws IOException {

            //
            jsonGen.writeStartArray();
            for (Bind bind : binds.getBinds()) {
                jsonGen.writeString(bind.toString());
            }
            jsonGen.writeEndArray();
            //
        }

    }

    public static class Deserializer extends JsonDeserializer<Binds> {
        @Override
        public Binds deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException {

            List<Bind> binds = new ArrayList<Bind>();
            ObjectCodec oc = jsonParser.getCodec();
            JsonNode node = oc.readTree(jsonParser);
            for (Iterator<JsonNode> it = node.elements(); it.hasNext();) {

                JsonNode field = it.next();
                if (!field.equals(NullNode.getInstance())) {
                    binds.add(Bind.parse(field.textValue()));
                }
            }
            return new Binds(binds.toArray(new Bind[0]));
        }
    }

    @Override
    public String toString() {
        return "Binds{" +
                "binds=" + binds +
                '}';
    }
}