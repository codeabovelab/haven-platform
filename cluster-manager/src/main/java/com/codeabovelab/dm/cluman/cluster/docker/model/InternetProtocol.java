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

package com.codeabovelab.dm.cluman.cluster.docker.model;


/**
 * The IP protocols supported by Docker.
 *
 * @see #TCP
 * @see #UDP
 */
public enum InternetProtocol {
    /** The <i>Transmission Control Protocol</i> */
    TCP,

    /** The <i>User Datagram Protocol</i> */
    UDP;

    /**
     * The default {@link InternetProtocol}: {@link #TCP}
     */
    public static final InternetProtocol DEFAULT = TCP;

    /**
     * Returns a string representation of this {@link InternetProtocol} suitable for inclusion in a JSON message. The
     * output is the lowercased name of the Protocol, e.g. <code>tcp</code>.
     *
     * @return a string representation of this {@link InternetProtocol}
     */
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    /**
     * Parses a string to an {@link InternetProtocol}.
     *
     * @param serialized
     *            the protocol, e.g. <code>tcp</code> or <code>TCP</code>
     * @return an {@link InternetProtocol} described by the string
     * @throws IllegalArgumentException
     *             if the argument cannot be parsed
     */
    public static InternetProtocol parse(String serialized) throws IllegalArgumentException {
        try {
            return valueOf(serialized.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing Protocol '" + serialized + "'");
        }
    }

}