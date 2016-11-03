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

import java.net.InetAddress;

public class OSUtils {
    /**
     * operating system family
     */
    private enum Family {
        WINDOWS, LINUX, MAC, UNKNOWN, ANDROID
    }

    static {
        family = detectOsFamily();
    }

    private static final Family family;

    private static Family detectOsFamily() {
        final String kernelName = System.getProperty("os.name").toLowerCase();
        final Family family;
        if(kernelName.contains("linux")) {
            family = Family.LINUX;
        } else if(kernelName.contains("win")) {
            family = Family.WINDOWS;
        } else if(kernelName.contains("mac")) {
            family = Family.MAC;
        } else {
            family = Family.UNKNOWN;
        }
        return family;
    }

    public static String getHostName() {
        String name = null;
        try {
            name = InetAddress.getLocalHost().getHostName();
        } catch(Exception e) {
            // so we use this
            System.out.println("can not detect host name forom localhost");
        }
        // obviously, we don`t need names like `localhost`
        if(name != null && name.contains("localhost")) {
            name = null;
        }
        if(name == null) {
            if(family == Family.WINDOWS) {
                name = System.getenv("COMPUTERNAME");
            } else if(family == Family.LINUX) {
                name = System.getenv("HOSTNAME");
                if(name == null) {
                    name = System.getenv("HOST");
                }
            }
        }
        return name;
    }

    public static String getTempDir() {
        return System.getProperty("java.io.tmpdir");
    }
}
