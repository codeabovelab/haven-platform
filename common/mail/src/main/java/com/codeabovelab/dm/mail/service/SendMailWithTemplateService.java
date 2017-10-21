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
import com.codeabovelab.dm.mail.dto.*;
import com.codeabovelab.dm.mail.template.MailSourceProcessorService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 */
@Component
@AllArgsConstructor
public class SendMailWithTemplateService {
    private static final Logger LOG = LoggerFactory.getLogger(SendMailWithTemplateService.class);

    private final MailSenderService senderService;
    private final MailSourceProcessorService processorService;


    public void send(MailSource source, Consumer<MailSendResult> consumer) {
        MailMessage message = processorService.process(source);
        try {
            senderService.send(message, consumer);
        } catch (Exception e) {
            MailSendResult.Builder result = MailSendResult.builder();
            result.setHead(message.getHead());
            LOG.error("On " + source + " message, we get error.", e);
            result.setStatus(MailStatus.UNKNOWN_FAIL);
            result.setError(Throwables.printToString(e));
            consumer.accept(result.build());
        }
    }
}
