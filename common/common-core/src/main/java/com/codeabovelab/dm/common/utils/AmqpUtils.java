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

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.util.concurrent.Callable;

/**
 * utils for rabbitmq
 */
public final class AmqpUtils {

    /**
     * Name of spring prfile.
     */
    public static final String PROFILE = "rabbitmq";

    public static final class ExchangeFactory implements Callable<Exchange> {

        private String exchangeType = ExchangeTypes.FANOUT;
        private String exchangeName;
        private boolean durable = false;
        private boolean autoDelete = false;

        /**
         * Default value is {@link org.springframework.amqp.core.ExchangeTypes#TOPIC }.
         * @return
         */
        public String getExchangeType() {
            return exchangeType;
        }

        /**
         * Default value is {@link org.springframework.amqp.core.ExchangeTypes#TOPIC }.
         * @param exchangeType
         */
        public void setExchangeType(String exchangeType) {
            this.exchangeType = exchangeType;
        }

        public String getExchangeName() {
            return exchangeName;
        }

        public void setExchangeName(String exchangeName) {
            this.exchangeName = exchangeName;
        }

        /**
         * Default value is false.
         * @return
         */
        public boolean isDurable() {
            return durable;
        }

        /**
         * Default value is false.
         * @param durable
         */
        public void setDurable(boolean durable) {
            this.durable = durable;
        }

        /**
         * Default value is false.
         * @return
         */
        public boolean isAutoDelete() {
            return autoDelete;
        }

        /**
         * Default value is false.
         * @param autoDelete
         */
        public void setAutoDelete(boolean autoDelete) {
            this.autoDelete = autoDelete;
        }

        @Override
        public Exchange call() {
            return createExchange(this.exchangeType, this.exchangeName, this.durable, this.autoDelete);
        }
    }

    private AmqpUtils() {
    }

    /**
     * decalre specified excahnge
     * @param connectionFactory
     * @param exchangeType
     * @param exchangeName
     * @param durable
     * @param autoDelete
     */
    public static void declareExchange(ConnectionFactory connectionFactory,
                                       String exchangeType,
                                       String exchangeName,
                                       boolean durable,
                                       boolean autoDelete) {
        Exchange x = createExchange(exchangeType, exchangeName, durable, autoDelete);
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.declareExchange(x);
    }

    /**
     * create specified exchange
     * @param exchangeType
     * @param exchangeName
     * @param durable
     * @param autoDelete
     * @return
     */
    private static Exchange createExchange(String exchangeType, String exchangeName, boolean durable, boolean autoDelete) {
        switch (exchangeType) {
            case ExchangeTypes.TOPIC:
                return new TopicExchange(exchangeName, durable, autoDelete);
            case ExchangeTypes.DIRECT:
                return new DirectExchange(exchangeName, durable, autoDelete);
            case ExchangeTypes.HEADERS:
                return new HeadersExchange(exchangeType, durable, autoDelete);
            default:
                return new FanoutExchange(exchangeName, durable, autoDelete);
        }
    }
}
