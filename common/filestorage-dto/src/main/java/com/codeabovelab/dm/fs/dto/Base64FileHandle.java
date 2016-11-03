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

package com.codeabovelab.dm.fs.dto;

import com.codeabovelab.dm.common.utils.Throwables;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

/**
 * FileHandle which store file data as base64 encoded string.
 */
public final class Base64FileHandle implements FileHandle {

    private final String id;
    private final Map<String, String> attributes;
    private final byte[] bytes;

    /**
     * We cannot publish this ctor because it expose internal byte buffer
     * @param id
     * @param attributes
     * @param bytes
     */
    private Base64FileHandle(String id, Map<String, String> attributes, byte[] bytes) {
        this.id = id;
        this.attributes = FileStorageUtils.unmodifiableCaseInsensitiveCopy(attributes);
        this.bytes = bytes;
        Assert.hasLength(id, "is is null or empty");
    }

    /**
     * Construct handle from base64 encoded data
     * @param id
     * @param attributes
     * @param base64Data
     * @return
     */
    @JsonCreator
    public static Base64FileHandle from(@JsonProperty("id") String id,
                                        @JsonProperty("attributes") Map<String, String> attributes,
                                        @JsonProperty("base64Data") String base64Data) {
        return new Base64FileHandle(id, attributes, Base64Utils.decodeFromString(base64Data));
    }

    /**
     * Copy all from specified fileHandle. <p/>
     * If data greater than maxDataLen then throw IllegalArgumentException.
     * @param fileHandle
     * @param maxDataLength
     * @return
     */
    public static Base64FileHandle from(FileHandle fileHandle, int maxDataLength) {
        final byte[] arr;
        try(InputStream data = fileHandle.getData()) {
            arr = toByteArray(data, maxDataLength);
        } catch (IOException e) {
            throw Throwables.asRuntime(e);
        }

        return new Base64FileHandle(fileHandle.getId(), fileHandle.getAttributes(), arr);
    }

    private static byte[] toByteArray(InputStream input, int maxSize) throws IOException {
        if(input == null) {
            return null;
        }

        if (maxSize <= 0) {
            throw new IllegalArgumentException("Bad maxSize: " + maxSize);
        }

        byte[] data = new byte[1024];
        int offset = 0;

        while (offset < maxSize) {
            if(data.length <= offset) {
                byte[] tmp = new byte[(int) (data.length * 1.5)];
                System.arraycopy(data, 0, tmp, 0, data.length);
                data = tmp;
            }
            final int readed = input.read(data, offset, data.length - offset);
            if(readed == -1) {
                break;
            }
            offset += readed;
        }

        if (offset >= maxSize) {
            throw new IOException("InputStream is too long. Current: " + offset + ", maxSize: " + maxSize);
        }
        if(data.length == offset) {
            return data;
        }
        // we can return offset too, in this case we do not need copy large arrays
        return Arrays.copyOf(data, offset);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    @JsonIgnore
    public InputStream getData() {
        if(bytes == null) {
            return null;
        } else {
            return new ByteArrayInputStream(bytes);
        }
    }

    public String getBase64Data() {
        return Base64Utils.encodeToString(this.bytes);
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }
}
