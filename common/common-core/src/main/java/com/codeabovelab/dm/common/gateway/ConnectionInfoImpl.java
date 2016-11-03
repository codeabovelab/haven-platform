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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Basic implementation of connection info.
 */
public class ConnectionInfoImpl implements ConnectionInfo {

    public static class Builder<B extends Builder<B, T>, T extends ConnectionInfo> {
        private final Set<Class<?>> supportedMessages = new HashSet<>();

        @SuppressWarnings("unchecked")
        private B thiz() {
            return (B) this;
        }

        /**
         * Add most basic type supported by underlying connection protocol.
         * @param supportedMessage
         * @return
         */
        public B addSupportedMessage(Class<?> supportedMessage) {
            getSupportedMessages().add(supportedMessage);
            return thiz();
        }

        /**
         * Set of the most basic types supported by underlying connection protocol.
         * @param supportedMessages
         * @return
         */
        public B supportedMessages(Collection<Class<?>> supportedMessages) {
            setSupportedMessages(supportedMessages);
            return thiz();
        }

        /**
         * Set of the most basic types supported by underlying connection protocol.
         * @param supportedMessages
         */
        public void setSupportedMessages(Collection<Class<?>> supportedMessages) {
            this.supportedMessages.clear();
            this.supportedMessages.addAll(supportedMessages);
        }

        /**
         * Set of the most basic types supported by underlying connection protocol.
         * @return
         */
        public Collection<Class<?>> getSupportedMessages() {
            return supportedMessages;
        }

        @SuppressWarnings("unchecked")
        public T build() {
            return (T)new ConnectionInfoImpl(this);
        }
    }

    private final Set<Class<?>> supportedMessages;

    public ConnectionInfoImpl(Builder<?, ?> builder) {
        this.supportedMessages = Collections.unmodifiableSet(new HashSet<>(builder.supportedMessages));
    }

    @Override
    public Set<Class<?>> getSupportedMessages() {
        return null;
    }

    public static Builder<?, ConnectionInfoImpl> builder() {
        return new Builder<>();
    }
}
