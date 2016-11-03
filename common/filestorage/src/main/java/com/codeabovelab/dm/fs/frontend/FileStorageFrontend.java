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

package com.codeabovelab.dm.fs.frontend;

import com.codeabovelab.dm.common.utils.HttpHeaderSourceAdapter;
import com.codeabovelab.dm.fs.FileStorage;
import com.codeabovelab.dm.fs.FileStorageAbsentException;
import com.codeabovelab.dm.fs.FileStorageException;
import com.codeabovelab.dm.fs.FileStorageExistsException;
import com.codeabovelab.dm.fs.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static com.codeabovelab.dm.fs.dto.FileStorageUtils.SERVICE;

/**
 * Frontend implementation of {@link com.codeabovelab.dm.fs.FileStorage } iface. <p/>
 * It is a single public API which is provided by file storage service.
 */
public class FileStorageFrontend implements FileStorage {

    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024 /* 10 MiB */;
    private final RestTemplate restTemplate;
    private final LoadBalancerClient balancerClient;

    private int maxFileSize = MAX_FILE_SIZE;


    /**
     * Do not use this ctor directly, because is signature can be changed in future.
     */
    @Autowired
    public FileStorageFrontend(LoadBalancerClient balancerClient) {
        this.restTemplate = FileStorageUtils.createRestTemplate();
        this.balancerClient = balancerClient;
    }

    /**
     * Limit of file size.
     *
     * @return
     */
    public int getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * Limit of file size.
     *
     * @param maxFileSize
     */
    public void setMaxFileSize(int maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    @Override
    public String write(final WriteOptions options) throws IOException {
        String result = balancerClient.execute(SERVICE, instance -> {
            FileHandle fileHandle = options.getFileHandle();
            URI uri = constructUrl(fileHandle.getId(), instance);
            HttpHeaders headers = new HttpHeaders();
            FileStorageUtils.setHeaders(fileHandle.getAttributes(), headers);
            InputStream data = fileHandle.getData();
            InputStreamResource resource = null;
            final boolean update = !options.isFailIfExists();
            HttpMethod method = update ? HttpMethod.POST : HttpMethod.PUT;
            if (data != null) {
                resource = new InputStreamResource(data);
            } else if (update) {
                // with update and null (but not a empty) data we want to change attributes only
                method = HttpMethod.PATCH;
            }
            RequestEntity<InputStreamResource> requestEntity = new RequestEntity<>(resource, headers, method, uri);
            ResponseEntity<String> exchange = restTemplate.exchange(requestEntity, String.class);
            throwIfError(options, exchange);
            return exchange.getBody();
        });
        return result;
    }

    private void throwIfError(BasicOptions<?> options, ResponseEntity<?> exchange) {
        HttpStatus statusCode = exchange.getStatusCode();
        if (!statusCode.is2xxSuccessful()) {
            if (statusCode == HttpStatus.NOT_FOUND) {
                if (!options.isFailIfAbsent()) {
                    return; // suppress error as it configured
                }
                throw new FileStorageAbsentException();
            }
            if (statusCode == HttpStatus.CONFLICT) {
                if (!options.isFailIfExists()) {
                    return; // suppress error as it configured
                }
                throw new FileStorageExistsException();
            }
            throw new FileStorageException(exchange.toString());
        }
    }

    @Override
    public FileHandle read(final ReadOptions options) throws IOException {

        FileHandle result = balancerClient.execute(SERVICE, (LoadBalancerRequest<FileHandle>) instance -> {
            final String id = options.getId();
            URI uri = constructUrl(id, instance);
            ResponseEntity<byte[]> entity = restTemplate.exchange(new RequestEntity<>(HttpMethod.GET, uri), byte[].class);
            throwIfError(options, entity);
            HttpHeaders headers = entity.getHeaders();
            SimpleFileHandle.Builder builder = SimpleFileHandle.builder()
                    .id(id);
            byte[] body = entity.getBody();
            if (body != null) {
                builder.streamFactory(FileStorageUtils.streamFactory(body));
            }
            FileStorageUtils.setAttributes(new HttpHeaderSourceAdapter(headers), builder.getAttributes());
            return builder.build();
        });
        return result;
    }

    @Override
    public void delete(final DeleteOptions options) throws IOException {
        balancerClient.execute(SERVICE, instance -> {
            URI uri = constructUrl(options.getId(), instance);
            RequestEntity<Object> requestEntity = new RequestEntity<>(HttpMethod.DELETE, uri);
            ResponseEntity<String> responseEntity = restTemplate.exchange(requestEntity, String.class);
            throwIfError(options, responseEntity);
            return null;
        });

    }

    private URI constructUrl(String id, ServiceInstance instance) throws Exception {
        return FileStorageUtils.constructUrl(this.balancerClient, id, instance);
    }
}
