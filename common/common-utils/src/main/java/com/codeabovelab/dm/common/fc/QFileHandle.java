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

package com.codeabovelab.dm.common.fc;

import com.codeabovelab.dm.common.utils.Closeables;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

/**
 * File handle. <p/>
 * File structure.
 * <pre>
 *  entry:
 *          collection type (1b)
 *      sign |  version (1b)
 *     / 3b \|\/| /index of queue\
 *     F11EBA0001[  int32[1024]  ][all other space - data]
 *
 *  index of queue:
 *   most significant byte
 *   contains 'deleted flag'
 *     \  all other 31 bits - the size of each item in Big-endian
 *      \/
 *     [00 00 00 00] * N
 * </pre>
 */
final class QFileHandle<E> implements AutoCloseable {
    /**
     * Do not make this iface a public api.
     */
    private interface Visitor {
        boolean visit(int i, int size, int offset) throws IOException;
    }

    private static final int DEL_MASK = 0x80000000;
    static final int ITEMS_IN_FILE = 1024;
    private static final int DIRTY_COUNT = -1;
    private static final byte QUEUE_TYPE = 0x00;
    private static final byte SCHEMA_VERSION = 0x01;
    private static final int INDEX_OFF = 2 + FbUtils.SIGN_LEN;
    private static final int HEADER_OFF = ITEMS_IN_FILE * 4 + INDEX_OFF;
    private final FbStorage storage;
    private final File file;
    private final RandomAccessFile raf;
    private final int[] index = new int[ITEMS_IN_FILE];
    private final ByteBuffer indexBuff = ByteBuffer.allocate(ITEMS_IN_FILE * 4).order(ByteOrder.BIG_ENDIAN);
    private final FbAdapter<E> adapter;
    private int maxItemSize = 64 /* initial number number mean nothing*/;
    private int count = DIRTY_COUNT;
    private int tail;
    private long tailOff;

    QFileHandle(FbStorage storage, FbAdapter<E> adapter, File file) throws IOException {
        this.storage = storage;
        this.file = file;
        this.adapter = adapter;
        this.raf = new RandomAccessFile(this.file, "rw");
        if(this.raf.length() == 0) {
            save();
        } else {
            load();
        }
    }

    private synchronized void load() throws IOException {
        this.raf.seek(0);
        FbUtils.readSign(this.raf);
        FbUtils.readAndValidate(this.raf, QUEUE_TYPE);
        FbUtils.readAndValidate(this.raf, SCHEMA_VERSION);
        for(int i = 0; i < index.length; ++i) {
            index[i] = this.raf.readInt();
        }
    }

    private synchronized void save() throws IOException {
        this.raf.seek(0);
        FbUtils.writeSign(this.raf);
        this.raf.writeByte(QUEUE_TYPE);
        this.raf.writeByte(SCHEMA_VERSION);
        saveIndex();
    }

    private synchronized void saveIndex() throws IOException {
        this.raf.seek(INDEX_OFF);
        indexBuff.clear();
        indexBuff.asIntBuffer().put(index);
        indexBuff.flip();
        this.raf.write(indexBuff.array());
    }

    private synchronized void iterate(Visitor v) {
        //TODO we may iterate over internal snapshot, it allow us to skip lock in iteration process
        iterate(this.index, v);
    }

    private static void iterate(int[] index, Visitor v) {
        try {
            int off = HEADER_OFF;
            for(int i = 0; i < index.length; i++) {
                int item = index[i];
                int size = getSize(item);
                if(size == 0) {
                    // the unallocated item, mean end
                    break;
                }
                if(!isDeleted(item)) {
                    boolean next = v.visit(i, size, off);
                    if(!next) {
                        break;
                    }
                }
                off += size;
            }
        } catch (IOException e) {
            throw new FbException(e);
        }
    }

    synchronized int count() {
        //we do not use iterator for avoid object allocation
        if(this.count == DIRTY_COUNT) {
            int c = 0;
            int off = HEADER_OFF;
            this.tail = index.length;
            for(int i = 0; i < index.length; i++) {
                int item = index[i];
                int size = getSize(item);
                off += size;
                if(isDeleted(item)) {
                    continue;
                }
                if(size == 0) {
                    // the unallocated item, mean end
                    this.tail = i;
                    this.tailOff = off;
                    break;
                }
                c++;
            }
            this.count = c;
        }
        return this.count;
    }

    private static boolean isDeleted(int i) {
        return (i & DEL_MASK) != 0;
    }

    synchronized boolean offer(E e) {
        count();
        if(tail == index.length) {
            return false;
        }
        try {
            byte[] bytes = adapter.serialize(e);
            if(bytes == null || bytes.length == 0) {
                throw new FbException("Adapter return null or empty buffer for: " + e);
            }
            this.raf.seek(this.tailOff);
            this.raf.write(bytes);
            index[tail] = bytes.length;
            //TODO we need update runtime index only after success save
            // for prevent index corrupt
            saveIndex();
        } catch (IOException ex) {
            throw new FbException(ex);
        } finally {
            dirty();
        }
        return true;
    }

    private void dirty() {
        this.count = DIRTY_COUNT;
    }

    synchronized E poll() {
        PeekVisitor v = new PeekVisitor();
        iterate(v);
        v.remove();
        return v.getValue();
    }

    synchronized E peek() {
        PeekVisitor v = new PeekVisitor();
        iterate(v);
        return v.getValue();
    }

    @Override
    public synchronized void close() throws Exception {
        raf.close();
    }

    /**
     * Do not allow user to pass own consumer into this method because it may hang.
     * @param consumer
     */
    synchronized void readAllTo(Consumer<E> consumer) {
        iterate(new ReadVisitor(consumer));
    }

    private static int getSize(int item) {
        int size = item & 0x7FFFFFFF;
        if(size < 0) {
            throw new FbException("Size is negative.");
        }
        return size;
    }

    public void remove() {
        Closeables.close(raf);
        file.delete();
    }

    private class PeekVisitor implements Visitor {

        private E value;
        private int i;

        @Override
        public boolean visit(int i, int size, int offset) throws IOException {
            this.i = i;
            byte[]  buff = new byte[size];
            raf.seek(offset);
            raf.readFully(buff);
            this.value = adapter.deserialize(buff, 0, size);
            return false;
        }

        public E getValue() {
            return value;
        }

        public void remove() {
            Assert.isTrue(!isDeleted(index[i]));
            index[i] |= DEL_MASK;
            dirty();
            try {
                saveIndex();
            } catch (IOException e) {
                throw new FbException(e);
            }
        }
    }

    String getFileName() {
        return this.file.getName();
    }

    public FbSnapshot<E> snapshot() {
        return new QFileHandleSnapshot();
    }

    class QFileHandleSnapshot implements FbSnapshot<E> {
        private final int[] index = new int[ITEMS_IN_FILE];
        private final int count;
        private final int tail;
        private final long tailOff;
        private final int maxItemSize;

        QFileHandleSnapshot() {
            synchronized (QFileHandle.this) {
                System.arraycopy(QFileHandle.this.index, 0, this.index, 0, QFileHandle.this.index.length);
                this.maxItemSize = QFileHandle.this.maxItemSize;
                this.count = QFileHandle.this.count();
                this.tail = QFileHandle.this.tail;
                this.tailOff = QFileHandle.this.tailOff;
            }
        }

        @Override
        public void visit(int offset, Consumer<E> consumer) {
            ReadVisitor rv = new ReadVisitor(consumer);
            rv.setStart(offset);
            QFileHandle.iterate(index, rv);
        }

        @Override
        public void close() throws Exception {
            /*
            TODO we must record snapshots in file handle, and raise warning at closing handle with unclosed
               snapshots, because snapshot does not work correct after closing handle.
             */
        }
    }

    private class ReadVisitor implements Visitor {

        private final Consumer<E> consumer;
        byte[]  buff;
        int start;

        public ReadVisitor(Consumer<E> consumer) {
            this.consumer = consumer;
            buff = new byte[maxItemSize];
        }

        /**
         * index which reading will start
         * @param start
         */
        public void setStart(int start) {
            this.start = start;
        }

        @Override
        public boolean visit(int i, int size, int offset) throws IOException {
            if(i < this.start) {
                return true;
            }
            if(size > buff.length) {
                buff = new byte[maxItemSize = size];
            }
            raf.seek(offset);
            raf.readFully(buff, 0, size);
            E e = adapter.deserialize(buff, 0, size);
            consumer.accept(e);
            return true;
        }
    }
}
