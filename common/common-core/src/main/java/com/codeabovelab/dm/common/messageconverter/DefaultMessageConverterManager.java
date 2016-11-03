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

package com.codeabovelab.dm.common.messageconverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides ability to convert any version of pojo to a latest or any requested version.
 * It composes chains of MessageConverter's. Used for up casting of commands and down casting of results.
 */
@Component
public class DefaultMessageConverterManager implements MessageConverterManager {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMessageConverterManager.class);
    private final Map<Class<?>, List<MessageConverter<?, ?>>> messageConverterChainByClass = new HashMap<>();

    @Autowired(required = false)
    public DefaultMessageConverterManager(final List<MessageConverter<?, ?>> messageConverters) {
        final Map<Class<?>, MessageConverter<?, ?>> messageConverterMap = new HashMap<>();
        //Generate a map of converters indexed by "from" class
        for (final MessageConverter<?, ?> messageConverter : messageConverters) {
            final Class<?> fromClass = messageConverter.getFrom();
            if (messageConverterMap.containsKey(fromClass)) {
                throw new IllegalArgumentException (String.format(
                        "Failed to add message converter %s, message convert from %s already defined by %s", messageConverter
                                .getClass().getName(), fromClass, messageConverterMap.get(fromClass).getClass().getName()));
            }
            messageConverterMap.put(fromClass, messageConverter);
        }
        //Generate a chains of converters
        for (MessageConverter<?, ?> converter : messageConverterMap.values()) {
            final List<MessageConverter<?, ?>> messageConverterChain = new ArrayList<>();
            fillMessageConverterChain(converter, messageConverterChain, messageConverterMap);
            LOG.debug("Message converter chain registered for {}", converter.getFrom().getClass().getSimpleName());
            messageConverterChainByClass.put(converter.getFrom(), messageConverterChain);
        }
    }

    /**
     * Convert a message till the end of chain
     * @param message to convert
     * @return converted message
     */
    @Override
    public <T> T convertToNewest(Object message, Class<T> resultType) {
        final List<MessageConverter<?, ?>> messageConverters = messageConverterChainByClass.get(message.getClass());
        if (messageConverters != null) {
            for (MessageConverter<?, ?> messageConverter : messageConverters) {
                if (!messageConverter.getFrom().isAssignableFrom(message.getClass())) {
                    throw new IllegalStateException("Missing message converter to convert from " + message.getClass().getName()
                            + " Instead, found converter for class " + messageConverter.getFrom().getName());
                }
                message = convert(messageConverter, message);
            }
        }
        LOG.debug("Message of {} converted to {}", message.getClass().getSimpleName(), resultType.getSimpleName());
        return resultType.cast(message);
    }

    /**
     * Convert a message to a specified version
     * @param message to convert
     * @return converted message
     * @throws RuntimeException if a chain of converter for conversion does not exists
     */
    @Override
    public <T> T convert(final Object message, final Class<T> toType) {
        if (message.getClass().equals(toType)) {
            return toType.cast(message);
        }

        Object convertedMessage = message;
        final List<MessageConverter<?, ?>> messageConverters = messageConverterChainByClass.get(message.getClass());
        if (messageConverters != null) {
            for (MessageConverter<?, ?> messageConverter : messageConverters) {
                convertedMessage = convert(messageConverter, convertedMessage);
                if (messageConverter.getTo().equals(toType)) {
                    LOG.debug("Message of {} converted to {}", message.getClass().getSimpleName(), toType.getSimpleName());
                    return toType.cast(convertedMessage);
                }
            }
        }
        throw new IllegalArgumentException(String.format("Can't convert %s to %s", message.getClass().getName(), toType.getName()));
    }

    /**
     * Helper method for recursive construction of converter chain
     * @param converter
     * @param messageConverters
     * @param messageConverterMap
     */
    private void fillMessageConverterChain(final MessageConverter<?, ?> converter,
            final List<MessageConverter<?, ?>> messageConverters, final Map<Class<?>, MessageConverter<?, ?>> messageConverterMap) {
        messageConverters.add(converter);
        final MessageConverter<?, ?> nextConverter = messageConverterMap.get(converter.getTo());
        if (nextConverter != null) {
            fillMessageConverterChain(nextConverter, messageConverters, messageConverterMap);
        }
    }

    @SuppressWarnings("unchecked")
    private Object convert(MessageConverter<?, ?> messageConverter, Object message) {
        return ((MessageConverter<Object, Object>) messageConverter).convert(message);
    }
}
