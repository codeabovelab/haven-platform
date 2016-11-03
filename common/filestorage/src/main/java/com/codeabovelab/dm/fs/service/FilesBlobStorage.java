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

package com.codeabovelab.dm.fs.service;

import com.codeabovelab.dm.fs.FileStorageExistsException;
import com.codeabovelab.dm.fs.dto.ExistsFlags;
import org.apache.commons.io.IOUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.abs;

/**
 * Simple implementation of blob storage
 */
public class FilesBlobStorage implements BlobStorage {

    private static final String EXT_BLOB = "blob";
    private static final String EXT_TMP = "tmp";
    private static final int LOCKS_POOL_SIZE = 10;
    private final String dir;
    private final Lock locksPool[];

    public FilesBlobStorage(String dir) throws IOException {
        Path path = Paths.get(dir).toAbsolutePath();
        path = Files.createDirectories(path);
        Assert.isTrue(Files.exists(path), path + " does not exists");
        Assert.isTrue(Files.isDirectory(path), path + " must be a directory");
        this.dir = path.toString();

        locksPool = new Lock[LOCKS_POOL_SIZE];
        for(int i = 0; i < locksPool.length; ++i) {
            locksPool[i] = new ReentrantLock();
        }
    }

    @Override
    public InputStream read(String uuid) throws IOException {
        Path filePath = getPath(uuid, EXT_BLOB);
        if(!Files.exists(filePath)) {
            return null;
        }
        return Files.newInputStream(filePath);
    }

    @Override
    public boolean delete(String uuid) throws IOException {
        Path filePath = getPath(uuid, EXT_BLOB);
        return Files.deleteIfExists(filePath);
    }

    @Override
    public void write(String uuid, InputStream stream, ExistsFlags flags) throws IOException {
        Path filePath = getPath(uuid, EXT_TMP);
        Lock lock = getLock(uuid);
        lock.lock();
        try {
            final Path target = getPath(uuid, EXT_BLOB);
            final boolean exists = Files.exists(target);
            if(stream == null) {
                if(!exists) {
                    // file is not exists and stream null
                    throw new IOException("null stream is not allowed for new file");
                }
                return;
            }
            if(exists) {
                if(flags.isFailIfExists()) {
                    throw new FileStorageExistsException(target.toString());
                } else {
                    Files.delete(target);
                }
            }
            Files.createDirectories(filePath.getParent());
            try(OutputStream os = Files.newOutputStream(filePath, StandardOpenOption.CREATE)) {
                IOUtils.copy(stream, os);
            }
            Files.move(filePath, target, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            lock.unlock();
        }
    }

    private Lock getLock(String uuid) {
        final int lockNum = uuid.hashCode() % locksPool.length;
        return locksPool[abs(lockNum)];
    }

    private Path getPath(String uuid, String ext) {
        // normal UUID looks like 'd3761577-a7f1-41ae-b2a3-adbb1ae987a4' in any letters case
        // therefore we need to remove '-' and convert to lowercase
        String hex = StringUtils.delete(uuid, "-").toLowerCase();
        Assert.isTrue(hex.length() == 32 /* UUID is 128 bit (32 hex byte)! */, "Bad UUID format");
        return Paths.get(this.dir, hex.substring(0, 2), hex.substring(2, 4), hex + "." + ext);
    }
}
