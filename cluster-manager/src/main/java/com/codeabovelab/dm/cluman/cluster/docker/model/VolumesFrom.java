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
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.IOException;

@JsonSerialize(using = VolumesFrom.Serializer.class)
@JsonDeserialize(using = VolumesFrom.Deserializer.class)
public class VolumesFrom {

    private String container;

    private AccessMode accessMode;

    public VolumesFrom(String container) {
        this(container, AccessMode.DEFAULT);
    }

    public VolumesFrom(String container, AccessMode accessMode) {
        this.container = container;
        this.accessMode = accessMode;
    }

    public String getContainer() {
        return container;
    }

    public AccessMode getAccessMode() {
        return accessMode;
    }

    /**
     * Parses a volume from specification to a {@link VolumesFrom}.
     *
     * @param serialized
     *            the specification, e.g. <code>container:ro</code>
     * @return a {@link VolumesFrom} matching the specification
     * @throws IllegalArgumentException
     *             if the specification cannot be parsed
     */
    public static VolumesFrom parse(String serialized) {
        try {
            String[] parts = serialized.split(":");
            switch (parts.length) {
                case 1: {
                    return new VolumesFrom(parts[0]);
                }
                case 2: {
                    return new VolumesFrom(parts[0], AccessMode.valueOf(parts[1]));
                }

                default: {
                    throw new IllegalArgumentException();
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing Bind '" + serialized + "'");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VolumesFrom) {
            VolumesFrom other = (VolumesFrom) obj;
            return new EqualsBuilder().append(container, other.getContainer())
                    .append(accessMode, other.getAccessMode()).isEquals();
        } else
            return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(container).append(accessMode).toHashCode();
    }

    /**
     * Returns a string representation of this {@link VolumesFrom} suitable for inclusion in a JSON message. The format
     * is <code>&lt;container&gt;:&lt;access mode&gt;</code>, like the argument in {@link #parse(String)}.
     *
     * @return a string representation of this {@link VolumesFrom}
     */
    @Override
    public String toString() {
        return container + ":" + accessMode.toString();
    }

    public static class Serializer extends JsonSerializer<VolumesFrom> {

        @Override
        public void serialize(VolumesFrom volumeFrom, JsonGenerator jsonGen, SerializerProvider serProvider)
                throws IOException {

            jsonGen.writeString(volumeFrom.toString());

        }

    }

    public static class Deserializer extends JsonDeserializer<VolumesFrom> {
        @Override
        public VolumesFrom deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException {

            ObjectCodec oc = jsonParser.getCodec();
            JsonNode node = oc.readTree(jsonParser);
            return VolumesFrom.parse(node.asText());

        }
    }

}