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

package com.codeabovelab.dm.cluman.ui.tty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 */
@Slf4j
class TtyProxy implements WebSocketHandler {

    private static final String KEY = TtyProxy.class.getName();
    private final String containerId;
    private final WebSocketSession frontend;
    private volatile WebSocketSession backend;


    TtyProxy(String containerId, WebSocketSession frontend) {
        this.containerId = containerId;
        this.frontend = frontend;
    }

    static void set(WebSocketSession session, TtyProxy ttyProxy) {
        session.getAttributes().put(KEY, ttyProxy);
    }

    static TtyProxy get(WebSocketSession session) {
        return (TtyProxy) session.getAttributes().get(KEY);
    }

    static void close(WebSocketSession session) throws Exception {
        TtyProxy tty = (TtyProxy) session.getAttributes().get(KEY);
        tty.close();
    }

    private synchronized void close() throws Exception {
        frontend.getAttributes().remove(KEY, this);
        WebSocketSession localBackend = this.backend;
        if(localBackend != null) {
            localBackend.close();
        }
    }

    synchronized void toBackend(WebSocketMessage<?> message) throws Exception {
        WebSocketSession localBackend = this.backend;
        if(localBackend != null) {
            localBackend.sendMessage(message);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        synchronized (this) {
            this.backend = backend;
        }
        log.info("Success connect to backed with sessions: front={}, back={}", frontend, backend);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        frontend.sendMessage(message);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Backend transport error:", exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        close();
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
