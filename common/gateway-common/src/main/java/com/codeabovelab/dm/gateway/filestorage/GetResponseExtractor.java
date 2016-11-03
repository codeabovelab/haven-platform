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

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseExtractor;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Extract response to servlet response.
*/
class GetResponseExtractor implements ResponseExtractor<Object> {
    private final HttpServletResponse servletResponse;
    private static final Set<String> FORBIDDEN_HEADERS;
    static {
        Set<String> headers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        headers.add(HttpHeaders.TRANSFER_ENCODING);
        FORBIDDEN_HEADERS = Collections.unmodifiableSet(headers);
    }

    public GetResponseExtractor(HttpServletResponse servletResponse) {
        this.servletResponse = servletResponse;
    }

    @Override
    public Object extractData(ClientHttpResponse response) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(response.getHeaders());
        for(Map.Entry<String, List<String>> e: headers.entrySet()) {
            List<String> values = e.getValue();
            for(int i = 0; i < values.size(); i++) {
                final String key = e.getKey();
                if(FORBIDDEN_HEADERS.contains(key)) {
                    continue;
                }
                servletResponse.setHeader(key, values.get(i));
            }
        }
        try(InputStream is = response.getBody();
            ServletOutputStream os = servletResponse.getOutputStream()) {
            IOUtils.copy(is, os);
            servletResponse.flushBuffer();

        }
        return null;
    }
}
