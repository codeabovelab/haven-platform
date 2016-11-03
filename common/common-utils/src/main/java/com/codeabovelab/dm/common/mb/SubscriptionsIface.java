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

package com.codeabovelab.dm.common.mb;

import java.util.function.Consumer;

/**
 * Common iface for any object which is support for subscription.
 */
public interface SubscriptionsIface<M> {

    /**
     * Method which subscribe specified listener on current bus. If listener already subscribed then doing nothing.
     * @param listener
     */
    void subscribe(Consumer<M> listener);

    /**
     * Act like {@link #subscribe(Consumer)}, but return closeable which is doing unsubcription.
     * @param listener
     * @return
     */
    default Subscription openSubscription(Consumer<M> listener) {
        subscribe(listener);
        return new SubscriptionImpl<>(this, listener);
    }

    /**
     * Method which unsubscribe specified listener from current bus. If listener not subscribed then doing nothing.
     * @param listener
     */
    void unsubscribe(Consumer<M> listener);
}
