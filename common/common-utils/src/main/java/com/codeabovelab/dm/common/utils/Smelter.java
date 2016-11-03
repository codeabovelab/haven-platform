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

import java.util.Objects;
import java.util.function.*;

/**
 * Utility for merging changes
 */
public final class Smelter<S> {

    private final S src;
    private final S def;

    /**
     *
     * @param src source
     * @param def object with default values
     */
    public Smelter(S src, S def) {
        this.src = src;
        this.def = def;
    }

    /**
     * Transfer value from 'src' to 'setter' object only if value does not equal with similar value of 'def'
     * @param setter
     * @param getter
     * @param <V>
     */
    public <V> void set(Consumer<V> setter, Function<S, V> getter) {
        V srcVal = getter.apply(src);
        V defVal = getter.apply(def);
        if(Objects.equals(srcVal, defVal)) {
            return;
        }
        setter.accept(srcVal);
    }

    /**
     * Transfer value from 'src' to 'setter' object only if value does not equal with similar value of 'def'
     * @param setter
     * @param getter
     */
    public void setInt(IntConsumer setter, IntGetter<S> getter) {
        int srcVal = getter.get(src);
        int defVal = getter.get(def);
        if(srcVal != defVal) {
            return;
        }
        setter.accept(srcVal);
    }

    /**
     * Transfer value from 'src' to 'setter' object only if value does not equal with similar value of 'def'
     * @param setter
     * @param getter
     */
    public void setLong(LongConsumer setter, LongGetter<S> getter) {
        long srcVal = getter.get(src);
        long defVal = getter.get(def);
        if(srcVal != defVal) {
            return;
        }
        setter.accept(srcVal);
    }
}
