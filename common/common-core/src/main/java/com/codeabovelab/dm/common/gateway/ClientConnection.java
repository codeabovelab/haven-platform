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

package com.codeabovelab.dm.common.gateway;

import org.springframework.util.concurrent.ListenableFuture;

/**
 * Iface for connection between gateway and client.
 */
public interface ClientConnection {
    /**
     * Id of client connection. <p/>
     * It can be null, because some implementations may provide id only after credentials. <p/>
     * Note that this id must be unique for all system, therefore it must include 'module instance id' and its local connection id.
     * @return
     */
    String getId();

    /**
     * Id of connected device. In lifetime of connection it attribute can be changed from null to concrete device id (in
     * some protocols we can not known device id at first package).
     * @return
     */
    String getDevice();

    /**
     * Return connection info.
     * @return
     */
    ConnectionInfo getConnectionInfo();

    /**
     * Mutable container for different connection attributes.
     * @return
     */
    ConnectionAttributes getAttributes();

    /**
     * Close connection.
     * @return
     */
    ListenableFuture<Void> close();

    /**
     * Push generic data into connection. Data handled by protocol implementation, some implementations can support
     * usual java bean, other - not, so we can check supported messages by {@link ConnectionInfo#getSupportedMessages() }.
     * @see com.codeabovelab.dm.common.gateway.TextMessage
     * @param data
     * @return listenable future
     */
    <T> ListenableFuture<T> send(T data);
}
