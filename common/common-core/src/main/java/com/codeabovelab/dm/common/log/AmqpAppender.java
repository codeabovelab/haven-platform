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

package com.codeabovelab.dm.common.log;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.pattern.TargetLengthBasedClassNameAbbreviator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.codeabovelab.dm.common.json.JacksonUtils;
import com.codeabovelab.dm.common.utils.AmqpUtils;
import com.codeabovelab.dm.common.utils.AppInfo;
import com.codeabovelab.dm.common.utils.MessageHeaders;
import com.codeabovelab.dm.common.utils.OSUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * a appender to amqp based on {@link org.springframework.amqp.rabbit.logback.AmqpAppender }.
 * This implementation can use spring configuration of rabbit connection factory
 */
public class AmqpAppender extends AppenderBase<DeferredProcessingAware> {

    /**
     * Key name for the application id in the message properties.
     */
    public static final String APPLICATION_ID = "applicationId";

    /**
     * Key name for the logger category name in the message properties
     */
    public static final String CATEGORY_NAME = "categoryName";

    /**
     * Key name for the logger level name in the message properties
     */
    public static final String CATEGORY_LEVEL = "level";
    public static final String KEY_USER = "user";
    public static final String REQUEST_UUID = "x-request-uuid";
    public static final String REQUEST_DESCRIPTION = "request-description";

    //possibly we need system wide object mapper
    private final ObjectMapper objectMapper = JacksonUtils.objectMapperBuilder();

    /**
     * Name of the exchange to publish log events to.
     */
    private String exchangeName = "logs";

    private boolean fillLocation;

    /**
     * Range of captured accessLogs from/to
     */
    private Integer resultCodeFrom = 0;

    /**
     * Range of captured accessLogs from/to
     */
    private Integer resultCodeTo = Integer.MAX_VALUE;

    /**
     * Type of the exchange to publish log events to.
     */
    private String exchangeType = "fanout";

    private final PatternLayout locationLayout = new PatternLayout();

    private final ch.qos.logback.access.PatternLayout accessLayout = new ch.qos.logback.access.PatternLayout();

    {
        this.locationLayout.setPattern("%nopex%class|%method|%line");
        this.accessLayout.setPattern("host: %h; requestURL: \"%r\"; statusCode: %s; %b");
    }

    /**
     * Queue for logging entries.
     * We must store events against to {@link #doInit(org.springframework.context.ApplicationContext)} will be invoked.
     * But in some cases (tests or rabbitMQ fail) this method will not be invoked, and buffer will
     * start growing, therefore we use bounded queue and must remove last element if queue is full.
     * we use LBQ for not blocking producer thread (ABQ has one monitor)
     */
    private final LinkedBlockingDeque<Event> events = new LinkedBlockingDeque<>(1024);

    /**
     * The pool of senders.
     */
    private ExecutorService senderPool = null;

    /**
     * How many senders to use at once. Use more senders if you have lots of log output going through this appender.
     */
    private int senderPoolSize = 2;

    /**
     * How many times to retry sending a message if the broker is unavailable or there is some other error.
     */
    private int maxSenderRetries = 30;

    /**
     * Retries are delayed like: N ^ log(N), where N is the retry number.
     */
    private final Timer retryTimer = new Timer("log-event-retry-delay", true);

    /**
     * RabbitMQ ConnectionFactory.
     */
    private ConnectionFactory connectionFactory;

    /**
     * Default content-encoding of log messages.
     */
    private String contentEncoding = null;

    /**
     * Whether or not to try and declare the configured exchange when this appender starts.
     */
    private boolean declareExchange = false;

    /**
     * charset to use when converting String to byte[], default null (system default charset used).
     * If the charset is unsupported on the current platform, we fall back to using
     * the system charset.
     */
    private String charset;

    public Integer getResultCodeFrom() {
        return resultCodeFrom;
    }

    public void setResultCodeFrom(Integer resultCodeFrom) {
        this.resultCodeFrom = resultCodeFrom;
    }

    public Integer getResultCodeTo() {
        return resultCodeTo;
    }

    public void setResultCodeTo(Integer resultCodeTo) {
        this.resultCodeTo = resultCodeTo;
    }

    private boolean durable = true;

    private MessageDeliveryMode deliveryMode = MessageDeliveryMode.PERSISTENT;

    private boolean autoDelete = false;

    private TargetLengthBasedClassNameAbbreviator abbreviator;
    private String applicationName;
    private String hostName;
    private String applicationVersion;

    public AmqpAppender() {
        //self register for initialisation wait
        register(this);
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public String getExchangeType() {
        return exchangeType;
    }

    public void setExchangeType(String exchangeType) {
        this.exchangeType = exchangeType;
    }

    public boolean isDeclareExchange() {
        return declareExchange;
    }

    public void setDeclareExchange(boolean declareExchange) {
        this.declareExchange = declareExchange;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    public int getSenderPoolSize() {
        return senderPoolSize;
    }

    public void setSenderPoolSize(int senderPoolSize) {
        this.senderPoolSize = senderPoolSize;
    }

    public int getMaxSenderRetries() {
        return maxSenderRetries;
    }

    public void setMaxSenderRetries(int maxSenderRetries) {
        this.maxSenderRetries = maxSenderRetries;
    }

    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean durable) {
        this.durable = durable;
    }

    public String getDeliveryMode() {
        return this.deliveryMode.toString();
    }

    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = MessageDeliveryMode.valueOf(deliveryMode);
    }

    public boolean isAutoDelete() {
        return autoDelete;
    }

    public void setAutoDelete(boolean autoDelete) {
        this.autoDelete = autoDelete;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public void setAbbreviation(int len) {
        this.abbreviator = new TargetLengthBasedClassNameAbbreviator(len);
    }

    public boolean getFillLocation() {
        return fillLocation;
    }

    public void setFillLocation(boolean fillLocation) {
        this.fillLocation = fillLocation;
    }

    @Override
    public void start() {
        super.start();
        this.locationLayout.setContext(getContext());
        this.locationLayout.start();
        this.senderPool = Executors.newCachedThreadPool();
        init();
    }

    @Override
    public void stop() {
        super.stop();
        if (null != this.senderPool) {
            this.senderPool.shutdownNow();
            this.senderPool = null;
        }
        this.retryTimer.cancel();
    }

    protected void append(DeferredProcessingAware event) {

        if (event instanceof ILoggingEvent) {
            sendLoggingEvent((ILoggingEvent) event);
        } else if (event instanceof IAccessEvent) {
            sendAccessEvent((IAccessEvent) event);
        } else {
            // ignore
        }
    }

    public void sendLoggingEvent(ILoggingEvent logEvent) {

        // we can also serialize mdc to JSON
        Map<String, String> props = Collections.unmodifiableMap(logEvent.getMDCPropertyMap());

        LogstashEntity entity = new LogstashEntity();

        IThrowableProxy throwableProxy = logEvent.getThrowableProxy();
        if (throwableProxy != null) {
            entity.setThrowable(ThrowableProxyUtil.asString(throwableProxy));
        }

        entity.setThreadName(logEvent.getThreadName());

        String loggerName = logEvent.getLoggerName();
        if (abbreviator != null) {
            entity.setLoggerName(abbreviator.abbreviate(loggerName));
        } else {
            entity.setLoggerName(loggerName);
        }

        Level level = logEvent.getLevel();
        entity.setLevel(level.toString());
        entity.setDate(new Date(logEvent.getTimeStamp()));

        entity.setHost(hostName);
        entity.setUser(props.get(KEY_USER));

        entity.setRequestUID(props.get(REQUEST_UUID));
        entity.setDescription(props.get(REQUEST_DESCRIPTION));

        entity.setApplicationVersion(applicationVersion);

        //there we can modify message text of entry if need
        entity.setMessage(logEvent.getFormattedMessage());

        MessageProperties amqpProps = fillAmqpProps();
        amqpProps.setHeader(CATEGORY_NAME, loggerName);
        amqpProps.setHeader(CATEGORY_LEVEL, level.toString());
        amqpProps.setTimestamp(new Date(logEvent.getTimeStamp()));
        amqpProps.setHeader(MessageHeaders.HOST, entity.getHost());
        amqpProps.setMessageId(entity.getRequestUID());
        // Set applicationId, if we're using one
        if (null != applicationName) {
            amqpProps.setAppId(applicationName);
            entity.setApplication(applicationName);
            logEvent.getLoggerContextVO().getPropertyMap().put(APPLICATION_ID, applicationName);
        }

        if (fillLocation) {
            String[] location = locationLayout.doLayout(logEvent).split("\\|");
            if (!"?".equals(location[0])) {
                amqpProps.setHeader("location", String.format("%s.%s()[%s]", location[0], location[1], location[2]));
            }
        }
        sendMessage(entity, amqpProps);
    }

    public void sendAccessEvent(IAccessEvent logEvent) {

        if (logEvent.getStatusCode() < resultCodeFrom ||
                logEvent.getStatusCode() > resultCodeTo) {
            //skip log entry
            return;
        }
        AccessLogRecord entity = new AccessLogRecord();
        entity.setHost(hostName);

        String text = accessLayout.doLayout(logEvent);

        entity.setApplicationVersion(applicationVersion);

        //there we can modify message text of entry if need
        entity.setMessage(text);
        entity.setElapsedTime(logEvent.getElapsedTime());
        entity.setMethod(logEvent.getMethod());
        entity.setStatusCode(logEvent.getStatusCode());
        entity.setProtocol(logEvent.getProtocol());
        entity.setRemoteAddr(logEvent.getRemoteAddr());
        entity.setRemoteHost(logEvent.getRemoteHost());
        entity.setRemoteUser(logEvent.getRemoteUser());
        entity.setRequestContent(logEvent.getRequestContent());
        entity.setRequestHeaderMap(logEvent.getRequestHeaderMap());
        entity.setRequestParameterMap(logEvent.getRequestParameterMap());
        entity.setRequestURI(logEvent.getRequestURI());
        entity.setRequestURL(logEvent.getRequestURL());
        entity.setResponseContent(logEvent.getResponseContent());
        entity.setResponseHeaderMap(logEvent.getResponseHeaderMap());
        entity.setServerName(logEvent.getServerName());

        String uidFromRequest = logEvent.getRequestHeaderMap().get(REQUEST_UUID);

        entity.setRequestUID(uidFromRequest);
        entity.setDate(new Date(logEvent.getTimeStamp()));

        MessageProperties amqpProps = fillAmqpProps();
        amqpProps.setTimestamp(new Date(logEvent.getTimeStamp()));
        amqpProps.setHeader(MessageHeaders.HOST, entity.getHost());
        amqpProps.setMessageId(entity.getRequestUID());
        // Set applicationId, if we're using one
        if (null != applicationName) {
            amqpProps.setAppId(applicationName);
            entity.setApplication(applicationName);
        }
        // Send a message
        sendMessage(entity, amqpProps);
    }

    private MessageProperties fillAmqpProps() {
        MessageProperties amqpProps = new MessageProperties();
        amqpProps.setDeliveryMode(deliveryMode);
        amqpProps.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        if (contentEncoding != null) {
            amqpProps.setContentEncoding(contentEncoding);
        }
        return amqpProps;
    }

    private void sendMessage(BasicLogRecord entity, MessageProperties amqpProps) {
        // because queue is bounded, we need to remove head manually if queue is full
        while (!this.events.offer(new Event(amqpProps, entity))) {
            this.events.poll();
        }
    }

    private void doInit(ApplicationContext applicationContext) {
        Environment environment = applicationContext.getEnvironment();
        if(!environment.acceptsProfiles(AmqpUtils.PROFILE)) {
            return;
        }
        this.connectionFactory = applicationContext.getBean(ConnectionFactory.class);
        this.applicationName = AppInfo.getApplicationName();
        this.hostName = OSUtils.getHostName();
        this.applicationVersion = AppInfo.getApplicationVersion();
        init();
    }

    private void init() {
        if (!started || connectionFactory == null) {
            return;
        }
        // declare exchange if need
        if (this.declareExchange) {
            AmqpUtils.declareExchange(connectionFactory, exchangeType, exchangeName, durable, autoDelete);
        }

        //push sender workers to pool
        for (int i = 0; i < this.senderPoolSize; i++) {
            this.senderPool.submit(new EventSender());
        }
    }

    private static void register(AmqpAppender thiz) {
        final WeakReference<AmqpAppender> reference = new WeakReference<>(thiz);
        LogConfiguration.DEFERRED_RESULT.addCallback(new ListenableFutureCallback<ApplicationContext>() {
            @Override
            public void onFailure(Throwable throwable) { }

            @Override
            public void onSuccess(ApplicationContext applicationContext) {
                AmqpAppender amqpAppender = reference.get();
                if (amqpAppender != null) {
                    amqpAppender.doInit(applicationContext);
                }
            }
        });
    }

    private void processException(Exception t) {
        // we cannot use log because it in log internally process
        t.printStackTrace();
    }

    private String doLayout(BasicLogRecord entity) {
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            processException(e);
            return "{'message':'Can not serialize logEvent to json, see log.'}";
        }
    }

    /**
     * Helper class to actually send LoggingEvents asynchronously.
     */
    protected class EventSender implements Runnable {

        private RabbitTemplate rabbitTemplate;

        @Override
        public void run() {
            try {
                rabbitTemplate = new RabbitTemplate(connectionFactory);
                while (!Thread.currentThread().isInterrupted()) {
                    final Event event = events.take();
                    try {
                        Message message;
                        String msgBody = doLayout(event.getEntity());
                        MessageProperties amqpProps = event.getMessageProperties();
                        try {
                            message = new Message(msgBody.getBytes(AmqpAppender.this.charset), amqpProps);
                        } catch (UnsupportedEncodingException e) {
                            message = new Message(msgBody.getBytes(), amqpProps);
                        }
                        final String routingKey = applicationName;
                        rabbitTemplate.send(exchangeName, routingKey, message);
                    } catch (BeanCreationNotAllowedException e) {
                        // this exception usually appeared on application closing
                    } catch (AmqpException e) {
                        int retries = event.incrementRetries();
                        if (retries < maxSenderRetries) {
                            // Schedule a retry based on the number of times I've tried to re-send this
                            retryTimer.schedule(new ReplayEvent(event), getScheduleTime(retries));
                        } else {
                            addError("Could not send log message {" + event.getEntity() + "} after " + maxSenderRetries + " retries", e);
                        }
                    }

                }
            } catch (InterruptedException e) {
                // on interrupt we must silently exit
                Thread.currentThread().interrupt();
            } catch (Exception t) {
                processException(t);
            }
        }

        private long getScheduleTime(int retries) {
            // basic behaviour use pow for calculation of time, but we don't need more than one hour
            return Math.max(TimeUnit.HOURS.toMillis(1), (long) (Math.pow(retries, Math.log(retries)) * 1000));
        }
    }

    private final class ReplayEvent extends TimerTask {

        private final Event event;

        public ReplayEvent(Event event) {
            this.event = event;
        }

        @Override
        public void run() {
            events.addFirst(event);
        }
    }

    /**
     * Small helper interface which encapsulate a Message and the number of retries.
     */
    private final static class Event {

        private final MessageProperties messageProperties;
        private final BasicLogRecord entity;
        private final AtomicInteger retries = new AtomicInteger(0);

        public Event(final MessageProperties amqpProps, final BasicLogRecord entity) {
            this.messageProperties = amqpProps;
            this.entity = entity;
        }

        public MessageProperties getMessageProperties() {
            return messageProperties;
        }

        public BasicLogRecord getEntity() {
            return entity;
        }

        /**
         * increase retries counter
         *
         * @return
         */
        public int incrementRetries() {
            return retries.incrementAndGet();
        }
    }


}
