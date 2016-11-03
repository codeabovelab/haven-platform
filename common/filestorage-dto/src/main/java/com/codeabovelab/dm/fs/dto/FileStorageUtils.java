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

package com.codeabovelab.dm.fs.dto;

import com.codeabovelab.dm.common.utils.HttpHeaderSource;
import com.codeabovelab.dm.common.utils.Throwables;
import com.codeabovelab.dm.common.utils.Uuids;
import com.codeabovelab.dm.fs.FsResponseErrorHandler;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.mail.internet.MimeUtility;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

/**
 * Some common utilities and constants for file storage.
 */
public final class FileStorageUtils {
    /**
     * This prefix used for publishing file attributes as http header in internal REST api. <p/>
     * Note that all values will has been encoded with <code>MimeUtility.encodeText(name, "UTF-8", null);<code/>
     */
    public static final String HEADER_ATTR_PREFIX = "X-File-Attribute-";
    /**
     * Attribute prefix in lower case
     */
    private static final String ATTR_PREFIX_LC = HEADER_ATTR_PREFIX.toLowerCase();

    /**
     * Id of filestorage service
     */
    public static final String SERVICE = "dm-filestorage-core";

    private FileStorageUtils() {
    }

    /**
     * Create stream factory from specified data. <p/>
     * Note that this method does not copy data, therefore any modification on data affect to stream.
     * @param data
     * @return
     */
    public static Callable<InputStream> streamFactory(final byte[] data) {
        return () -> new ByteArrayInputStream(data);
    }

    /**
     * Create stream factory from specified base64 encoded data.
     * @param base64Data
     * @return
     */
    public static Base64StreamFactory base64StreamFactory(String base64Data) {
        return new Base64StreamFactory(base64Data);
    }

    /**
     * Create configured rest template fro using with file storage
     * @return
     */
    public static RestTemplate createRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new FsResponseErrorHandler());
        // restTemplate can not handle response while do coping large streams, because we lost some error messages
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setBufferRequestBody(false);
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }

    /**
     * Reconstruct url with filestorage service instance which has been resolved from load balancer.
     * @param client
     * @param id
     * @return
     * @throws Exception
     */
    public static URI constructUrl(LoadBalancerClient client, String id, ServiceInstance instance) throws Exception {
        // we do not want any value instead of uuid
        Uuids.validate(id);
        final URI src = new URI("http", null, instance.getServiceId(), 80, "/files/" + id, null, null);
        return client.reconstructURI(instance, src);
    }

    /**
     * Set attributes from http headers
     * @param source
     * @param attributes
     */
    public static void setAttributes(HttpHeaderSource source, Map<String, String> attributes) {
        Iterator<String> headerNames = source.iterateNames();
        final int prefixLen = ATTR_PREFIX_LC.length();
        while(headerNames.hasNext()) {
            // all name is presented in lower case
            String name = headerNames.next().toLowerCase();
            if(!name.startsWith(ATTR_PREFIX_LC)) {
                continue;
            }
            String val = source.getValue(name);
            String key = name.substring(prefixLen);
            attributes.put(key, val);
        }
    }

    /**
     * Set http headers from attributes
     * @param attributes
     * @param headers
     */
    public static void setHeaders(Map<String, String> attributes, HttpHeaders headers) {
        for(Map.Entry<String, String> e: attributes.entrySet()) {
            String ev = e.getValue();
            if(!StringUtils.isEmpty(ev)) {
                headers.set(FileStorageUtils.HEADER_ATTR_PREFIX + e.getKey(), encode(ev));
            }
        }
    }

    /**
     * Encode specified value as RFC 2047
     * @param name
     * @return
     * @throws Exception
     */
    public static String encode(String name) {
        try {
            return MimeUtility.encodeText(name, "UTF-8", null);
        } catch (UnsupportedEncodingException e) {
            throw Throwables.asRuntime(e);
        }
    }

    /**
     * Create unmodifiable copy of map, which keys is case insensitive.
     * @param attributes
     * @return
     */
    public static Map<String, String> unmodifiableCaseInsensitiveCopy(Map<String, String> attributes) {
        TreeMap<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        map.putAll(attributes);
        return Collections.unmodifiableMap(map);
    }
}
