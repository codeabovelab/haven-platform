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

package com.codeabovelab.dm.mail.service;

import com.codeabovelab.dm.common.utils.Throwables;
import com.codeabovelab.dm.mail.dto.MailMessage;
import com.codeabovelab.dm.mail.dto.MailSendResult;
import com.codeabovelab.dm.mail.dto.MailStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Service which do mail sending.
 */
@Component
public class MailSenderService {
    private static final Logger LOG = LoggerFactory.getLogger(MailSenderService.class);
    private final List<MailSenderBackend> backends;
    private final AtomicInteger goodBackend = new AtomicInteger(-1);
    private final ExecutorService executorService;

    @Autowired
    public MailSenderService(@Qualifier("mailSenderExecutor") ExecutorService executorService,
                             List<MailSenderBackend> backends) {
        this.executorService = executorService;
        this.backends = Collections.unmodifiableList(new ArrayList<>(backends));
    }

    /**
     * Put message into deliver queue.
     * @param message
     */
    public void send(MailMessage message, Consumer<MailSendResult> sendResultCallback) {
        executorService.submit(new SendMessageTask(message, sendResultCallback));
    }

    private class SendMessageTask implements Runnable {
        private final MailMessage message;
        private final Consumer<MailSendResult> sendResultCallback;

        public SendMessageTask(MailMessage message, Consumer<MailSendResult> sendResultCallback) {
            this.message = message;
            this.sendResultCallback = sendResultCallback;
        }

        @Override
        public void run() {
            try {
                MailSendResult.Builder builder = MailSendResult.builder();
                builder.setHead(message.getHead());
                try {
                    trySend();
                    builder.setStatus(MailStatus.OK);
                } catch (Exception e) {
                    LOG.error("On " + message, e);
                    builder.setStatus(MailStatus.UNKNOWN_FAIL);
                    builder.setError(Throwables.printToString(e));
                }
                MailSendResult result = builder.build();
                if(sendResultCallback != null) {
                    sendResultCallback.accept(result);
                }
            } catch (Exception e) {
                LOG.error("On " + message, e);
            }
        }

        private void trySend() {
            // there we must do choose of appropriate backend, retry on fails, and choose alternative backend if has server fault
            final int localGoodBackend = goodBackend.get();
            if(localGoodBackend >= 0) {
                MailSenderBackend backend = backends.get(localGoodBackend);
                try {
                    backend.send(message);
                    // if send is ok, then we no need more tries
                    return;
                } catch (MailIOException | MailServerFaultException e) {
                    // if server or connection error,
                    // and no more servers then throw error
                    if(backends.size() < 1) {
                        throw e;
                    }
                    LOG.error("Error with good backend '" + backend + "' " , e);
                    // else we must try with other server
                }
            }
            for(int i = 0; i < backends.size(); ++i) {
                MailSenderBackend backend = backends.get(i);
                try {
                    backend.send(message);
                    // i believe, that if other thread choose another server, there we do not need to rewrite it
                    goodBackend.compareAndSet(localGoodBackend, i);
                    // if send is ok, then we no need more tries
                    break;
                } catch (MailIOException | MailServerFaultException e) {
                    if(i + 1 >= backends.size()) {
                        throw e;
                    }
                    LOG.error("Error with backend '" + backend + "' " , e);
                    // if server or connection error, thew we can try with other server
                }
            }
        }
    }
}
