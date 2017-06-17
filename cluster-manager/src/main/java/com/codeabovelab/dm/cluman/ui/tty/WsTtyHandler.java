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

import com.codeabovelab.dm.cluman.ds.container.ContainerRegistration;
import com.codeabovelab.dm.cluman.ds.container.ContainerStorage;
import com.codeabovelab.dm.cluman.ds.nodes.NodeRegistration;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.model.DockerContainer;
import com.codeabovelab.dm.cluman.model.NodeInfo;
import com.codeabovelab.dm.cluman.security.TempAuth;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.codeabovelab.dm.common.utils.AddressUtils;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 */
@Slf4j
@Component
public class WsTtyHandler implements WebSocketHandler {

    @Autowired
    private ContainerStorage containerStorage;

    @Autowired
    private NodeStorage nodeStorage;

    @Autowired
    private WebSocketClient webSocketClient;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        URI uri = session.getUri();
        try {
            UriComponents uc = UriComponentsBuilder.fromUri(uri).build();
            MultiValueMap<String, String> params = uc.getQueryParams();
            String containerId = params.getFirst("container");
            try(TempAuth ta = withAuth(session)) {
                connectToContainer(session, containerId);
            }
        } catch (Exception e) {
            log.error("Can not establish connection for '{}' due to error:", uri, e);
        }
    }

    private TempAuth withAuth(WebSocketSession session) {
        Authentication auth = (Authentication) session.getPrincipal();
        return TempAuth.open(auth);
    }

    private void connectToContainer(WebSocketSession session, String containerId) {
        ContainerRegistration containerReg = containerStorage.getContainer(containerId);
        ExtendedAssert.notFound(containerReg, "Can not find container: " + containerId);
        NodeRegistration nodeReg = nodeStorage.getNodeRegistration(containerReg.getNode());
        DockerContainer dc = containerReg.getContainer();
        NodeInfo nodeInfo = nodeReg.getNodeInfo();
        TtyProxy tty = new TtyProxy(containerReg.getId(), session, ImmutableMap.<String, String>builder()
          .put("container.name", dc.getName())
          .put("container.image", dc.getImage())
          .put("node.name", nodeInfo.getName())
          .put("node.addr", nodeInfo.getAddress())
          .build()
        );
        TtyProxy.set(session, tty);
        ListenableFuture<WebSocketSession> future = webSocketClient.doHandshake(tty, getContainerUri(containerReg.getId(), nodeReg));
        future.addCallback((r) -> {}, (e) -> log.error("failure to open backend connection to '{}' of cluster '{}' due to error: ", containerId, nodeReg.getCluster(), e));
    }

    private String getContainerUri(String containerId, NodeRegistration nr) {
        String addr = nr.getNodeInfo().getAddress();
        String host = AddressUtils.getHostPort(addr);
        String proto = AddressUtils.isHttps(addr)? "wss" : "ws";
        return proto + "://" + host + "/containers/" + containerId +
          "/attach/ws?stream=true&stdin=true&stdout=true&stderr=true";
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        TtyProxy tty = TtyProxy.get(session);
        if(tty == null) {
            return;
        }
        tty.toBackend(message);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Frontend transport error: ", exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        TtyProxy tty = TtyProxy.get(session);
        if(tty != null) {
            tty.closeCausedFront();
        }

    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
