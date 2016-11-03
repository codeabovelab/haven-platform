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

package com.codeabovelab.dm.gateway.filestorage;

import com.codeabovelab.dm.fs.dto.FileHandle;
import com.codeabovelab.dm.fs.dto.FileStorageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.codeabovelab.dm.fs.dto.FileStorageUtils.SERVICE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * REST controller is implementing the file storage frontend. <p/>
 * We cannot use common http proxy, because need do some auth and modify specific headers. <p/>
 * TODO credentials
 */
@RestController
@RequestMapping("${filestorage.rest.mapping:/files/}")
public class FileStorageRestController {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorageRestController.class);
    public static final String ATTR_PREFIX_LC = FileStorageUtils.HEADER_ATTR_PREFIX.toLowerCase();
    private final RestTemplate restTemplate;
    private final LoadBalancerClient balancerClient;


    @Autowired
    public FileStorageRestController(LoadBalancerClient balancerClient) {
        this.restTemplate = FileStorageUtils.createRestTemplate();
        this.balancerClient = balancerClient;
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    private void readFile(final @PathVariable("id") String id, @RequestParam Map<String,String> allRequestParams, final HttpServletResponse servletResponse) throws Exception {
        final MultiValueMap<String, String> valueMap = new LinkedMultiValueMap<>();
        valueMap.setAll(allRequestParams);

        balancerClient.execute(SERVICE, instance -> {
            // we do not want any value instead of uuid
            URI uri = constructUrl(id, instance);
            URI targetUrl = UriComponentsBuilder.fromUri(uri)
                    .queryParams(valueMap)
                    .build()
                    .toUri();
            LOG.debug("send GET request to {}", targetUrl.toString());
            restTemplate.execute(targetUrl, HttpMethod.GET, null, new GetResponseExtractor(servletResponse));
            return null;
        });

    }

    @RequestMapping(value = "{id}", method = DELETE)
    private ResponseEntity<?> deleteFile(final @PathVariable("id") String id) throws Exception {


        ResponseEntity<Object> responseEntity = balancerClient.execute(SERVICE, instance -> {
            URI uri = constructUrl(id, instance);
            RequestEntity<Object> requestEntity = new RequestEntity<>(HttpMethod.DELETE, uri);
            LOG.debug("send DELETE request to {}", uri.toString());
            return restTemplate.exchange(requestEntity, Object.class);
        });
        return responseEntity;
    }

    @RequestMapping(value = "{id}", method = {PUT, POST, PATCH}, consumes = {"application/*", "image/*"})
    private ResponseEntity<InputStreamResource> writeFile(@PathVariable("id") String id,
                                                          @RequestParam Map<String, String> attributes,
                                                          final HttpServletRequest request) throws Exception {
        InputStreamResource resource = new InputStreamResource(request.getInputStream());
        return transferFile(id, resource, attributes, request);
    }

    @RequestMapping(method = {PUT, POST, PATCH}, consumes = {"application/*", "image/*"})
    private ResponseEntity<InputStreamResource> writeFileGenerateId(@RequestParam Map<String, String> attributes,
                                                          final HttpServletRequest request) throws Exception {
        return writeFile(UUID.randomUUID().toString(), attributes, request);
    }

    private ResponseEntity<InputStreamResource> transferFile(final String id, final InputStreamResource resource,
                                                             final Map<String, String> attributes, final HttpServletRequest request) throws Exception {
        ResponseEntity<InputStreamResource> responseEntity = balancerClient.execute(SERVICE, instance -> {
            URI uri = constructUrl(id, instance);
            HttpHeaders headers = new HttpHeaders();
            processHeaders(attributes, request, headers);
            LOG.debug("send request to {}", uri.toString());
            RequestEntity<InputStreamResource> requestEntity = new RequestEntity<>(resource, headers, HttpMethod.valueOf(request.getMethod()), uri);
            return restTemplate.exchange(requestEntity, InputStreamResource.class);
        });

        return responseEntity;

    }

    @RequestMapping(method = {PUT, POST, PATCH}, consumes = {MULTIPART_FORM_DATA_VALUE})
    private ResponseEntity<InputStreamResource> writeFileMultiPartGenerateId(@RequestParam Map<String, String> attributes,
                                                                             MultipartHttpServletRequest request) throws Exception {
        return writeFileMultiPart(UUID.randomUUID().toString(), attributes, request);
    }

    @RequestMapping(value = "{id}", method = {PUT, POST, PATCH}, consumes = {MULTIPART_FORM_DATA_VALUE})
    private ResponseEntity<InputStreamResource> writeFileMultiPart(@PathVariable("id") String id,
                                                                   @RequestParam Map<String, String> attributes,
                                                                   MultipartHttpServletRequest request) throws Exception {

        final MultipartFile file = request.getFile("file");
        Assert.notNull(file);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(file.getBytes()));
        Map<String, String> params = processFileAttrs(attributes, file);
        return transferFile(id, resource, params, request);
    }

    private Map<String, String> processFileAttrs(Map<String, String> params, MultipartFile file) {
        Map<String, String> result = new HashMap<>(params);
        result.put(FileHandle.ATTR_NAME, file.getOriginalFilename());
        result.put(FileHandle.ATTR_MIME_TYPE, file.getContentType());
        return result;
    }

    private void processHeaders(Map<String, String> attributes, HttpServletRequest request, HttpHeaders headers) {
        Enumeration<String> headerNames = request.getHeaderNames();
        // copy headers which start from specified prefix
        while(headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if(!headerName.startsWith(ATTR_PREFIX_LC)) {
                continue;
            }
            headers.set(headerName, request.getHeader(headerName));
        }
        for(Map.Entry<String, String> e: attributes.entrySet()) {
            //TODO encode headers, remove some headers (author & etc)
            headers.set(FileStorageUtils.HEADER_ATTR_PREFIX + e.getKey(), e.getValue());
        }
    }

    private URI constructUrl(String id, ServiceInstance instance) throws Exception {
        return FileStorageUtils.constructUrl(this.balancerClient, id, instance);
    }

}
