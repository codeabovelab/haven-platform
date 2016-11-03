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

package com.codeabovelab.dm.fs.configuration;

import com.codeabovelab.dm.fs.repository.AttributesRepository;
import com.codeabovelab.dm.fs.service.BlobStorage;
import com.codeabovelab.dm.fs.service.FilesBlobStorage;
import com.codeabovelab.dm.fs.service.SimpleFileStorageBackend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * File Storage for storing files on file system
 */
@Configuration
@ComponentScan(
  basePackageClasses = {
    SimpleFileStorageBackend.class,
    AttributesRepository.class
  })
public class FileStorageModuleConfig {
    @Value("${dm.fs.filesBlobStorage.dir:/data/files}")
    private String dataDir;

    /**
     * Max file size in bytes. Currently set at 10MB
     */
    @Value("${dm.fs.maxFileSize:10485760}")
    private int maxFileSize;

    public int getMaxFileSize() {
        return maxFileSize;
    }

    @Bean
    BlobStorage blobStorage() throws IOException {
       return new FilesBlobStorage(dataDir);
    }
}
