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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 */
public final class Sugar {

    public static <S> void setIfNotNull(Consumer<S> consumer, S val) {
        if(val == null) {
            return;
        }
        consumer.accept(val);
    }

    /**
     * Put value into consumer only if value source {@link Changeable#isChanged()}  is changed}.
     * @see Keeper
     * @param consumer
     * @param valueSource
     * @param <S>
     * @param <G>
     */
    public static <S, G extends Supplier<S> & Changeable> void setIfChanged(Consumer<S> consumer, G valueSource) {
        if(valueSource == null || !valueSource.isChanged()) {
            return;
        }
        consumer.accept(valueSource.get());
    }

    public static void setStringIfNotNull(Consumer<String> consumer, Object s) {
        if(s == null) {
            return;
        }
        String str = s.toString();
        consumer.accept(str);
    }

    /**
     * Sequence apply getters and test its values for Objects.equal.
     * @param left
     * @param right
     * @param getters
     * @param <T>
     * @return true if each value of left is equals with right value
     */
    @SuppressWarnings("unchecked")
    public static <T> boolean equals(T left, T right, Function<T, ?>... getters) {
        if(left == right) {
            return true;
        }
        if(left == null || right == null) {
            return false;
        }
        for(Function<T, ?> getter: getters) {
            Object lv = getter.apply(left);
            Object rv = getter.apply(right);
            if(!Objects.equals(lv, rv)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Make immutable collection, make empty collection for nulls.
     * @param src
     * @param <I>
     * @return
     */
    public static <I> List<I> immutableList(Collection<? extends I> src) {
        return src == null ? ImmutableList.of() : ImmutableList.copyOf(src);
    }

    /**
     * Make immutable collection, make empty collection for nulls.
     * @param src
     * @param <I>
     * @return
     */
    public static <I> Set<I> immutableSet(Collection<? extends I> src) {
        return src == null ? ImmutableSet.of() : ImmutableSet.copyOf(src);
    }

    /**
     * Make immutable collection, make empty collection for nulls.
     * @param src
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Map<K, V> immutableMap(Map<? extends K, ? extends V> src) {
        return src == null ? ImmutableMap.of() : ImmutableMap.copyOf(src);
    }
}
