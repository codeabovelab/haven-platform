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

package com.codeabovelab.dm.cluman.mail;

import com.codeabovelab.dm.common.kv.KeyValueStorage;
import com.codeabovelab.dm.common.kv.KvUtils;
import com.codeabovelab.dm.common.kv.WriteOptions;
import com.codeabovelab.dm.common.kv.mapping.KvClassMapper;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.cluman.model.Severity;
import com.codeabovelab.dm.cluman.model.WithSeverity;
import com.codeabovelab.dm.cluman.objprinter.ObjectPrinterFactory;
import com.codeabovelab.dm.cluman.reconfig.ReConfigObject;
import com.codeabovelab.dm.cluman.reconfig.ReConfigurable;
import com.codeabovelab.dm.cluman.ui.HttpException;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.codeabovelab.dm.common.mb.SmartConsumer;
import com.codeabovelab.dm.common.mb.Subscriptions;
import com.codeabovelab.dm.mail.dto.*;
import com.codeabovelab.dm.mail.service.SendMailWithTemplateService;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@ReConfigurable
@Component
public class MailNotificationsService {

    private static final String DEFAULT_TEMPLATE = "res:eventAlert";
    private final Map<String, MailSubscription> subs = new ConcurrentHashMap<>();
    private final Map<String, Source<?>> sources;
    private final SendMailWithTemplateService sendMailService;
    private final String from;

    private class Source<T> {
        private final String id;
        private final Subscriptions<T> subscriptions;
        private final Consumer<T> consumer;

        public Source(String id, Subscriptions<T> subscriptions) {
            this.id = id;
            this.subscriptions = subscriptions;
            this.consumer = new SmartConsumer<T>() {
                @Override
                public void accept(T event) {
                    MailNotificationsService.this.onEvent(Source.this.id, event);
                }

                @Override
                public int getHistoryCount() {
                    //it disable history
                    return 0;
                }
            };
        }

        public String getId() {
            return id;
        }

        void init() {
            this.subscriptions.subscribe(this.consumer);
        }
    }

    private final ObjectPrinterFactory objectPrinterFactory;
    private final KvMapperFactory kvMapperFactory;
    private KvClassMapper<MailSubscription.Builder> classMapper;

    @Autowired
    public MailNotificationsService(MailConfiguration.MailProperties props,
                                    KvMapperFactory kvMapperFactory,
                                    ObjectPrinterFactory objectPrinterFactory,
                                    SendMailWithTemplateService sendMailService,
                                    Map<String, Subscriptions<?>> sources) {
        this.objectPrinterFactory = objectPrinterFactory;
        this.kvMapperFactory = kvMapperFactory;
        this.from = props.getFrom();
        this.sendMailService = sendMailService;
        ImmutableMap.Builder<String, Source<?>> b = ImmutableMap.builder();
        sources.forEach((k, v) -> b.put(k, new Source<>(k, v)));
        this.sources = b.build();
    }

    @PostConstruct
    public void init() {
        this.sources.values().forEach(Source::init);

        KeyValueStorage storage = kvMapperFactory.getStorage();
        final String prefix = KvUtils.join(storage.getPrefix(), "/mail-notifications");
        this.classMapper = kvMapperFactory.createClassMapper(prefix, MailSubscription.Builder.class);

        try {
            storage.setdir(prefix, WriteOptions.builder().failIfAbsent(false).failIfExists(false).build());

            List<String> subscrNames = classMapper.list();
            for(String subscrName: subscrNames) {
                MailSubscription.Builder subscr = classMapper.load(subscrName);
                registerInternal(subscr.build());
            }
        } catch (Exception e) {
            log.error("Can not load mail notifications subscriptions", e);
        }
    }

    private void onEvent(String eventSource, Object ev) {
        if(!(ev instanceof WithSeverity)) {
            return;
        }
        Severity severity = ((WithSeverity) ev).getSeverity();
        MailSubscription sub = this.subs.get(eventSource);
        if(sub == null ||
          //compare that subs severity is greater and we need skip this event
          sub.getSeverity().compareTo(severity) > 0) {
            return;
        }
        //TODO cache, and also disable processing on errors, for prevent hangs
        String templateUri = sub.getTemplate();
        if(!StringUtils.hasText(templateUri)) {
            templateUri = DEFAULT_TEMPLATE;
        }
        List<String> emails = sub.getEmailRecipients();
        Object var = objectPrinterFactory.printer(ev);
        for(String email: emails) {
            MailSourceImpl.Builder msb = MailSourceImpl.builder();
            msb.templateUri(templateUri)
              .addVariable("to", email)
              .addVariable("event", ev)
              .addVariable("eventText", var)
              .addVariable("eventSource", eventSource)
              .addVariable("severity", severity)
              .addVariable("from", from);
            sendMailService.send(msb.build(), msr -> {
                if(msr.getStatus() == MailStatus.UNKNOWN_FAIL) {
                    log.error("Sent notification to {} is failed with error {} ", email, msr.getError());
                } else {
                    log.info("Sent notification to {} with result {} ", email, msr);
                }
            });
        }
    }


    public Collection<MailSubscription> list() {
        return subs.values();
    }

    public MailSubscription get(String eventSource) {
        return subs.get(eventSource);
    }

    public void put(MailSubscription newSub) {
        MailSubscription registered = registerInternal(newSub);
        if(registered != newSub) {
            throw new HttpException(HttpStatus.CONFLICT, newSub.getEventSource() + " already has mail subscription.");
        }
        persist(newSub);
    }

    private void persist(MailSubscription newSub) {
        //persist
        classMapper.save(newSub.getEventSource(), MailSubscription.builder().from(newSub));
    }

    /**
     * This method does not allow rewrite existed subscription, and return registered value.
     * @param newSub
     * @return
     */
    private MailSubscription registerInternal(MailSubscription newSub) {
        String eventSource = newSub.getEventSource();
        ExtendedAssert.matchId(eventSource, "event source");
        MailSubscription oldSub = subs.putIfAbsent(eventSource, newSub);
        if(oldSub == null) {
            onUpdate(newSub);
            return newSub;
        }
        return oldSub;
    }

    private void onUpdate(MailSubscription newSub) {
        log.info("register or update subscription: {}", newSub);
    }

    public void addSubscribers(String eventSource, Collection<String> emails) {
        if(CollectionUtils.isEmpty(emails)) {
            return;
        }
        MailSubscription sub = subs.computeIfPresent(eventSource, (k, old) -> {
            MailSubscription.Builder b = MailSubscription.builder();
            b.from(old);
            b.getEmailRecipients().addAll(emails);
            return b.build();
        });
        checkNotNull(eventSource, sub);
        onUpdate(sub);
        persist(sub);
    }

    public void removeSubscribers(String eventSource, Collection<String> emails) {
        if(CollectionUtils.isEmpty(emails)) {
            return;
        }
        MailSubscription sub = subs.computeIfPresent(eventSource, (k, old) -> {
            MailSubscription.Builder b = MailSubscription.builder();
            b.from(old);
            b.getEmailRecipients().removeAll(emails);
            return b.build();
        });
        checkNotNull(eventSource, sub);
        onUpdate(sub);
        persist(sub);
    }

    private void checkNotNull(String eventSource, MailSubscription sub) {
        ExtendedAssert.notFound(sub, "Can not find subscription on source: " + eventSource);
    }

    @ReConfigObject
    public MailNotificationsConfigObject getConfigObject() {
        MailNotificationsConfigObject co = new MailNotificationsConfigObject();
        co.setSubscriptions(this.subs.values().stream().collect(Collectors.toList()));
        return co;
    }

    @ReConfigObject
    public void setConfigObject(MailNotificationsConfigObject co) {
        List<MailSubscription> subs = co.getSubscriptions();
        if(subs == null) {
            return;
        }
        subs.forEach((s) -> {
            this.subs.put(s.getEventSource(), s);
            onUpdate(s);
            persist(s);
        });
    }
}
