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

package com.codeabovelab.dm.common.utils;

import com.google.common.net.InetAddresses;
import org.springframework.util.Assert;

import java.net.InetAddress;

/**
 */
public class AddressUtils {

    public static int getPort(String addr) {
        if (addr == null) {
            return -1;
        }
        int portStart = addr.lastIndexOf(':');
        String portStr = addr.substring(portStart + 1);
        return Integer.parseInt(portStr);
    }

    public static boolean hasPort(String addr) {
        if (addr == null) {
            return false;
        }
        int portStart = addr.lastIndexOf(':');
        return portStart > 0 && (addr.length() - portStart) > 1;
    }

    public static String setPort(String address, int port) {
        Assert.notNull(address, "Address is null or empty");
        Assert.isTrue(port > 0 && port < Short.MAX_VALUE, "Invalid port number: " + port);
        int from = 0;
        if(isIpv6(address)) {
            from = address.indexOf(']');
        }
        int portStart = address.lastIndexOf(':');
        String hostAndProto = address;
        if(portStart > from) {
            hostAndProto = address.substring(0, portStart);
        }
        return hostAndProto + ":" + port;
    }

    /**
     * Test that url has ipv6 address
     * @return true if url has an ipv6 address
     */
    public static boolean isIpv6(String url) {
        if(url == null) {
            return false;
        }
        // ipv6 url looks like 'https://['<addr>']'(':'<port>)?
        int sqBegin = url.indexOf('[');
        int sqEnd = url.indexOf(']', sqBegin);
        return sqBegin >= 0 && sqEnd > sqBegin;
    }

    /**
     * Extract host:port pair from url.
     * @param addr any url with host & port
     * @return host:port
     */
    public static String getHostPort(String addr) {
        if (addr == null) {
            return null;
        }
        int hostStart = getHostStart(addr);
        int portEnd = addr.indexOf('/', hostStart);
        if(portEnd < 0 && hostStart < 0) {
            return addr;
        }
        if(hostStart > 0) {
            if(portEnd < hostStart) {
                return addr.substring(hostStart);
            }
            return addr.substring(hostStart, portEnd);
        }
        return addr.substring(0, portEnd);
    }

    private static int getHostStart(String addr) {
        final int prefixLen = 3  /* '://'.length() */;
        int hostStart = addr.indexOf("://");
        if(hostStart > 0) {
            hostStart += prefixLen;
        }
        return hostStart;
    }

    private static int getHostEnd(String addr, boolean ipv6, int from) {
        if(ipv6) {
            from = addr.indexOf(']', from);
        }
        int hostEnd = addr.indexOf(':', from);
        if(hostEnd < 0) {
            hostEnd = addr.indexOf('/', from);
        }
        return hostEnd;
    }

    public static boolean isHttps(String url) {
        return url != null && url.startsWith("https://");
    }

    /**
     * Extract host from url.
     * @param addr any url with host
     * @return host
     */
    public static String getHost(String addr) {
        if (addr == null) {
            return null;
        }
        int hostStart = getHostStart(addr);
        boolean ipv6 = isIpv6(addr);
        int hostEnd = getHostEnd(addr, ipv6, hostStart);
        if(hostEnd < 0 && hostStart < 0) {
            return addr;
        }
        if(hostStart > 0) {
            if(hostEnd < hostStart) {
                return addr.substring(hostStart);
            }
            return addr.substring(hostStart, hostEnd);
        }
        return addr.substring(0, hostEnd);
    }

    public static String setHost(String addr, String host) {
        int hostStart = getHostStart(addr);
        boolean ipv6 = isIpv6(addr);
        int hostEnd = getHostEnd(addr, ipv6, hostStart);
        if(hostEnd < 0 && hostStart < 0) {
            return host;
        }
        String prefix = hostStart > 0? addr.substring(0, hostStart) : "";
        String suff = hostEnd > 0? addr.substring(hostEnd) : "";
        return prefix + host + suff;
    }

    public static boolean isLocal(String host) {
        if("localhost".equalsIgnoreCase(host) || "localhost.localdomain".equalsIgnoreCase(host)) {
            return true;
        }
        boolean ip = InetAddresses.isInetAddress(host);
        if(!ip) {
            return true;
        }
        return InetAddresses.forString(host).isLoopbackAddress();
    }
}
