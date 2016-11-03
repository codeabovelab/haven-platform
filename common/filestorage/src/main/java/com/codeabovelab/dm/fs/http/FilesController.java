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

package com.codeabovelab.dm.fs.http;

import com.codeabovelab.dm.fs.FileStorage;
import com.codeabovelab.dm.fs.FileStorageAbsentException;
import com.codeabovelab.dm.fs.FileStorageExistsException;
import com.codeabovelab.dm.fs.dto.*;
import org.apache.http.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Spring rest controller which used as http entry point to our filestorgae module.
 * Also, this controller use REST concept (a http methods PUT, GET & etc.) for its api.
 */
@RestController
public class FilesController {

    private static final Logger LOG = LoggerFactory.getLogger(FilesController.class);

    private final FileStorage fileStorage;

    @Autowired
    private UrlBuilder urlBuilder;

    @Autowired
    public FilesController(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    /**
     * Do write file into storage. It used fo create and update operations. <p/>
     * Above operations differs by HTTP method: <ul>
     *   <li> POST is create</li>
     *   <li> PUT - update full file and attributes </li>
     *   <li> PATCH - update only attributes </li>
     * </ul>
     * On create we accept id or create new random id. <p/>
     * Fail if file is not exists on update and if is exists on create.
     * @param id uuid or null
     * @param attributes
     * @param request
     * @return entity with id
     * @throws Exception
     */
    @RequestMapping(value = "/files/{id}", method = {RequestMethod.PUT, RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<String> write(@PathVariable("id") String id, @RequestParam Map<String, String> attributes, final HttpServletRequest request) throws Exception  {
        final HttpMethod method = HttpMethod.valueOf(request.getMethod());
        final boolean update = method != HttpMethod.POST;
        if(TextUtils.isEmpty(id)) {
            if(update) {
                return new ResponseEntity<>("Cannot update with 'id' is null", HttpStatus.BAD_REQUEST);
            } else {
                id = UUID.randomUUID().toString();
            }
        }
        SimpleFileHandle.Builder builder = SimpleFileHandle.builder().id(id);
        builder.attributes(attributes);// below we override url attr with headers
        FileStorageUtils.setAttributes(new HttpServletRequestHeaderSource(request), builder.getAttributes());
        if(method != HttpMethod.PATCH) {
            builder.streamFactory(new ServletRequestStreamFactory(request));
        }
        FileHandle handle = builder.build();
        final String url = urlBuilder.buildUrl(id);
        try {
            fileStorage.write(WriteOptions.builder()
                    .failIfExists(update)
                    .failIfAbsent(!update)
                    .fileHandle(handle)
                    .build());
            HttpHeaders headers = new HttpHeaders();

            return new ResponseEntity<>(url, headers, update? HttpStatus.OK : HttpStatus.CREATED);
        } catch (FileStorageExistsException e) {
            return new ResponseEntity<>(url, HttpStatus.CONFLICT);
        } catch (FileStorageAbsentException e) {
            return new ResponseEntity<>(url, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Read specified file. All file attributes added as http headers with prefix {@link FileStorageUtils#HEADER_ATTR_PREFIX }. <p/>
     * Fail if file is not exists.
     * @param id
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/files/{id}", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> read(@PathVariable("id") String id, @RequestParam Map<String,String> allRequestParams) throws Exception {
        try {
            FileHandle handle = fileStorage.read(ReadOptions.builder().id(id).failIfAbsent(true).build());
            HttpHeaders headers = new HttpHeaders();
            setHeaders(handle, headers, allRequestParams);
            InputStreamResource body = new InputStreamResource(handle.getData());
            return new ResponseEntity<>(body, headers, HttpStatus.OK);
        } catch (FileStorageAbsentException e) {
            return new ResponseEntity<>((InputStreamResource)null, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Delete specified file. Fail if file is not exists.
     * @param id
     * @throws Exception
     */
    @RequestMapping(value = "/files/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> delete(@PathVariable("id") String id) throws Exception {
        try  {
            fileStorage.delete(DeleteOptions.builder().id(id).failIfAbsent(true).build());
            return new ResponseEntity<Object>(HttpStatus.OK);
        } catch (FileStorageAbsentException e) {
            return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
        }
    }

    private void setHeaders(FileHandle handle, HttpHeaders headers, Map<String, String> requestParams) throws Exception {
        HashMap<String, String> attributes = new  HashMap<>(handle.getAttributes());

        for (Map.Entry<String, String> entry : requestParams.entrySet()) {
            attributes.put(entry.getKey().toLowerCase(), entry.getValue());

        }

        String type = attributes.get(FileHandle.ATTR_MIME_TYPE.toLowerCase());
        if(StringUtils.isEmpty(type)) {
            type = MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;
        }
        try {
            headers.setContentType(MediaType.parseMediaType(type));
        } catch (InvalidMediaTypeException e) {
            logFileException(handle, e);
        }

        String name = attributes.get(FileHandle.ATTR_NAME);
        String disposition = attributes.get(HttpHeaders.CONTENT_DISPOSITION.toLowerCase());
        if(StringUtils.isEmpty(disposition)) {
            disposition = "attachment";
        }
        if(!StringUtils.isEmpty(name)) {
            headers.set(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=" + FileStorageUtils.encode(name));
        }

        String val = attributes.get(FileHandle.ATTR_DATE_CREATION);
        if(!StringUtils.isEmpty(val)) {
            try {
                long date = Long.parseLong(val);
                headers.setDate(HttpHeaders.DATE, date);
            } catch (NumberFormatException e) {
                logFileException(handle, e);
            }
        }

        FileStorageUtils.setHeaders(attributes, headers);
    }

    private void logFileException(FileHandle handle, Throwable e) {
        LOG.warn("On file " + handle.getId(), e);
    }

}
