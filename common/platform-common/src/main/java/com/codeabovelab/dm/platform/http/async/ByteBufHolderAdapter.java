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

package com.codeabovelab.dm.platform.http.async;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;

import java.io.IOException;

/**
 */
class ByteBufHolderAdapter implements ChunkedInputStream.Adapter<ByteBufHolder> {

    static final ByteBufHolderAdapter INSTANCE = new ByteBufHolderAdapter();

    private ByteBufHolderAdapter() {
    }

    @Override
    public void onAdd(ByteBufHolder chunk) {
        chunk.retain();
    }

    @Override
    public void onRemove(ByteBufHolder chunk) {
        chunk.release();
    }

    @Override
    public int readByte(ByteBufHolder chunk) throws IOException {
        ByteBuf buf = chunk.content();
        if (buf.readableBytes() == 0) {
            return ChunkedInputStream.EOF;
        }
        return buf.readByte();
    }

    @Override
    public int readBytes(ByteBufHolder chunk, byte[] arr, int off, int len) throws IOException {
        ByteBuf buf = chunk.content();
        int avail = buf.readableBytes();
        if (avail == 0) {
            return ChunkedInputStream.EOF;
        }
        int readed = Math.min(len, avail);
        buf.readBytes(arr, off, readed);
        return readed;
    }
}
