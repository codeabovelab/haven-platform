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

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility wrapper which present some property, contains default value, and also record first it changes.
 */
public final class Keeper<T> implements Supplier<T>, Consumer<T>, Changeable {
    private T value;
    private boolean changed;

    public Keeper() {
    }

    public Keeper(T value) {
        this.value = value;
    }

    @Override
    public boolean isChanged() {
        return changed;
    }

    @Override
    public void accept(T value) {
        if(!changed) {
            changed = true;
        }
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Keeper)) {
            return false;
        }

        Keeper<?> keeper = (Keeper<?>) o;

        if (changed != keeper.changed) {
            return false;
        }
        return !(value != null ? !value.equals(keeper.value) : keeper.value != null);

    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (changed ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Keeper{" +
          "value=" + value +
          ", changed=" + changed +
          '}';
    }
}
