/*
 * Copyright 2017 Code Above Lab LLC
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

package com.codeabovelab.dm.agent.dp;

import com.google.common.collect.Iterators;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 */
class Utils {
    static String reconstructUri(HttpServletRequest request) {
        String q = request.getQueryString();
        String req = request.getRequestURI();
        if(q == null) {
            return req;
        }
        return req + "?" + q;
    }

    static WebSocketVersion getWsVersion(String str) {
        switch (str) {
            case "0":
                return WebSocketVersion.V00;
            case "7":
                return WebSocketVersion.V07;
            case "8":
                return WebSocketVersion.V08;
            case "13":
                return WebSocketVersion.V13;
        }
        return WebSocketVersion.UNKNOWN;
    }

    static void copyHeaders(HttpServletRequest request, HttpHeaders to) {
        Enumeration<String> headers = request.getHeaderNames();
        while(headers.hasMoreElements()) {
            String header = headers.nextElement();
            Iterable<String> iter = () -> Iterators.forEnumeration(request.getHeaders(header));
            to.add(header, iter);
        }
    }
}
