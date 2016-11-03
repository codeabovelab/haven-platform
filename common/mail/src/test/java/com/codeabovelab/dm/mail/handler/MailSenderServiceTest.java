package com.codeabovelab.dm.mail.handler;

import com.codeabovelab.dm.mail.configuration.MailServiceConfiguration;
import com.codeabovelab.dm.mail.dto.MailHeadImpl;
import com.codeabovelab.dm.mail.dto.MailMessageImpl;
import com.codeabovelab.dm.mail.dto.MailTextBody;
import com.codeabovelab.dm.mail.service.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSender;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MailSenderServiceTest.TestConfiguration.class)
public class MailSenderServiceTest {

    @EnableAutoConfiguration
    @Configuration
    @Import(MailServiceConfiguration.class)
    public static class TestConfiguration {
        @Bean
        MailSender mailSender() {
            return mock(MailSender.class);
        }
    }

    @Autowired
    MailSenderService service;

    @Test
    public void test() {
        MailMessageImpl.Builder b = MailMessageImpl.builder();
        b.setHead(MailHeadImpl.builder()
          .from("test@test.te")
          .subject("test")
          .to(Arrays.asList("user@test.te")));
        b.setBody(new MailTextBody("testtesttest"));
        service.send(b.build(), null);
    }

}