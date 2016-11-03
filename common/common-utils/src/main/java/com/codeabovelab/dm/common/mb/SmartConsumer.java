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

import com.codeabovelab.dm.common.utils.Predicates;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Extension iface for sonsumer.
 * <p/>
 *  All methods of this iface must be 'default'.
 */
public interface SmartConsumer<T> extends Consumer<T> {

    /**
     * Cast consumer to smart consumer, or return wrapper, which simulate default values.
     * @param consumer
     * @param <T>
     * @return casted instance or default wrapper, never return null
     */
    @SuppressWarnings("unchecked")
    static <T> SmartConsumer<T> of(Consumer<T> consumer) {
        // first we try potentially wrapped consumer
        if(consumer instanceof SmartConsumer) {
            return (SmartConsumer<T>) consumer;
        }
        if(consumer instanceof WrappedConsumer) {
            Consumer<T> orig = WrappedConsumer.unwrap(consumer);
            // then unwrap ant try again
            if(orig != consumer && orig instanceof SmartConsumer) {
                return (SmartConsumer<T>) orig;
            }
        }
        // we assume that all methods of this iface must be 'default'
        return consumer::accept;
    }

    /**
     * Specify count of last history entries which is passed into consumer at subscription.<p/>
     * Some implementation may support only full count, but any implementation must consider
     * '0' - as value which is disable history.
     * @return {@link Integer#MAX_VALUE} for read full history (default), 0 for disable history
     */
    default int getHistoryCount() {
        return Integer.MAX_VALUE;
    }

    /**
     * Predicate which must return true for passed entries, and false for skipped. <p/>
     * @see Predicates#truePredicate()
     * @return never null, default value {@link Predicates#TRUE}
     */
    default Predicate<T> historyFilter() {
        return Predicates.truePredicate();
    }
}
