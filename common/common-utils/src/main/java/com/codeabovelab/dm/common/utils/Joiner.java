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

import java.util.Map;
import java.util.Set;

/**
 * Utility which join items into string with specified delimiter. Optionally it can do conversion see {@link #converter(Converter)}
 */
public final class Joiner<T> {

    public interface Converter<T> {
        void convert(StringBuilder sb, T item);
    }

    private final StringBuilder sb = new StringBuilder();
    private boolean first = true;
    private final String delimiter;
    private Converter<Object> converter;

    private Joiner(String delimiter) {
        this.delimiter = delimiter;
    }

    public static Joiner<Object> on(String delimiter) {
        return new Joiner<>(delimiter);
    }

    /**
     * Change current converter to new.
     * @param converter new converter
     * @param <N> type of new converter
     * @return this
     */
    @SuppressWarnings("unchecked")
    public <N> Joiner<N> converter(Converter<N> converter) {
        this.converter = (Converter<Object>) converter;
        return (Joiner<N>) this;
    }

    public Joiner<T> join(Iterable<? extends T> iterable) {
        iterable.forEach(this::item);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <N extends Map.Entry<K, V>, K, V> Joiner<N> join(Map<K, V> iterable) {
        Joiner<N> nj = (Joiner<N>) this;
        ((Set<N>)iterable.entrySet()).forEach(nj::item);
        return nj;
    }

    /**
     * Append string value to current internal buffer without delimiter.
     * @param str
     * @return
     */
    public Joiner<T> append(String str) {
        sb.append(str);
        return this;
    }

    /**
     * Append value to current internal buffer without delimiter.
     * @param c
     * @return
     */
    public Joiner<T> append(char c) {
        sb.append(c);
        return this;
    }

    public Joiner<T> item(T item) {
        if(first) {
            first = false;
        } else {
            sb.append(delimiter);
        }
        if(converter != null) {
            converter.convert(sb, item);
        } else {
            sb.append(item);
        }
        return this;
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
