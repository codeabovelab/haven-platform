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

import com.codeabovelab.dm.common.utils.AttributeSupport;
import com.codeabovelab.dm.common.utils.Throwables;
import com.codeabovelab.dm.fs.FileStorage;
import com.codeabovelab.dm.fs.FileStorageAbsentException;
import com.codeabovelab.dm.fs.dto.DeleteOptions;
import com.codeabovelab.dm.fs.dto.FileHandle;
import com.codeabovelab.dm.fs.dto.ReadOptions;
import com.codeabovelab.dm.fs.dto.WriteOptions;
import com.codeabovelab.dm.fs.entity.FileAttribute;
import com.codeabovelab.dm.fs.repository.AttributesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Simple backend for storing files in directory.
 */
@Component
@Transactional
public class SimpleFileStorageBackend implements FileStorage {

    @Autowired
    private BlobStorage blobStorage;

    @Autowired
    private AttributesRepository attributesRepository;

    @Override
    public String write(WriteOptions options) {
        FileHandle fileHandle = options.getFileHandle();
        final String id = fileHandle.getId();
        try(InputStream stream = fileHandle.getData()) {
            blobStorage.write(id, stream, options);
        } catch (IOException e) {
            throw Throwables.asRuntime(e);
        }
        /* if we update existing file and it has attributes, then we need to merge the attributes,
         but in future option which overwrites old attrs can be added */
        AdapterImpl adapter = new AdapterImpl(UUID.fromString(id));
        AttributeSupport.merge(adapter, adapter, fileHandle.getAttributes());
        return id;
    }

    @Override
    public FileHandle read(final ReadOptions options) {
        final String id = options.getId();
        final Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        List<FileAttribute> list = attributesRepository.findByFileId(UUID.fromString(id));
        for(FileAttribute attribute: list) {
            map.put(attribute.getName(), attribute.getData());
        }
        return new LazyFileHandle(map, options);
    }

    @Override
    public void delete(DeleteOptions options) {
        String id = options.getId();
        try {
            boolean wasExists = blobStorage.delete(id);
            if(!wasExists && options.isFailIfAbsent()) {
                throw createNotExists(id);
            }
        } catch (IOException e) {
            throw Throwables.asRuntime(e);
        }
        List<FileAttribute> attrs = attributesRepository.findByFileId(UUID.fromString(id));
        attributesRepository.delete(attrs);
    }

    private RuntimeException createNotExists(String id) {
        return new FileStorageAbsentException(id + " does not exists");
    }

    private class AdapterImpl implements AttributeSupport.Adapter<FileAttribute>, AttributeSupport.Repository<FileAttribute> {

        private final UUID fileId;

        public AdapterImpl(UUID fileId) {
            this.fileId = fileId;
        }

        @Override
        public FileAttribute create() {
            FileAttribute attribute = new FileAttribute();
            attribute.setFileId(fileId);
            return attribute;
        }

        @Override
        public void setKey(FileAttribute attr, String key) {
            attr.setName(key);
        }

        @Override
        public String getKey(FileAttribute attr) {
            return attr.getName();
        }

        @Override
        public void setValue(FileAttribute attr, String value) {
            attr.setData(value);
        }

        @Override
        public String getValue(FileAttribute attr) {
            return attr.getData();
        }

        @Override
        public List<FileAttribute> getAttributes() {
            return attributesRepository.findByFileId(fileId);
        }

        @Override
        public <S extends FileAttribute> List<S> save(Iterable<S> entities) {
            return attributesRepository.save(entities);
        }

        @Override
        public void delete(Iterable<? extends FileAttribute> entities) {
            attributesRepository.delete(entities);
        }
    }

    private class LazyFileHandle implements FileHandle {

        private final Map<String, String> attrs;
        private final String id;
        private final ReadOptions options;

        LazyFileHandle(Map<String, String> map, ReadOptions options) {
            this.options = options;
            this.id = options.getId();
            attrs = Collections.unmodifiableMap(map);
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public InputStream getData() {
            try {
                InputStream read = blobStorage.read(id);
                if(read == null && options.isFailIfAbsent()) {
                    throw createNotExists(id);
                }
                return read;
            } catch (IOException e) {
                throw Throwables.asRuntime(e);
            }
        }

        @Override
        public Map<String, String> getAttributes() {
            return attrs;
        }

        @Override
        public String toString() {
            return "LazyFileHandle{" +
              "id='" + id + '\'' +
              ", attrs=" + attrs +
              '}';
        }
    }
}
