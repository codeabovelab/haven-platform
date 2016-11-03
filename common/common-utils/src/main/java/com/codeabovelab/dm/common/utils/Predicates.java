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

package com.codeabovelab.dm.common.utils;

import java.util.function.Predicate;

/**
 */
public final class Predicates {

    public static final Predicate<Object> TRUE = o -> true;
    public static final Predicate<Object> FALSE = o -> false;

    private Predicates() {
    }

    /**
     *
     * @param <T>
     * @return {@link #TRUE}
     */
    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> truePredicate() {
        return (Predicate<T>) TRUE;
    }

    /**
     *
     * @param <T>
     * @return {@link #FALSE}
     */
    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> falsePredicate() {
        return (Predicate<T>) FALSE;
    }
}
