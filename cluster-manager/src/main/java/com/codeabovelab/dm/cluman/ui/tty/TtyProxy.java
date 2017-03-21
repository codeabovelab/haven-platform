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

import com.codeabovelab.dm.common.utils.Closeables;
import com.google.common.base.MoreObjects;
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
    private final Object backendLock = new Object();
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

    void closeCausedBack() {
        // split close method for easy resolve cause of closing
        log.info("Close caused back of {}", this);
        close();
    }

    void closeCausedFront() {
        // split close method for easy resolve cause of closing
        log.info("Close caused front of {}", this);
        close();
    }

    private void close() {
        frontend.getAttributes().remove(KEY, this);
        // usually this method called twice - at close frontend, and at close backend
        WebSocketSession localBackend;
        synchronized (backendLock) {
            localBackend = this.backend;
        }
        Closeables.close(localBackend);
        Closeables.close(frontend);
    }

    void toBackend(WebSocketMessage<?> message) {
        WebSocketSession localBackend;
        synchronized (backendLock) {
            localBackend = this.backend;
        }
        if(localBackend == null) {
            return;
        }
        try {
            if(!localBackend.isOpen()) {
                log.warn("Message to closed backend, {}", this);
                closeCausedBack();
                return;
            }
            localBackend.sendMessage(message);
        } catch (Exception e) {
            log.error("In {}, can no send message to backend due to: ", this, e);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        synchronized (backendLock) {
            this.backend = session;
        }
        log.info("Success connect to backed with sessions: front={}, back={}", frontend, session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        try {
            if(!frontend.isOpen()) {
                log.warn("Message to closed frontend, {}", this);
                closeCausedFront();
                return;
            }
            frontend.sendMessage(message);
        } catch (Exception e) {
            log.error("In {}, can no send message to frontend due to: ", this, e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Backend transport error:", exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        closeCausedBack();
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
          .add("containerId", containerId)
          .add("frontend", frontend)
          .add("backend", backend)
          .toString();
    }
}
