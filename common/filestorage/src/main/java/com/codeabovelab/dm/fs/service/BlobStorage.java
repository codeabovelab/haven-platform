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

import com.codeabovelab.dm.fs.FileStorageException;
import com.codeabovelab.dm.fs.dto.ExistsFlags;

import java.io.IOException;
import java.io.InputStream;

/**
 * Iface for atomic storage for BLOB data.
 * Storage does not support modification operations? but allow to overwrite existing data.
 */
public interface BlobStorage {

    /**
     * read stream by specified UUID.
     * @param uuid
     * @return stream or null if not exists
     */
    InputStream read(String uuid) throws IOException, FileStorageException;

    /**
     * Delete object with specified UUID.
     * @param uuid
     * @return true if data exists and deleted, false if not exists.
     * @throws java.io.IOException if file can not be deleted
     */
    boolean delete(String uuid) throws IOException, FileStorageException;

    /**
     * Write stream into new file or over exists file.
     * @param uuid
     * @param stream
     * @param flags
     */
    void write(String uuid, InputStream stream, ExistsFlags flags) throws IOException, FileStorageException;
}
