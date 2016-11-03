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

package com.codeabovelab.dm.fs;

import com.codeabovelab.dm.fs.dto.DeleteOptions;
import com.codeabovelab.dm.fs.dto.FileHandle;
import com.codeabovelab.dm.fs.dto.ReadOptions;
import com.codeabovelab.dm.fs.dto.WriteOptions;

import java.io.IOException;

/**
 * iface for FileStorage service
 */
public interface FileStorage {

    /**
     * Write file.
     * @param options
     */
    String write(WriteOptions options) throws FileStorageException, IOException;

    /**
     * Write file. <p/>
     * At this process all behavior which can modify state is inacceptable.
     * @param options
     * @return
     */
    FileHandle read(ReadOptions options) throws FileStorageException, IOException;

    /**
     * Delete file. <p/>
     * At this process all behavior which create new file is inacceptable.
     * @param options
     */
    void delete(DeleteOptions options) throws FileStorageException, IOException;
}
