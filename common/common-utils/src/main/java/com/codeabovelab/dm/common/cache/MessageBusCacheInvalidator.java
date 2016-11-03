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

package com.codeabovelab.dm.common.cache;

import com.codeabovelab.dm.common.mb.MessageBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * Invalidator which reset cache on events in message bus
 */
@Component
public class MessageBusCacheInvalidator implements CacheInvalidator {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Name of bus bean
     */
    public final static String BUS_KEY = "bus";
    /**
     * SpEL expression which return id of cache key which must been invalidated. If absent then all cache will be invalidated.
     */
    public final static String EXP_KEY = "expression";

    private final BeanFactory beanFactory;
    private final SpelExpressionParser parser;
    private final Map<Cache, CacheInvalidatorAgent> agents = new WeakHashMap<>();

    @Autowired
    public MessageBusCacheInvalidator(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        this.parser = new SpelExpressionParser();
    }

    @Override
    public void init(Cache cache, Map<String, String> args) {
        CacheInvalidatorAgent cia;
        synchronized (agents) {
            cia = agents.get(cache);
        }
        if(cia != null) {
            return;
        }

        String busName = args.get(BUS_KEY);
        Assert.hasText(busName, "bus name is null or empty");

        String exprStr = args.get(EXP_KEY);
        SpelExpression expr = null;
        if(StringUtils.hasText(exprStr)) {
            expr = parser.parseRaw(exprStr);
        }

        cia = new CacheInvalidatorAgent(cache, expr);
        CacheInvalidatorAgent old;
        synchronized (agents) {
            old = agents.putIfAbsent(cache, cia);
        }
        if(old == null) {
            @SuppressWarnings("unchecked")
            MessageBus<Object> bus = beanFactory.getBean(busName, MessageBus.class);
            bus.subscribe(cia);
        }
    }

    private class CacheInvalidatorAgent implements Consumer<Object> {
        private final Cache cache;
        private final SpelExpression expr;

        public CacheInvalidatorAgent(Cache cache, SpelExpression expr) {
            this.cache = cache;
            this.expr = expr;
        }

        @Override
        public void accept(Object o) {
            Object key = null;
            if(this.expr != null) {
                try {
                    key = this.expr.getValue(o);
                } catch (Exception e) {
                    log.error("Error on '{}' event and expression '{}' in cache invalidator.", o, e);
                }
            }
            if(key != null) {
                this.cache.evict(key);
            } else {
                this.cache.clear();
            }
        }
    }
}
