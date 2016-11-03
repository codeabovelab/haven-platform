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

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Threadsafe tool for lazy initialization.
 * Prevent annoying creation of double cheking pattern and other boilerplate code;
 */
public final class LazyInitializer<T> {
    private static final Object NULL = new Object();
    private volatile T instance;
    private final Lock lock = new ReentrantLock();
    private final Callable<T> factory;

    public LazyInitializer(Callable<T> factory) {
        this.factory = factory;
    }

    /**
     * Return cached instance or create it by {@link java.util.concurrent.Callable#call()}, if creation is fail
     * then throw runtime exception.
     * Note that if factory return null then result will not be cached.
     * @return
     */
    public T get() {
        if(this.instance == null) {
            while(lock.tryLock()) {
                try {
                    if(this.instance == null) {
                        return this.instance = factory.call();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    lock.unlock();
                }
            }
        }
        return instance;
    }
}
