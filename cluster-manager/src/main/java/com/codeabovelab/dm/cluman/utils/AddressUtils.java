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

package com.codeabovelab.dm.cluman.utils;

import org.springframework.util.Assert;

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

    public static String setPort(String address, int port) {
        Assert.notNull(address, "Address is null or empty");
        Assert.isTrue(port > 0 && port < Short.MAX_VALUE, "Invalid port number: " + port);
        return getHost(address) + ":" + port;
    }

    public static String getHost(String addr) {
        if (addr == null) {
            return null;
        }
        int portStart = addr.lastIndexOf(':');
        if(portStart < 0) {
            return addr;
        }
        return addr.substring(0, portStart);
    }

}
