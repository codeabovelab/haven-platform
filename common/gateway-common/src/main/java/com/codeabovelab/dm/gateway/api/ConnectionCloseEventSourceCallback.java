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

package com.codeabovelab.dm.gateway.api;

import com.codeabovelab.dm.common.security.dto.AuthenticationData;
import com.codeabovelab.dm.common.gateway.ClientConnection;
import com.codeabovelab.dm.common.gateway.ConnectionAttributeKeys;
import com.codeabovelab.dm.common.gateway.events.ClientDisconnectedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Send {@link com.codeabovelab.dm.common.gateway.events.ClientDisconnectedEvent } at connection close. <p/>
 * Note that it need correct {@link ConnectionAttributeKeys#KEY_AUTHENTICATION } attribute in connection.
 */
@Component
public class ConnectionCloseEventSourceCallback implements ConnectionCloseCallback {

    private ConnectionEventHub eventHub;

    @Autowired
    public ConnectionCloseEventSourceCallback(ConnectionEventHub eventHub) {
        this.eventHub = eventHub;
    }

    @Override
    public void connectionClose(ClientConnection connection) {
        AuthenticationData auth = connection.getAttributes().get(ConnectionAttributeKeys.KEY_AUTHENTICATION);
        if(auth == null) {
            return;
        }
        final SecurityContext sc = SecurityContextHolder.getContext();
        sc.setAuthentication(auth);
        try {
            eventHub.listen(ClientDisconnectedEvent.builder()
              //TODO right cause we do not known it in this context
              .cause(ClientDisconnectedEvent.Cause.SERVER_CLOSE)
              .connectionId(connection.getId())
              .build());
        } finally {
            sc.setAuthentication(null);
        }
    }
}
