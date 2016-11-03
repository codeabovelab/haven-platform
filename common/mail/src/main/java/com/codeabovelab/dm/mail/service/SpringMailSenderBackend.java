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

import com.codeabovelab.dm.mail.dto.MailBody;
import com.codeabovelab.dm.mail.dto.MailHead;
import com.codeabovelab.dm.mail.dto.MailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default backend which used spring MailSender.
 */
@ConditionalOnBean(MailSender.class)
@Component
public class SpringMailSenderBackend implements MailSenderBackend {

    private static final Logger LOG = LoggerFactory.getLogger(SpringMailSenderBackend.class);

    private final MailSender mailSender;

    @Autowired
    public SpringMailSenderBackend(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(MailMessage message) throws MailSenderException {
        SimpleMailMessage smm = new SimpleMailMessage();
        MailBody body = message.getBody();
        smm.setText(MailUtils.toPlainText(body));
        MailHead head = message.getHead();
        smm.setFrom(head.getFrom());
        smm.setReplyTo(head.getReplyTo());
        smm.setSubject(head.getSubject());
        smm.setTo(asArray(head.getTo()));
        smm.setCc(asArray(head.getCc()));
        smm.setBcc(asArray(head.getBcc()));
        smm.setSentDate(head.getSentDate());
        LOG.info("message to send {}", smm);
        mailSender.send(smm);
    }

    private String[] asArray(List<String> to) {
        return to.toArray(new String[to.size()]);
    }
}
