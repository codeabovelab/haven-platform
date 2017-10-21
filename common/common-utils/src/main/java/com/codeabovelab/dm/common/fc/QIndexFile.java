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

import lombok.Data;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 */
@Data
class QIndexFile implements AutoCloseable, IndexFile {
    private static final int HEADER_SIZE = FbUtils.SIGN_LEN + 1 + 1;
    private static final long MAX_SIZE = 1024 * 1024 /* 1MiB */;
    private static final byte TYPE = 0x10;
    private static final byte VERSION = 0x01;
    private final File file;
    private final List<String> list = new CopyOnWriteArrayList<>();
    private String id;
    private long maxFileSize;
    private int maxFiles;
    private int maxSize;
    private boolean loaded;

    QIndexFile(File dir) {
        this.file = new File(dir, "index");
    }

    synchronized void load() throws IOException {
        if(loaded) {
            return;
        }
        Path path = file.toPath();
        ByteBuffer buff;
        try (FileChannel fc = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ)) {
            long size = fc.size();
            if (size > MAX_SIZE) {
                throw new FbException("Index file: " + path + " has too large size: " + size);
            }
            buff = ByteBuffer.allocate((int) size).order(ByteOrder.BIG_ENDIAN);
            fc.read(buff);
        }

        buff.flip();

        byte[] tmp = new byte[FbUtils.MAX_STR_LEN];
        FbUtils.readSign(buff);
        FbUtils.readAndValidate(buff, TYPE);
        FbUtils.readAndValidate(buff, VERSION);
        FbUtils.validate(id, FbUtils.readString(tmp, buff), "id") ;
        FbUtils.validate(this.maxFileSize, buff.getLong(), "maxFileSize");
        FbUtils.validate(this.maxFiles, buff.getInt(), "maxFiles");
        FbUtils.validate(this.maxSize, buff.getInt(), "maxSize");
        int listSize = buff.getShort();
        List<String> list = new ArrayList<>(listSize);
        //our files can not have name larger that 256 bytes
        for(int i = 0; i < listSize; i++) {
            String str = FbUtils.readString(tmp, buff);
            list.add(str);
        }
        this.list.addAll(list);
        this.loaded = true;
    }

    synchronized void save() throws IOException {
        int listSize = list.size();
        Assert.isTrue(listSize < Short.MAX_VALUE, "Too large list");
        byte[] idBs = FbUtils.toBytes(id);
        int fileSize = HEADER_SIZE + idBs.length + 1
          + 8 /* maxFileSize */
          + 4 /* maxFiles */
          + 4 /* maxSize */
          + 2 /* listSize */
          ;
        List<byte[]> fileNamesBs = new ArrayList<>(listSize);
        for(int i = 0; i < listSize; ++i) {
            String fileName = list.get(i);
            byte[] fileNameBs = FbUtils.toBytes(fileName);
            fileNamesBs.add(fileNameBs);
            int length = fileNameBs.length;
            fileSize += length + 1;
        }
        ByteBuffer buff = ByteBuffer.allocate(fileSize).order(ByteOrder.BIG_ENDIAN);
        FbUtils.writeSign(buff);
        buff.put(TYPE);
        buff.put(VERSION);
        FbUtils.writeString(idBs, buff);
        buff.putLong(this.maxFileSize);
        buff.putInt(this.maxFiles);
        buff.putInt(this.maxSize);
        buff.putShort((short) listSize);
        for(int i = 0; i < listSize; i++) {
            byte[] fileNameBs = fileNamesBs.get(i);
            FbUtils.writeString(fileNameBs, buff);
        }

        buff.flip();

        Path path = file.toPath();
        try (FileChannel fc = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            fc.write(buff);
        }
    }

    synchronized void delete() {
        this.file.delete();
    }

    @Override
    public synchronized void close() throws Exception {
        save();
    }

    public List<String> getList() {
        return list;
    }

    public void setList(List<String> list)  {
        //below not thread safe
        this.list.clear();
        this.list.addAll(list);
    }

    public boolean isExists() {
        return this.file.exists();
    }
}
