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
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * File backed queue. <p/>
 * Current implementation make dir with name of 'queue.id' in storage,
 * then it make file for each {@link #getMaxItemsInFile() portion of elements}, pull-ed element
 * marked in file as removed, when all elements in file removed - file also removed. When file capacity
 * is exceeded, queue create new file.
 */
@Slf4j
public final class FbQueue<E> extends AbstractQueue<E> implements FileBased {

    @Data
    public static final class Builder<E> {
        private FbStorage storage;
        /**
         * Unique id. It must pe persistent.
         */
        private String id;
        /**
         * Queue size limit, also used for calculate average file size.
         */
        private int maxSize;
        private final FbAdapter<E> adapter;

        public Builder<E> storage(FbStorage storage) {
            setStorage(storage);
            return this;
        }

        public Builder<E> id(String id) {
            setId(id);
            return this;
        }

        public Builder<E> maxSize(int queueSize) {
            setMaxSize(queueSize);
            return this;
        }

        public FbQueue<E> build() {
            return new FbQueue<E>(this);
        }
    }

    private final File queueDir;
    private final String id;
    private final FbStorage storage;
    private final int digitsInFileName;
    private final int maxSize;
    private final FbAdapter<E> adapter;
    private final AtomicInteger filesCounter = new AtomicInteger(-1);
    private final Object lock = new Object();
    private final QIndexFile indexFile;
    private final Deque<QFileHandle<E>> files = new LinkedList<>();

    private FbQueue(Builder<E> b) {
        this.id = b.id;
        //TODO check that id is unique
        Assert.hasText(this.id, "Id is null or empty");
        this.storage = b.storage;
        Assert.notNull(this.storage, "Storage is null");
        this.digitsInFileName = (int) Math.ceil(Math.log10(this.storage.getMaxFiles()));
        this.maxSize = b.maxSize;
        Assert.isTrue(this.maxSize > 0, "Queue size is less than one.");
        this.adapter = b.adapter;
        Assert.notNull(this.adapter, "Adapter is null");
        this.queueDir = new File(this.storage.getStorageDir(), this.id);
        FbStorage.makeAndCheckDir(this.queueDir);
        this.indexFile = new QIndexFile(this.queueDir);
        this.indexFile.init(id, storage);
        this.indexFile.setMaxSize(maxSize);
        if(this.indexFile.isExists()) {
            load();
        }
    }

    private void load() {
        synchronized (lock) {
            List<String> files = null;
            try {
                indexFile.load();
                files = indexFile.getList();
                for (String fileName : files) {
                    File file = new File(this.queueDir, fileName);
                    addFileHandle(file);
                }
            } catch (FbException|IOException e) {
                Path dir = this.queueDir.toPath();
                log.warn("Corrupted data in \"{}\" with error: \"{}\", clear it.", dir, e.toString());
                //corrupted data
                this.indexFile.delete();
                this.files.clear();
                if(files != null) {
                    for(String fileName: files) {
                        try {
                            Files.delete(dir.resolve(fileName));
                        } catch (IOException ex) {/* nothing */}
                    }
                }
            }
        }
    }

    /**
     * Global unique identifier of queue
     */
    public String getId() {
        return id;
    }

    /**
     * Size limit of queue.
     * @return
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Max count of items in single file.
     * @return
     */
    public int getMaxItemsInFile() {
        return QFileHandle.ITEMS_IN_FILE;
    }

    public static <E> Builder<E> builder(FbAdapter<E> adapter) {
        return new Builder<>(adapter);
    }

    /**
     * Iterate from head (first added element) to tail.
     * @return iterator which traverse over queue snapshot.
     */
    @Override
    public Iterator<E> iterator() {
        return iterator(Integer.MAX_VALUE);

    }

    /**
     * Iterate from head (first added element) to tail. Iterate only over last elements. <p/>
     * For example, queue with <code>[(head) 0, 1, 2, 3, 4, 5 (tail)]</code> and <code>last=3</code> iterate over
     * <code>[3, 4, 5 (tail)]</code>.
     * @param last count of last elements, or {@link Integer#MAX_VALUE} for all.
     * @return iterator which traverse over queue snapshot.
     */
    public Iterator<E> iterator(int last) {
        if(last < 0) {
            return Collections.emptyIterator();
        }
        // prevent impact of modifications to iterator we use snapshots
        int qOffset;
        List<FbSnapshot<E>> snapshots = new ArrayList<>();
        synchronized (lock) {
            final int size = size();
            if(last > size) {
                last = size;
            }
            qOffset = size - last;
            for(QFileHandle<E> fh: files) {
                int count = fh.count();
                if(snapshots.isEmpty() && qOffset > count) {
                    // we decrease offset before fist added snapshot only (last snapshot may be less than offset)
                    qOffset -= count;
                } else {
                    snapshots.add(fh.snapshot());
                }
            }
        }
        final int fisrtOffset = qOffset;
        return new Iterator<E>() {
            final Iterator<FbSnapshot<E>> snapshotsIter = snapshots.iterator();
            final List<E> itemsBuff = new ArrayList<>(QFileHandle.ITEMS_IN_FILE);
            Iterator<E> iterator = Collections.emptyIterator();
            private boolean fisrt = true;

            @Override
            public boolean hasNext() {
                boolean hasNext = iterator.hasNext();
                if(hasNext) {
                    return true;
                }
                if(!snapshotsIter.hasNext()) {
                    return false;
                }
                FbSnapshot<E> snapshot = snapshotsIter.next();
                int offset = 0;
                if(fisrt) {
                    fisrt = false;
                    offset = fisrtOffset;
                }
                itemsBuff.clear();
                snapshot.visit(offset, itemsBuff::add);
                iterator = itemsBuff.iterator();
                return iterator.hasNext();
            }

            @Override
            public E next() {
                return iterator.next();
            }
        };
    }

    @Override
    public int size() {
        int size = 0;
        synchronized (lock) {
            for(QFileHandle<E> fh: files) {
                size += fh.count();
            }
        }
        return size;
    }

    @Override
    public boolean offer(E e) {
        Assert.notNull(e, "element is null");
        synchronized (lock) {
            int size = 0;
            QFileHandle<E> last = null;
            for(QFileHandle<E> fh: files) {
                size += fh.count();
                last = fh;
            }
            if(size >= maxSize) {
                return false;
            }
            while(true) {
                if(last != null && last.offer(e)) {
                    return true;
                }
                last = allocate(last);
            }
        }
    }

    /**
     * Add element like into queue tail, if queue is full then remove head.
     * @param e
     * @return
     */
    public void push(E e) {
        Assert.notNull(e, "element is null");
        synchronized (lock) {
            int size = 0;
            QFileHandle<E> last = null;
            for(QFileHandle<E> fh: files) {
                size += fh.count();
                last = fh;
            }
            while(size >= maxSize) {
                poll();
                size--;
            }
            while(true) {
                if(last != null && last.offer(e)) {
                    return;
                }
                last = allocate(last);
            }
        }
    }

    @Override
    public E poll() {
        return onHead((fh) -> {
            E val = fh.poll();
            deallocate(fh);
            return val;
        });
    }

    @Override
    public E peek() {
        return onHead((fh) -> {
            E val = fh.peek();
            return val;
        });
    }

    private <T> T onHead(Function<QFileHandle<E>, T> consumer) {
        synchronized (lock) {
            QFileHandle<E> fh;
            while(true) {
                fh = files.peekFirst();
                if(fh == null) {
                    return null;
                }
                synchronized (fh) {
                    if(fh.count() != 0) {
                        return consumer.apply(fh);
                    }
                    deallocate(fh);
                }
            }
        }
    }

    private QFileHandle<E> allocate(QFileHandle<E> oldHead) {
        synchronized (lock) {
            try {
                QFileHandle<E> currHead = files.peekLast();
                if(currHead == oldHead) {
                    File file = allocateFile();
                    currHead = addFileHandle(file);
                }
                return currHead;
            } catch (IOException e) {
                throw new FbException(e);
            }
        }
    }

    private QFileHandle<E> addFileHandle(File file) throws IOException {
        QFileHandle<E> currHead;
        currHead = new QFileHandle<>(this.storage, this.adapter, file);
        files.addLast(currHead);
        return currHead;
    }

    private void deallocate(QFileHandle<E> fh) {
        // we cannot remove first because it may be
        // already removed from another thread, and we
        // have only one method which can remove compare and remove first
        if(fh.count() == 0) {
            files.removeFirstOccurrence(fh);
            fh.remove();
        }
    }

    private File allocateFile() throws IOException {
        //we try to create file while not find non exists name
        final int maxFiles = storage.getMaxFiles();
        int tries = maxFiles;
        while(tries >= 0) {
            tries--;// it prevent hangs when limit is reach
            int num = -1;
            while (num < 0) {
                num = filesCounter.incrementAndGet();
                if(num >= maxFiles || num < 0) {
                    filesCounter.compareAndSet(num, num = 0);
                }
            }
            String fileName = String.format("%0" + digitsInFileName + "d", num);
            File file = new File(queueDir, fileName);
            if(file.createNewFile()) {
               return file;
            }
        }
        throw new FbException("The 'maxFiles' limit is reached. We can not allocate file.");
    }

    @Override
    public void close() throws Exception {
        synchronized (lock) {
            indexFile.setList(this.files.stream().map(QFileHandle::getFileName).collect(Collectors.toList()));
            indexFile.close();
            QFileHandle<E> fh;
            while((fh = files.pollFirst()) != null) {
                Closeables.close(fh);
            }
        }
    }
}
