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

package com.codeabovelab.dm.cluman.reconfig;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 */
class ReConfigurableBeanAdapter implements ReConfigurableAdapter {
    private final String name;
    private final Object bean;
    private final Class<?> type;
    private Supplier<Object> supplier;
    private Consumer<Object> consumer;

    ReConfigurableBeanAdapter(String name, Object bean) {
        this.name = name;
        this.bean = bean;
        this.type = this.bean.getClass();
        load();
    }

    private void load() {
        ReflectionUtils.doWithMethods(this.type, (m) -> {
            if(!m.isAnnotationPresent(ReConfigObject.class)) {
                return;
            }
            int pc = m.getParameterCount();
            final boolean returnVoid = Void.TYPE.equals(m.getReturnType());
            // note that in future we may pass ConfigReadContext into this methods, then we must to update below conditions
            boolean getter = pc == 0 && !returnVoid;
            boolean setter = pc == 1 && returnVoid;
            if(!getter && !setter) {
                return;
            }
            if(getter) {
                Assert.isNull(this.supplier, "Can not override existed getter with: " + m);
                makeAccessible(m);
                this.supplier = new GetterSupplier(m);
            } else {
                Assert.isNull(this.consumer, "Can not override existed consumer with: " + m);
                makeAccessible(m);
                this.consumer = new SetterConsumer(m);
            }
        });
        if(this.supplier != null && this.consumer != null) {
            return;
        }
        ReflectionUtils.doWithFields(this.type, (f) -> {
            if(!f.isAnnotationPresent(ReConfigObject.class)) {
                return;
            }
            if(Modifier.isStatic(f.getModifiers())) {
                throw new IllegalStateException("Static field '" + f + "' must not be annotated with "
                  + ReConfigObject.class );
            }
            if(this.supplier == null) {
                makeAccessible(f);
                this.supplier = new FieldSupplier(f);
            }
            if(this.consumer == null && !Modifier.isFinal(f.getModifiers())) {
                makeAccessible(f);
                this.consumer = new FieldConsumer(f);
            }
        });
        if(this.supplier == null || this.consumer == null) {
            throw new RuntimeException("Can not extract setter (" + this.consumer + ") and/or getter (" +
              this.supplier + ") from annotated bean: " + name);
        }
    }

    private void makeAccessible(AccessibleObject f) {
        if(!f.isAccessible()) {
            f.setAccessible(true);
        }
    }

    @Override
    public Object getConfig(ConfigWriteContext ctx) {
        return this.supplier.get();
    }

    @Override
    public void setConfig(ConfigReadContext ctx, Object o) {
        this.consumer.accept(o);
    }

    public String getName() {
        return name;
    }

    private final class GetterSupplier implements Supplier<Object> {
        private final Method m;

        GetterSupplier(Method m) {
            this.m = m;
        }

        @Override
        public Object get() {
            try {
                return m.invoke(bean);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return m.toString();
        }
    }

    private final class SetterConsumer implements Consumer<Object> {
        private final Method m;

        SetterConsumer(Method m) {
            this.m = m;
        }

        @Override
        public void accept(Object o) {
            try {
                m.invoke(bean, o);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return m.toString();
        }
    }

    private final class FieldSupplier implements Supplier<Object> {
        private final Field f;

        FieldSupplier(Field f) {
            this.f = f;
        }

        @Override
        public Object get() {
            try {
                return f.get(bean);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return f.toString();
        }
    }

    private final class FieldConsumer implements Consumer<Object> {
        private final Field f;

        FieldConsumer(Field f) {
            this.f = f;
        }

        @Override
        public void accept(Object o) {
            try {
                f.set(bean, o);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return f.toString();
        }
    }
}
