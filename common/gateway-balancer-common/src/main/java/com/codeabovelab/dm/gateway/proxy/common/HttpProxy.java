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

package com.codeabovelab.dm.gateway.proxy.common;

import com.codeabovelab.dm.common.utils.Closeables;
import org.apache.http.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.*;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Java HTTP proxy which is based on
 * https://github.com/mitre/HTTP-Proxy-Servlet (author David Smiley dsmiley@mitre.org). <p/>
 * We do some refactoring and remove servlet dependency.
 */
public class HttpProxy implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(HttpProxy.class);
    private final static String FORWARD_HEADER_NAME = "X-Forwarded-For";
    /**
     * User agents shouldn't send the url fragment but what if it does?
     */
    private final static boolean DO_SEND_URL_FRAGMENT = true;

    private final ProxyClient proxyClient;

    public HttpProxy(ProxyClient proxyClient) {
        this.proxyClient = proxyClient;

    }

    @PostConstruct
    public void start() {
        proxyClient.start();
    }

    @Override
    public void close() {
        Closeables.close(proxyClient);
    }

    private void log(String msg, Exception e) {
        LOG.error(msg, e);
    }

    @SuppressWarnings("deprecation")
    public void service(HttpProxyContext proxyContext) throws Exception {
        final HttpServletRequest servletRequest = proxyContext.getRequest();
        final HttpServletResponse servletResponse = proxyContext.getResponse();
        // Make the Request
        //note: we won't transfer the protocol version because I'm not sure it would truly be compatible
        String method = servletRequest.getMethod();
        String proxyRequestUri = rewriteUrlFromRequest(proxyContext);
        HttpRequest proxyRequest;
        //spec: RFC 2616, sec 4.3: either of these two headers signal that there is a message body.
        if (servletRequest.getHeader(HttpHeaders.CONTENT_LENGTH) != null ||
                servletRequest.getHeader(HttpHeaders.TRANSFER_ENCODING) != null) {
            HttpEntityEnclosingRequest requestWithBody = new BasicHttpEntityEnclosingRequest(method, proxyRequestUri);
            requestWithBody.setEntity(createEntity(servletRequest));
            proxyRequest = requestWithBody;
        } else {
            proxyRequest = new BasicHttpRequest(method, proxyRequestUri);
        }

        copyRequestHeaders(proxyContext, proxyRequest);

        setXForwardedForHeader(servletRequest, proxyRequest);
        setXUUIDHeader(proxyRequest, proxyContext);

        HttpResponse proxyResponse = null;
        try {
            // Execute the request
            if (LOG.isDebugEnabled()) {
                LOG.debug("proxy " + method + " uri: " + servletRequest.getRequestURI() + " -- " + proxyRequest.getRequestLine().getUri());
            }
            proxyResponse = proxyClient.execute(proxyContext.getTargetHost(), proxyRequest);

            // Process the response
            int statusCode = proxyResponse.getStatusLine().getStatusCode();

            if (doResponseRedirectOrNotModifiedLogic(proxyContext, proxyResponse, statusCode)) {
                //the response is already "committed" now without any body to send
                //TODO copy response headers?
                return;
            }

            // Pass the response code. This method with the "reason phrase" is deprecated but it's the only way to pass the
            //  reason along too.
            //noinspection deprecation
            servletResponse.setStatus(statusCode, proxyResponse.getStatusLine().getReasonPhrase());

            copyResponseHeaders(proxyResponse, servletRequest, servletResponse);

            // Send the content to the client
            copyResponseEntity(proxyResponse, servletResponse);

        } finally {
            // make sure the entire entity was consumed, so the connection is released
            if (proxyResponse != null) {
                consumeQuietly(proxyResponse.getEntity());
            }
            //Note: Don't need to close servlet outputStream:
            // http://stackoverflow.com/questions/1159168/should-one-call-close-on-httpservletresponse-getoutputstream-getwriter
        }
    }

    private HttpEntity createEntity(HttpServletRequest servletRequest) throws IOException {
        final String contentType = servletRequest.getContentType();
        // body with 'application/x-www-form-urlencoded' is handled by tomcat therefore we cannot
        // obtain it through input stream and need some workaround
        if (ContentType.APPLICATION_FORM_URLENCODED.getMimeType().equals(contentType)) {
            List<NameValuePair> entries = new ArrayList<>();
            // obviously that we also copy params from url, but we cannot differentiate its
            Enumeration<String> names = servletRequest.getParameterNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                entries.add(new BasicNameValuePair(name, servletRequest.getParameter(name)));
            }
            return new UrlEncodedFormEntity(entries, servletRequest.getCharacterEncoding());
        }

        // Add the input entity (streamed)
        //  note: we don't bother ensuring we close the servletInputStream since the container handles it
        return new InputStreamEntity(servletRequest.getInputStream(),
                servletRequest.getContentLength(),
                ContentType.create(contentType));
    }

    private boolean doResponseRedirectOrNotModifiedLogic(HttpProxyContext proxyContext,
                                                         HttpResponse proxyResponse,
                                                         int statusCode) throws ServletException, IOException {
        HttpServletResponse servletResponse = proxyContext.getResponse();
        // Check if the proxy response is a redirect
        // The following code is adapted from org.tigris.noodle.filters.CheckForRedirect
        if (statusCode >= HttpServletResponse.SC_MULTIPLE_CHOICES /* 300 */
                && statusCode < HttpServletResponse.SC_NOT_MODIFIED /* 304 */) {
            Header locationHeader = proxyResponse.getLastHeader(HttpHeaders.LOCATION);
            if (locationHeader == null) {
                throw new ServletException("Received status code: " + statusCode
                        + " but no " + HttpHeaders.LOCATION + " header was found in the response");
            }
            // Modify the redirect to go to this proxy servlet rather that the proxied host
            String locStr = rewriteUrlFromResponse(proxyContext, locationHeader.getValue());

            servletResponse.sendRedirect(locStr);
            return true;
        }
        // 304 needs special handling.  See:
        // http://www.ics.uci.edu/pub/ietf/http/rfc1945.html#Code304
        // We get a 304 whenever passed an 'If-Modified-Since'
        // header and the data on disk has not changed; server
        // responds w/ a 304 saying I'm not going to send the
        // body because the file has not changed.
        if (statusCode == HttpServletResponse.SC_NOT_MODIFIED) {
            servletResponse.setIntHeader(HttpHeaders.CONTENT_LENGTH, 0);
            servletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return true;
        }
        return false;
    }

    /**
     * HttpClient v4.1 doesn't have the
     * {@link EntityUtils#consumeQuietly(HttpEntity)} method.
     */
    private void consumeQuietly(HttpEntity entity) {
        try {
            EntityUtils.consume(entity);
        } catch (IOException e) {//ignore
            log(e.getMessage(), e);
        }
    }

    /**
     * These are the "hop-by-hop" headers that should not be copied.
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html
     * I use an HttpClient HeaderGroup class instead of Set<String> because this
     * approach does case insensitive lookup faster.
     */
    private static final HeaderGroup hopByHopHeaders;

    static {
        hopByHopHeaders = new HeaderGroup();
        String[] headers = new String[]{
                "Connection", "Keep-Alive", "Proxy-Authenticate", "Proxy-Authorization",
                "TE", "Trailers", "Transfer-Encoding", "Upgrade"};
        for (String header : headers) {
            hopByHopHeaders.addHeader(new BasicHeader(header, null));
        }
    }

    /**
     * Copy request headers from the servlet client to the proxy request.
     */
    private void copyRequestHeaders(HttpProxyContext proxyContext, HttpRequest proxyRequest) {
        HttpServletRequest servletRequest = proxyContext.getRequest();
        // Get an Enumeration of all of the header names sent by the client
        Enumeration enumerationOfHeaderNames = servletRequest.getHeaderNames();
        while (enumerationOfHeaderNames.hasMoreElements()) {
            String headerName = (String) enumerationOfHeaderNames.nextElement();
            //Instead the content-length is effectively set via InputStreamEntity
            if (headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
                continue;
            }
            if (hopByHopHeaders.containsHeader(headerName)) {
                continue;
            }

            Enumeration headers = servletRequest.getHeaders(headerName);
            while (headers.hasMoreElements()) {//sometimes more than one value
                String headerValue = (String) headers.nextElement();
                // In case the proxy host is running multiple virtual servers,
                // rewrite the Host header to ensure that we get content from
                // the correct virtual server
                if (headerName.equalsIgnoreCase(HttpHeaders.HOST)) {
                    HttpHost host = proxyContext.getTargetHost();
                    headerValue = host.getHostName();
                    if (host.getPort() != -1) {
                        headerValue += ":" + host.getPort();
                    }
                } else if (headerName.equalsIgnoreCase(org.apache.http.cookie.SM.COOKIE)) {
                    headerValue = getRealCookie(headerValue);
                }
                proxyRequest.addHeader(headerName, headerValue);
            }
        }
    }

    private void setXUUIDHeader(HttpRequest proxyRequest, HttpProxyContext proxyContext) {
        if (proxyContext.getUid() != null) {
            proxyRequest.addHeader(HttpProxyContext.REQUEST_UUID, proxyContext.getUid());
        }
    }

    private void setXForwardedForHeader(HttpServletRequest servletRequest,
                                        HttpRequest proxyRequest) {

        String newHeader = servletRequest.getRemoteAddr();
        String existingHeader = servletRequest.getHeader(FORWARD_HEADER_NAME);
        if (existingHeader != null) {
                newHeader = existingHeader + ", " + newHeader;
        }
        proxyRequest.setHeader(FORWARD_HEADER_NAME, newHeader);
        
    }

    /**
     * Copy proxied response headers back to the servlet client.
     */
    private void copyResponseHeaders(HttpResponse proxyResponse, HttpServletRequest servletRequest,
                                     HttpServletResponse servletResponse) {
        Header[] allHeaders = proxyResponse.getAllHeaders();
        for (int i = 0, l = allHeaders.length; i < l; i++) {
            Header header = allHeaders[i];
            String name = header.getName();
            if (hopByHopHeaders.containsHeader(name)) {
                continue;
            }
            String value = header.getValue();
            if (name.equalsIgnoreCase(org.apache.http.cookie.SM.SET_COOKIE) ||
                    name.equalsIgnoreCase(org.apache.http.cookie.SM.SET_COOKIE2)) {
                copyProxyCookie(servletRequest, servletResponse, header);
            } else {
                if (!servletResponse.containsHeader(name)) {
                    servletResponse.addHeader(name, value);
                }
            }
        }
    }

    /**
     * Copy cookie from the proxy to the servlet client.
     * Replaces cookie path to local path and renames cookie to avoid collisions.
     */
    private void copyProxyCookie(HttpServletRequest servletRequest,
                                 HttpServletResponse servletResponse, Header header) {
        List<HttpCookie> cookies = HttpCookie.parse(header.getValue());
        String path = servletRequest.getContextPath(); // path starts with / or is empty string
        path += servletRequest.getServletPath(); // servlet path starts with / or is empty string
        for (int i = 0, l = cookies.size(); i < l; i++) {
            HttpCookie cookie = cookies.get(i);
            //set cookie name prefixed w/ a proxy value so it won't collide w/ other cookies
            String proxyCookieName = getCookieNamePrefix() + cookie.getName();
            Cookie servletCookie = new Cookie(proxyCookieName, cookie.getValue());
            servletCookie.setComment(cookie.getComment());
            servletCookie.setMaxAge((int) cookie.getMaxAge());
            servletCookie.setPath(path); //set to the path of the proxy servlet
            // don't set cookie domain
            servletCookie.setSecure(cookie.getSecure());
            servletCookie.setVersion(cookie.getVersion());
            servletResponse.addCookie(servletCookie);
        }
    }

    /**
     * Take any client cookies that were originally from the proxy and prepare them to send to the
     * proxy.  This relies on cookie headers being set correctly according to RFC 6265 Sec 5.4.
     * This also blocks any local cookies from being sent to the proxy.
     * TODO FIX cookies accumulating
     */
    public static String getRealCookie(String cookieValue) {
        StringBuilder escapedCookie = new StringBuilder();
        String cookies[] = cookieValue.split("; ");
        for (int i = 0, l = cookies.length; i < l; i++) {
            String cookie = cookies[i];
            String cookieSplit[] = cookie.split("=");
            if (cookieSplit.length == 2) {
                String cookieName = cookieSplit[0];
                if (cookieName.startsWith(getCookieNamePrefix())) {
//                    cookieName = cookieName.substring(getCookieNamePrefix().length());
//                    if (escapedCookie.length() > 0) {
//                        escapedCookie.append("; ");
//                    }
//                    escapedCookie.append(cookieName).append("=").append(cookieSplit[1]);
                }
            }

            cookieValue = escapedCookie.toString();
        }
        return cookieValue;
    }

    /**
     * The string prefixing rewritten cookies.
     */
    private static String getCookieNamePrefix() {
        return "!Proxy!";
    }

    /**
     * Copy response body data (the entity) from the proxy to the servlet client.
     */
    private void copyResponseEntity(HttpResponse proxyResponse, HttpServletResponse servletResponse) throws IOException {
        HttpEntity entity = proxyResponse.getEntity();
        if (entity != null) {
            OutputStream servletOutputStream = servletResponse.getOutputStream();
            entity.writeTo(servletOutputStream);
        }
    }

    /**
     * Reads the request URI from {@code servletRequest} and rewrites it, considering targetUri.
     * It's used to make the new request.
     */
    private String rewriteUrlFromRequest(HttpProxyContext proxyContext) {
        HttpServletRequest servletRequest = proxyContext.getRequest();
        StringBuilder uri = new StringBuilder(500);
        uri.append(proxyContext.getTargetPath());
        // Handle the path given to the servlet
        if (servletRequest.getPathInfo() != null) {//ex: /my/path.html
            uri.append(HttpProxyUtil.encodeUriQuery(servletRequest.getPathInfo()));
        }
        // Handle the query string & fragment
        String queryString = servletRequest.getQueryString();//ex:(following '?'): name=value&foo=bar#fragment
        String fragment = null;
        //split off fragment from queryString, updating queryString if found
        if (queryString != null) {
            int fragIdx = queryString.indexOf('#');
            if (fragIdx >= 0) {
                fragment = queryString.substring(fragIdx + 1);
                queryString = queryString.substring(0, fragIdx);
            }
        }

        queryString = rewriteQueryStringFromRequest(servletRequest, queryString);
        if (queryString != null && queryString.length() > 0) {
            uri.append('?');
            uri.append(HttpProxyUtil.encodeUriQuery(queryString));
        }

        if (DO_SEND_URL_FRAGMENT && fragment != null) {
            uri.append('#');
            uri.append(HttpProxyUtil.encodeUriQuery(fragment));
        }
        return uri.toString();
    }

    private String rewriteQueryStringFromRequest(HttpServletRequest servletRequest, String queryString) {
        return queryString;
    }

    /**
     * For a redirect response from the target server, this translates {@code theUrl} to redirect to
     * and translates it to one the original client can use.
     */
    private String rewriteUrlFromResponse(HttpProxyContext proxyContext, String theUrl) {
        HttpServletRequest servletRequest = proxyContext.getRequest();
        //TODO document example paths
        final String targetUri = proxyContext.getTargetPath();
        if (theUrl.startsWith(targetUri)) {
            String curUrl = servletRequest.getRequestURL().toString();//no query
            String pathInfo = servletRequest.getPathInfo();
            if (pathInfo != null) {
                assert curUrl.endsWith(pathInfo);
                curUrl = curUrl.substring(0, curUrl.length() - pathInfo.length());//take pathInfo off
            }
            theUrl = curUrl + theUrl.substring(targetUri.length());
        }
        return theUrl;
    }
}
