package com.codeabovelab.dm.mail.handler;

import com.google.common.util.concurrent.MoreExecutors;
import com.codeabovelab.dm.mail.dto.MailHeadImpl;
import com.codeabovelab.dm.mail.dto.MailMessage;
import com.codeabovelab.dm.mail.dto.MailMessageImpl;
import com.codeabovelab.dm.mail.dto.MailTextBody;
import com.codeabovelab.dm.mail.service.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.mail.MailSender;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.*;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FailureMailSenderTest.TestConfiguration.class)
public class FailureMailSenderTest {
    static MailSenderBackend mockOfFailureBackend;
    static MailSenderBackend mockOfGoodBackend;

    @Configuration
    public static class TestConfiguration {

        @Bean
        ExecutorService mailSenderExecutor() {
            return MoreExecutors.newDirectExecutorService();
        }

        @Bean
        MailSender mailSender() {
            return mock(MailSender.class);
        }

        @Bean
        MailSenderService mailSenderService(@Qualifier("mailSenderExecutor") ExecutorService executorService) {
            return new MailSenderService(executorService, Arrays.asList(mailSenderBackendFail(), mailSenderBackend()));
        }

        @Bean
        MailSenderBackend mailSenderBackendFail() {
            MailSenderBackend backend = mockOfFailureBackend = mock(MailSenderBackend.class, "failure backend");
            doThrow(new MailIOException("TEST")).when(backend).send(any(MailMessage.class));
            return backend;
        }

        @Bean
        MailSenderBackend mailSenderBackend() {
            MailSenderBackend backend = mockOfGoodBackend = mock(MailSenderBackend.class, "good backend");
            return backend;
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
        // there we call failure service, then good service
        service.send(b.build(), null);

        verify(mockOfFailureBackend, times(1)).send(any(MailMessage.class));

        // there we call only good service
        service.send(b.build(), null);

        verify(mockOfFailureBackend, times(1)).send(any(MailMessage.class));
        verify(mockOfGoodBackend, times(2)).send(any(MailMessage.class));
    }

}