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
import com.google.common.base.MoreObjects;
import com.codeabovelab.dm.common.gateway.ConnectionEvent;

/**
 * Event caused on client connection.
 */
public class ClientConnectedEvent extends ConnectionEvent {

    public static class Builder extends ConnectionEvent.Builder<Builder, ClientConnectedEvent> {

        private long ttl;

        /**
         * Time (in ms) to connection is alive, after last event (except disconnected event).
         * @return
         */
        public long getTtl() {
            return ttl;
        }

        /**
         * Time (in ms) to connection is alive, after last event (except disconnected event).
         * @param ttl
         * @return
         */
        public Builder ttl(long ttl) {
            setTtl(ttl);
            return this;
        }

        /**
         * Time (in ms) to connection is alive, after last event (except disconnected event).
         * @param ttl
         */
        public void setTtl(long ttl) {
            this.ttl = ttl;
        }

        @Override
        public ClientConnectedEvent build() {
            return new ClientConnectedEvent(this);
        }
    }

    private final long ttl;

    @JsonCreator
    public ClientConnectedEvent(Builder b) {
        super(b);
        this.ttl = b.ttl;
    }

    public long getTtl() {
        return ttl;
    }

    public static Builder builder() {
        return new Builder();
    }


    @Override
    protected void toString(MoreObjects.ToStringHelper helper) {
        super.toString(helper);
        helper.add("ttl", ttl);
    }
}
