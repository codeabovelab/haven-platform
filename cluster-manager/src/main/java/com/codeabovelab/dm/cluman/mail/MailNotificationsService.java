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
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.codeabovelab.dm.common.mb.SmartConsumer;
import com.codeabovelab.dm.common.mb.Subscriptions;
import com.codeabovelab.dm.common.security.ExtendedUserDetails;
import com.codeabovelab.dm.common.security.UserIdentifiersDetailsService;
import com.codeabovelab.dm.mail.dto.*;
import com.codeabovelab.dm.mail.service.SendMailWithTemplateService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@ReConfigurable
@Component
public class MailNotificationsService {

    private static final String DEFAULT_TEMPLATE = "res:eventAlert";
    private final Map<String, List<MailSubscription>> map = new ConcurrentHashMap<>();
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
    private final UserIdentifiersDetailsService userDetailsService;
    private KvClassMapper<MailSubscription.Builder> classMapper;

    @Autowired
    public MailNotificationsService(MailConfiguration.MailProperties props,
                                    UserIdentifiersDetailsService userDetailsService,
                                    KvMapperFactory kvMapperFactory,
                                    ObjectPrinterFactory objectPrinterFactory,
                                    SendMailWithTemplateService sendMailService,
                                    Map<String, Subscriptions<?>> sources) {
        this.objectPrinterFactory = objectPrinterFactory;
        this.kvMapperFactory = kvMapperFactory;
        this.from = props.getFrom();
        this.sendMailService = sendMailService;
        this.userDetailsService = userDetailsService;
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
        List<MailSubscription> subs = this.map.get(eventSource);
        if(subs == null) {
            return;
        }
        subs.forEach(sub -> {
            processSubscription(eventSource, ev, sub);
        });
    }

    private void processSubscription(String eventSource, Object ev, MailSubscription sub) {
        Severity severity = ((WithSeverity) ev).getSeverity();
        //compare that subs severity is greater and we need skip this event
        if(sub.getSeverity().compareTo(severity) > 0) {
            return;
        }
        String templateUri = sub.getTemplate();
        if(!StringUtils.hasText(templateUri)) {
            templateUri = DEFAULT_TEMPLATE;
        }
        String user = sub.getUser();
        ExtendedUserDetails eud = userDetailsService.loadUserByUsername(user);
        if(eud == null) {
            log.error("Can not sent notification to {}, due it does not exists.", user);
            return;
        }
        String email = eud.getEmail();
        if(email == null) {
            log.error("Can not sent notification to {}, due it does not have an email.", user);
            return;
        }
        Object var = objectPrinterFactory.printer(ev);
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


    public void forEach(Consumer<MailSubscription> consumer) {
        map.forEach((k, subs) -> subs.forEach(consumer));
    }

    /**
     * List of subscriptions on concrete source.
     * @param eventSource source
     * @return list or null
     */
    public List<MailSubscription> get(String eventSource) {
        List<MailSubscription> list = map.get(eventSource);
        if(list == null) {
            return null;
        }
        return ImmutableList.copyOf(list);
    }

    public void put(MailSubscription newSub) {
        registerInternal(newSub);
        persist(newSub);
    }

    public void remove(String eventSource, String user) {
        List<MailSubscription> subs = this.map.get(eventSource);
        subs.removeIf(ms -> ms.getUser().equalsIgnoreCase(user));
    }


    private void persist(MailSubscription sub) {
        //persist
        classMapper.save(sub.getId(), MailSubscription.builder().from(sub));
    }

    /**
     * This method does not allow rewrite existed subscription, and return registered value.
     * @param newSub subscription
     */
    private void registerInternal(MailSubscription newSub) {
        String eventSource = newSub.getEventSource();
        ExtendedAssert.matchId(eventSource, "event source");
        Assert.notNull(newSub.getUser(), "user is null in mail subscription");
        List<MailSubscription> subs = this.map.computeIfAbsent(eventSource, (es) -> new ArrayList<>());
        boolean replaced = false;
        for(int i = 0; i < subs.size(); ++i) {
            MailSubscription sub = subs.get(i);
            if(!sub.getId().equals(newSub.getId())) {
                continue;
            }
            replaced = true;
            subs.set(i, newSub);
        }
        if(!replaced) {
            subs.add(newSub);
        }
        onUpdate(newSub);
    }

    private void onUpdate(MailSubscription newSub) {
        log.info("register or update subscription: {}", newSub);
    }

    private void checkNotNull(String eventSource, MailSubscription sub) {
        ExtendedAssert.notFound(sub, "Can not find subscription on source: " + eventSource);
    }

    @ReConfigObject
    public MailNotificationsConfigObject getConfigObject() {
        MailNotificationsConfigObject co = new MailNotificationsConfigObject();
        ArrayList<MailSubscription> list = new ArrayList<>();
        forEach(list::add);
        co.setSubscriptions(list);
        return co;
    }

    @ReConfigObject
    public void setConfigObject(MailNotificationsConfigObject co) {
        List<MailSubscription> subs = co.getSubscriptions();
        if(subs == null) {
            return;
        }
        subs.forEach((s) -> put(s));
    }
}
