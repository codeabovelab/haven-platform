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

package com.codeabovelab.dm.cluman.ui.msg;

import com.codeabovelab.dm.cluman.cluster.filter.FilterFactory;
import com.codeabovelab.dm.cluman.model.EventWithTime;
import com.codeabovelab.dm.cluman.persistent.PersistentBusFactory;
import com.codeabovelab.dm.cluman.ui.model.UiError;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.codeabovelab.dm.common.fc.FbQueue;
import com.codeabovelab.dm.common.mb.Subscriptions;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 */
@AllArgsConstructor(onConstructor = @__(@Autowired))
@ResponseBody
@RequestMapping(value = "/ui/api/events/", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
@Controller
public class EventController {

    static final String SUBSCRIPTIONS_GET = "/subscriptions/get";
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final SessionSubscriptions subscriptions;
    private final EventSources sources;
    private final FilterFactory filterFactory;

    @MessageMapping(SUBSCRIPTIONS_GET)
    @SendToUser(broadcast = false)
    public Collection<String> listSubs() {
        return subscriptions.getIds();
    }

    @MessageMapping("/subscriptions/add")
    /* do no use List<> here due it not support deserialization from string, which is need for back capability */
    public void addSub(UiAddSubscription[] uases) {
        //we save absent subscriptions into array for prevent multiple log records
        List<String> absent = new ArrayList<>(0/*usually is no absent ids*/);
        for(UiAddSubscription uas: uases) {
            String id = uas.getSource();
            Subscriptions<?> subs = sources.get(id);
            if(subs == null) {
                absent.add(id);
                continue;
            }
            subscriptions.subscribe(uas, subs);
        }
        if(!absent.isEmpty()) {
            log.warn("Can not find subscriptions with ids: {}", absent);
        }
    }

    @MessageMapping("/subscriptions/del")
    public void delSub(List<String> ids) {
        ids.forEach(subscriptions::unsubscribe);
    }


    @MessageMapping("/subscriptions/available")
    @SendToUser(broadcast = false)
    public Collection<String> listAvailable() {
        return sources.list();
    }

    @ApiOperation("You can get list of sourcing using STOMP: /subscriptions/available, see /ui/stomp.html")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public Collection<String> listAvailableRest() {
        return sources.list();
    }

    @MessageExceptionHandler
    public UiError onException(Message message, Exception ex) {
        log.error("On message: {}" , SimpMessageHeaderAccessor.wrap(message).getDestination(), message, ex);
        return UiError.from(ex);
    }

    @ApiOperation("Count of elements in specified events source since specified time (24 hours by default)." +
      " Note that not all sources have persisted store, these sources do not support getting count.")
    @RequestMapping(value = "/{source:.*}/count", method = RequestMethod.GET)
    public UiCountResult countOfLastEvents(@PathVariable("source") String source,
                                           @RequestParam(name = "filter", required = false) List<String> filtersSrc,
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                           @RequestParam(name = "from", required = false) LocalDateTime from) {
        Subscriptions<?> subs = sources.get(source);
        ExtendedAssert.notFound(subs, "Can not find Subscriptions: '" + source + "'");
        PersistentBusFactory.PersistentBus<?> pb = subs.getExtension(PersistentBusFactory.EXT_KEY);
        ExtendedAssert.notFound(pb, "Can not find persisted queue: '" + source + "'");
        List<FilterCollector> collectors = new ArrayList<>();
        if(filtersSrc != null) {
            filtersSrc.forEach((src) -> collectors.add(new FilterCollector(filterFactory, src)));
        }
        if(from == null) {
            from = LocalDateTime.now().minusDays(1);
        }
        long fromMillis = from.toEpochSecond(ZoneOffset.UTC);
        FbQueue<?> q = pb.getQueue();
        Iterator<?> iter = q.iterator();
        int i = 0;
        while(iter.hasNext()) {
            Object next = iter.next();
            if(!(next instanceof EventWithTime)) {
                continue;
            }
            long millis = ((EventWithTime) next).getTimeInMilliseconds();
            if(millis < fromMillis) {
                continue;
            }
            collectors.forEach(fc -> fc.collect(next));
            i++;
        }
        UiCountResult res = new UiCountResult();
        res.setFiltered(collectors.stream().map(FilterCollector::toUi).collect(Collectors.toList()));
        res.setSource(source);
        res.setCount(i);
        res.setFrom(from);
        return res;
    }
}
