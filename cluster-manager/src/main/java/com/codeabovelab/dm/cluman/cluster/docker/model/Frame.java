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

import java.util.Arrays;

/**
 * Represents a logging frame.
 */
public class Frame {
    private final StreamType streamType;

    private final byte[] payload;

    public Frame(StreamType streamType, byte[] payload) {
        this.streamType = streamType;
        this.payload = payload;
    }

    public StreamType getStreamType() {
        return streamType;
    }

    public byte[] getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", streamType, new String(payload).trim());
    }

    public String getMessage() {
        return new String(payload).trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Frame frame = (Frame) o;

        return streamType == frame.streamType && Arrays.equals(payload, frame.payload);

    }

    @Override
    public int hashCode() {
        int result = streamType.hashCode();
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }
}