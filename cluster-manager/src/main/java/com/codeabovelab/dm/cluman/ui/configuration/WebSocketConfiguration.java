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

package com.codeabovelab.dm.cluman.ui.configuration;

import com.codeabovelab.dm.common.security.TempAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

import java.security.Principal;

/**
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration extends AbstractWebSocketMessageBrokerConfigurer {

    public static final String ENDPOINT = "/ui/stomp";

    @Autowired
    private SecurityChannelInterceptor interceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(ENDPOINT)
          .setAllowedOrigins(UiConfiguration.ALLOWED_ORIGIN)
          .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.setInterceptors(interceptor);
    }

    /**
     * Interceptor which temporary extract auth from message into security context. <p/>
     * Note that current implementation is not good.
     */
    @Component
    private static class SecurityChannelInterceptor implements ExecutorChannelInterceptor {

        public static final String AUTH_KEY = TempAuth.class.getName();

        @Override
        public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
            SimpMessageHeaderAccessor smha = SimpMessageHeaderAccessor.wrap(message);
            Principal user = smha.getUser();
            if(user instanceof Authentication) {
                TempAuth auth = TempAuth.open((Authentication)user);
                smha.setHeader(AUTH_KEY, auth);
            }
            return message;
        }

        @Override
        public void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler, Exception ex) {
            SimpMessageHeaderAccessor smha = SimpMessageHeaderAccessor.wrap(message);
            Object attribute = smha.getHeader(AUTH_KEY);
            if(attribute instanceof TempAuth) {
                ((TempAuth)attribute).close();
            }
        }

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            return message;
        }

        @Override
        public void postSend(Message<?> message, MessageChannel channel, boolean sent) {

        }

        @Override
        public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {

        }

        @Override
        public boolean preReceive(MessageChannel channel) {
            return true;
        }

        @Override
        public Message<?> postReceive(Message<?> message, MessageChannel channel) {
            return message;
        }

        @Override
        public void afterReceiveCompletion(Message<?> message, MessageChannel channel, Exception ex) {

        }
    }

}