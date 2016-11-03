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

package com.codeabovelab.dm.common.gateway.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.codeabovelab.dm.common.gateway.ConnectionEvent;

/**
 * Event caused on client disconnection. <p/>
 */
public class ClientDisconnectedEvent extends ConnectionEvent {

    /**
     * Disconnection cause
     */
    public enum Cause {
        TIMEOUT, CLIENT_CLOSE, SERVER_CLOSE
    }

    public static class Builder extends ConnectionEvent.Builder<Builder, ClientDisconnectedEvent> {

        private Cause cause;

        /**
         * Disconnection cause
         * @return
         */
        public Cause getCause() {
            return cause;
        }

        /**
         * Disconnection cause
         * @param cause
         * @return
         */
        public Builder cause(Cause cause) {
            setCause(cause);
            return thiz();
        }

        /**
         * Disconnection cause
         * @param cause
         */
        public void setCause(Cause cause) {
            this.cause = cause;
        }

        @Override
        public ClientDisconnectedEvent build() {
            return new ClientDisconnectedEvent(this);
        }
    }

    private final Cause cause;

    @JsonCreator
    public ClientDisconnectedEvent(Builder b) {
        super(b);
        this.cause = b.cause;
    }

    /**
     * Disconnection cause
     * @return
     */
    public Cause getCause() {
        return cause;
    }

    public static Builder builder() {
        return new Builder();
    }
}
