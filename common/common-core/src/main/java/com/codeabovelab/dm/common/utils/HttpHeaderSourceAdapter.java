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

package com.codeabovelab.dm.common.utils;

import com.codeabovelab.dm.common.utils.HttpHeaderSource;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;

import java.util.Iterator;
import java.util.List;

/**
 * Adapter from {@link org.springframework.http.HttpHeaders } to {@link com.codeabovelab.dm.common.utils.HttpHeaderSource }
 */
public class HttpHeaderSourceAdapter implements HttpHeaderSource {
    private final HttpHeaders headers;

    public HttpHeaderSourceAdapter(HttpHeaders headers) {
        this.headers = headers;
    }

    @Override
    public Iterator<String> iterateNames() {
        return this.headers.keySet().iterator();
    }

    @Override
    public String getValue(String name) {
        List<String> strings = this.headers.get(name);
        if(CollectionUtils.isEmpty(strings)) {
            return null;
        }
        return strings.get(0);
    }
}
