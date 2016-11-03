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

package com.codeabovelab.dm.cluman.model;

import com.codeabovelab.dm.common.utils.pojo.PojoUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class LogEvent extends Event implements WithSeverity, WithAction {

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static abstract class Builder<B extends Builder<B, T>, T extends LogEvent> extends Event.Builder<B, T> {
        private String user;
        private Severity severity;
        private String action;
        private String name;
        private String message;

        public B user(String user) {
            setUser(user);
            return thiz();
        }

        public B severity(Severity severity) {
            setSeverity(severity);
            return thiz();
        }

        public B action(String action) {
            setAction(action);
            return thiz();
        }

        public B name(String name) {
            setName(name);
            return thiz();
        }

        public B message(String message) {
            setMessage(message);
            return thiz();
        }
    }

    private final String kind = makeKind(getClass());

    protected static String makeKind(Class<?> clazz) {
        return PojoUtils.decapitalize(clazz.getSimpleName());
    }

    private final String user;
    private final Severity severity;
    private final String action;
    private final String name;
    private final String message;

    public LogEvent(Builder<?, ?> b) {
        super(b);
        this.user = b.user;
        this.severity = b.severity;
        this.action = b.action;
        this.name = b.name;
        this.message = b.message;
    }

    @Override
    public Severity getSeverity() {
        return this.severity;
    }

    /**
     * Kind of event, actually short class name, used for event filtering.
     * @return
     */
    public String getKind() {
        return this.kind;
    }
}
