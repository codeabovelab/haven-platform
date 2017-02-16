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
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@JsonSerialize(using = VolumeRefs.Serializer.class)
@JsonDeserialize(using = VolumeRefs.Deserializer.class)
@Data
public class VolumeRefs {

    private VolumeRef[] volumes;

    public VolumeRefs(VolumeRef... volumes) {
        this.volumes = volumes;
    }

    public VolumeRefs(List<VolumeRef> volumes) {
        this.volumes = volumes.toArray(new VolumeRef[volumes.size()]);
    }

    public static class Serializer extends JsonSerializer<VolumeRefs> {

        @Override
        public void serialize(VolumeRefs volumes, JsonGenerator jsonGen, SerializerProvider serProvider)
                throws IOException {

            jsonGen.writeStartObject();
            for (VolumeRef volume : volumes.getVolumes()) {
                jsonGen.writeFieldName(volume.getPath());
                jsonGen.writeStartObject();
                jsonGen.writeEndObject();
            }
            jsonGen.writeEndObject();
        }

    }

    public static class Deserializer extends JsonDeserializer<VolumeRefs> {
        @Override
        public VolumeRefs deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException {

            List<VolumeRef> volumes = new ArrayList<VolumeRef>();
            ObjectCodec oc = jsonParser.getCodec();
            JsonNode node = oc.readTree(jsonParser);
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {

                Map.Entry<String, JsonNode> field = it.next();
                if (!field.getValue().equals(NullNode.getInstance())) {
                    String path = field.getKey();
                    VolumeRef volume = new VolumeRef(path);
                    volumes.add(volume);
                }
            }
            return new VolumeRefs(volumes.toArray(new VolumeRef[0]));
        }
    }

}