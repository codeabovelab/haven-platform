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

package com.codeabovelab.dm.cluman.ui.msg;

import com.codeabovelab.dm.cluman.events.EventsUtils;
import com.codeabovelab.dm.common.mb.Subscriptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.*;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.socket.messaging.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Utility which route internal events to STOMP clients. Also keep old messages and send its to newly subscribed clients.
 */
@Slf4j
@Component
class EventRouter implements ApplicationListener<AbstractSubProtocolEvent> {


    public static final String PREFIX = "/topic/";

    private class BusData {
        private final Subscriptions<?> bus;
        private final Queue<Object> last = new ArrayBlockingQueue<>(100);

        BusData(Subscriptions<?> bus) {
            this.bus = bus;
        }

        public void open() {
            this.bus.subscribe(this::onEvent);
        }

        private void onEvent(Object o) {
            log.debug("added new event {}", o);
            while(!last.offer(o)) {
                last.poll();
            }
            send(this.bus.getId(), o);
        }

        public List<Object> getEvents() {
            return new ArrayList<>(last);
        }

        public String getId() {
            return bus.getId();
        }
    }

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ConcurrentMap<String, BusData> buses = new ConcurrentHashMap<>();
    private final MessageChannel clientChannel;
    private PathMatcher pathMatcher;

    @Autowired
    public EventRouter(SimpMessagingTemplate simpMessagingTemplate,
                       @Qualifier("clientOutboundChannel") MessageChannel clientChannel,
                       @Qualifier(EventsUtils.BUS_ERRORS)  Subscriptions<?> errorsSubs) {
        this.clientChannel = clientChannel;
        this.simpMessagingTemplate = simpMessagingTemplate;
        //default value
        this.pathMatcher = new AntPathMatcher();
        this.acceptBus(errorsSubs);
    }

    @Autowired
    @Qualifier("simpleBrokerMessageHandler")
    public void onSimpleBrockedMessageChannel(AbstractBrokerMessageHandler handler) {
        // here we try to inherit matcher from subscription registry
        if (!(handler instanceof SimpleBrokerMessageHandler)) {
            return;
        }
        SubscriptionRegistry registry = ((SimpleBrokerMessageHandler) handler).getSubscriptionRegistry();
        if (!(registry instanceof DefaultSubscriptionRegistry)) {
            return;
        }
        PathMatcher pathMatcher = ((DefaultSubscriptionRegistry) registry).getPathMatcher();
        if(pathMatcher != null) {
            this.pathMatcher = pathMatcher;
        }
    }

    @Override
    public void onApplicationEvent(AbstractSubProtocolEvent ev) {
        if(ev instanceof SessionSubscribeEvent) {
            sendHistoryToNewSubscriber(ev);
        } else if(ev instanceof SessionConnectEvent || ev instanceof SessionDisconnectEvent) {
            Authentication user = (Authentication)ev.getUser();
            Object details = user.getDetails();
            String sessionId = null;
            String address = null;
            if(details instanceof WebAuthenticationDetails) {
                WebAuthenticationDetails wad = (WebAuthenticationDetails) details;
                address = wad.getRemoteAddress();
                sessionId = wad.getSessionId();
            }
            if(ev instanceof SessionDisconnectEvent) {
                log.info("WebSocket user \"{}\" was disconnected from {} with HTTP session: {}", user.getName(), address, sessionId);
            } else {
                log.info("WebSocket user \"{}\" was connected from {} with HTTP session: {}", user.getName(), address, sessionId);
            }
        }
    }

    private void sendHistoryToNewSubscriber(AbstractSubProtocolEvent ev) {
        Message<byte[]> msg = ev.getMessage();
        StompHeaderAccessor ha = StompHeaderAccessor.wrap(msg);
        String pattern = ha.getDestination();
        if(!pattern.startsWith(PREFIX)) {
            // we must send only to appropriate paths
            return;
        }
        MessageConverter messageConverter = this.simpMessagingTemplate.getMessageConverter();

        for(BusData data: buses.values()) {
            String dest = getDestination(data.getId());
            if(!this.pathMatcher.match(pattern, dest)) {
                continue;
            }
            for(Object obj: data.getEvents()) {
                StompHeaderAccessor mha = Stomp.createHeaders(ha.getSessionId(), ha.getSubscriptionId());
                mha.setDestination(dest);
                Message<?> message = messageConverter.toMessage(obj, mha.getMessageHeaders());
                clientChannel.send(message);
            }
        }
    }

    private void acceptBus(Subscriptions<?> bus) {
        buses.computeIfAbsent(bus.getId(), (id) -> {
            BusData bd = new BusData(bus);
            bd.open();
            return bd;
        });
    }

    private void send(String bus, Object event) {
        try {
            this.simpMessagingTemplate.convertAndSend(getDestination(bus), event);
        } catch (Exception e) {
            log.error("Send fail", e);
        }
    }

    private String getDestination(String bus) {
        return PREFIX + bus;
    }
}
