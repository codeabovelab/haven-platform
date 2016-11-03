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

import java.util.Collection;
/**
 * This interface is used by connection management command handlers. <p/>
 * Note that this interface implementation must be threadsafe.
 */
public interface ClientsConnections {

    /**
     * Return collection of all current clients connections.
     * @return
     */
    void getConnections(Collection<ClientConnection> connections);

    /**
     * Return collection of ids all current clients connections. Usual this method must faster than {@link #getConnections(java.util.Collection)} ,
     * or in worst case not slower.
     * @see ClientConnection#getId()
     * @return
     */
    void getConnectionsIds(Collection<String> ids);

    /**
     * Return connection by its id.
     * @param id
     * @return
     */
    ClientConnection getConnection(String id);
    ClientConnection getConnectionByDevice(String device);
}
