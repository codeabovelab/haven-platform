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

import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Input stream which wrap queue of chunks. You can add chunks in any time through {@link #add(Object)},
 * when no more chunks expected you may invoke {@link #end()} which mark that end of stream, but not close it.
 */
public class ChunkedInputStream<T> extends InputStream {

    public interface Adapter<T> {
        void onAdd(T chunk);
        void onRemove(T chunk);

        /**
         *
         * @param chunk
         * @return the next byte of data, or <code>-1</code> if the end of the stream is reached.
         * @throws IOException
         */
        int readByte(T chunk);

        /**
         *
         * @param chunk
         * @param      arr     the buffer into which the data is read.
         * @param      off   the start offset in array <code>b</code>
         *                   at which the data is written.
         * @param      len   the maximum number of bytes to read.
         * @return the total number of bytes read into the buffer, or
         *             <code>-1</code> if there is no more data because the end of
         *             the stream has been reached.
         * @throws IOException
         */
        int readBytes(T chunk, byte[] arr, int off, int len);
    }

    public static final int EOF = -1;
    private static final Object END = new Object();
    private final Adapter<T> adapter;
    private final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
    private volatile boolean closed;
    private volatile boolean end;
    private final AtomicReference<T> currentRef = new AtomicReference<>();
    /**
     * we can use lock only for 'read' methods, other uses may produce deadlocks
     */
    private final Lock lock = new ReentrantLock();


    public ChunkedInputStream(Adapter<T> adapter) {
        Assert.notNull(adapter, "adapter is null");
        this.adapter = adapter;
    }

    public void add(T chunk) {
        if(this.closed) {
            throw new IllegalStateException("Is closed.");
        }
        Assert.notNull(chunk);
        this.adapter.onAdd(chunk);
        queue.add(chunk);
    }

    /**
     * Notify this stream that no more chunks will be added.
     */
    public void end() {
        //we allow many many ends, but only first is meaning
        this.queue.add(END);
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        Assert.isTrue(b != null && b.length >= 0, "array is null or have zero length");
        Assert.isTrue(len > 0 && len <= b.length, "len is less than 1 or greater then array len");
        Assert.isTrue(off >= 0, "off is negative");
        try {
            lock.lockInterruptibly();
            int read = 0;
            while(true) {
                T curr = takeCurrent();
                if(curr == null) {
                    return EOF;
                }
                int localoff = off + read;
                int locallen = len - read;
                int res = adapter.readBytes(curr, b, localoff, locallen);
                if(res == EOF) {
                    //current chunk is empty we release it and return
                    releaseCurrent();
                    // we must read at least one byte
                    // also when chunk is ended we do not need to read next chunk if we has any data
                    if(read != 0) {
                        return read;
                    }
                } else {
                    Assert.isTrue(res >= 0, "Invalid number of read bytes: " + res);
                    read += res;
                    if(read == len) {
                        return read;
                    }
                    Assert.isTrue(read < len, "Invalid number of read bytes: read=" + read + " > buffer=" + len);
                }
            }
        } catch (InterruptedException e) {
            throw new IOException("Interrupted", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int read() throws IOException {
        try {
            lock.lockInterruptibly();
            while(true) {
                T curr = takeCurrent();
                if(curr == null) {
                    return EOF;
                }
                int res = adapter.readByte(curr);
                if(res != EOF) {
                    return res;
                }
                releaseCurrent();
            }
        } catch (InterruptedException e) {
            throw new IOException("Interrupted", e);
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private T takeCurrent() throws InterruptedException {
        if(end) {
            return null;
        }
        T curr = this.currentRef.get();
        if(curr == null) {
            Object obj = queue.take();
            if(obj == END) {
                this.end = true;
                return null;
            }
            curr = (T) obj;
            this.currentRef.set(curr);
        }
        return curr;
    }

    /**
     * releaseCurrent can be called out of lock
     */
    private void releaseCurrent() {
        T old = this.currentRef.getAndSet(null);
        if(old != null) {
            adapter.onRemove(old);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void close() throws IOException {
        if(this.closed) {
            return;
        }
        //race on 'close' flag is not important
        this.closed = true;
        super.close();
        releaseCurrent();
        for(Object chunk: queue) {
            if(chunk == END) {
                continue;
            }
            adapter.onRemove((T) chunk);
        }
        queue.clear();
    }
}
