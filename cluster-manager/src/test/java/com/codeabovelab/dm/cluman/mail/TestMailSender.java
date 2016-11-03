package com.codeabovelab.dm.cluman.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 */
@Slf4j
class TestMailSender implements MailSender {

    private final Map<String, List<SimpleMailMessage>> mailbox = new ConcurrentHashMap<>();

    @Override
    public void send(SimpleMailMessage msg) throws MailException {
        String[] tos = msg.getTo();
        for(String to: tos) {
            mailbox.computeIfAbsent(to, (k) -> new CopyOnWriteArrayList<>()).add(msg);
            log.info("Receive for {}, \n message: {} ", to, msg);
        }
    }

    @Override
    public void send(SimpleMailMessage... simpleMailMessages) throws MailException {
        for(SimpleMailMessage msg: simpleMailMessages) {
            send(msg);
        }
    }

    public List<SimpleMailMessage> getMailbox(String email) {
        List<SimpleMailMessage> msgs = mailbox.get(email);
        if(msgs == null) {
            return Collections.emptyList();
        }
        return msgs;
    }
}
