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

package com.codeabovelab.dm.common.log;

import ch.qos.logback.access.spi.IAccessEvent;

import java.util.Map;

/**
 * Access log entry for logstash
 * @see IAccessEvent for details
 */
public class AccessLogRecord extends UUIDLogRecord {
    private static final String TYPE = "accessLog";

    private String requestURI;
    private String requestURL;
    private String remoteHost;
    private String remoteUser;
    private String remoteAddr;
    private String protocol;
    private String method;
    private String serverName;
    private String requestContent;
    private String responseContent;
    private long elapsedTime;
    private int statusCode;

    private Map<String, String> requestHeaderMap;
    private Map<String, String[]> requestParameterMap;
    private Map<String, String> responseHeaderMap;
    private Map<String, Object> attributeMap;

    public AccessLogRecord() {
        setType(TYPE);
    }

    public String getRequestURI() {
        return requestURI;
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    public String getRequestURL() {
        return requestURL;
    }

    public void setRequestURL(String requestURL) {
        this.requestURL = requestURL;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public String getRemoteUser() {
        return remoteUser;
    }

    public void setRemoteUser(String remoteUser) {
        this.remoteUser = remoteUser;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getRequestContent() {
        return requestContent;
    }

    public void setRequestContent(String requestContent) {
        this.requestContent = requestContent;
    }

    public String getResponseContent() {
        return responseContent;
    }

    public void setResponseContent(String responseContent) {
        this.responseContent = responseContent;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public Map<String, String> getRequestHeaderMap() {
        return requestHeaderMap;
    }

    public void setRequestHeaderMap(Map<String, String> requestHeaderMap) {
        this.requestHeaderMap = requestHeaderMap;
    }

    public Map<String, String[]> getRequestParameterMap() {
        return requestParameterMap;
    }

    public void setRequestParameterMap(Map<String, String[]> requestParameterMap) {
        this.requestParameterMap = requestParameterMap;
    }

    public Map<String, String> getResponseHeaderMap() {
        return responseHeaderMap;
    }

    public void setResponseHeaderMap(Map<String, String> responseHeaderMap) {
        this.responseHeaderMap = responseHeaderMap;
    }

    public Map<String, Object> getAttributeMap() {
        return attributeMap;
    }

    public void setAttributeMap(Map<String, Object> attributeMap) {
        this.attributeMap = attributeMap;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public String toString() {
        return "AccessLogRecord{" +
                "requestURI='" + requestURI + '\'' +
                ", requestURL='" + requestURL + '\'' +
                ", remoteHost='" + remoteHost + '\'' +
                ", remoteUser='" + remoteUser + '\'' +
                ", remoteAddr='" + remoteAddr + '\'' +
                ", protocol='" + protocol + '\'' +
                ", method='" + method + '\'' +
                ", serverName='" + serverName + '\'' +
                ", requestContent='" + requestContent + '\'' +
                ", responseContent='" + responseContent + '\'' +
                ", elapsedTime=" + elapsedTime +
                ", statusCode=" + statusCode +
                ", requestHeaderMap=" + requestHeaderMap +
                ", requestParameterMap=" + requestParameterMap +
                ", responseHeaderMap=" + responseHeaderMap +
                ", attributeMap=" + attributeMap +
                "} " + super.toString();
    }
}
