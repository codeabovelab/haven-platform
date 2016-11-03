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

import com.google.common.base.MoreObjects;

/**
 * Event from client connection. All classes which implement this iface must be serializable to JSON.
 */
public abstract class ConnectionEvent {

    public static abstract class Builder<B extends Builder<B, T>, T extends ConnectionEvent> {

        protected String connectionId;

        @SuppressWarnings("unchecked")
        protected final B thiz() {
            return (B) this;
        }

        /**
         * Id of connection which cause this event.
         * @see ClientConnection#getId()
         * @return
         */
        public String getConnectionId() {
            return connectionId;
        }

        /**
         * Id of connection which cause this event.
         * @see ClientConnection#getId()
         * @param connectionId
         * @return
         */
        public B connectionId(String connectionId) {
            setConnectionId(connectionId);
            return thiz();
        }

        /**
         * Id of connection which cause this event.
         * @see ClientConnection#getId()
         * @param connectionId
         */
        public void setConnectionId(String connectionId) {
            this.connectionId = connectionId;
        }

        public abstract T build();
    }

    protected final String connectionId;

    public ConnectionEvent(Builder<?, ?> b) {
        this.connectionId = b.connectionId;
    }

    /**
     * Id of connection which cause this event.
     * @see ClientConnection#getId()
     * @return
     */
    public String getConnectionId() {
        return connectionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConnectionEvent)) {
            return false;
        }

        ConnectionEvent event = (ConnectionEvent) o;

        if (connectionId != null ? !connectionId.equals(event.connectionId) : event.connectionId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return connectionId != null ? connectionId.hashCode() : 0;
    }

    protected void toString(MoreObjects.ToStringHelper helper) {
        helper.add("connectionId", connectionId);
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
        toString(helper);
        return helper.toString();
    }
}
