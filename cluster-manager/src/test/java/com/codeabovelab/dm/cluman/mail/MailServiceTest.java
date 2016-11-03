package com.codeabovelab.dm.cluman.mail;

import com.codeabovelab.dm.cluman.cluster.registry.RegistryEvent;
import com.codeabovelab.dm.common.kv.InMemoryKeyValueStorage;
import com.codeabovelab.dm.common.kv.KeyValueStorage;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.objprinter.ObjectPrinterFactory;
import com.codeabovelab.dm.common.json.JacksonUtils;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.codeabovelab.dm.common.mb.MessageBuses;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.validation.Validation;
import javax.validation.Validator;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import java.util.Arrays;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(classes = MailServiceTest.Config.class)
public class MailServiceTest {

    private static final String EVENT_SOURCE = "eventSource.test";
    private static final String RECIPIENT_FIRST = "me@localhost";
    private static final String RECIPIENT_SECOND = "cat@carpet";

    @Import(MailConfiguration.class)
    @ComponentScan(basePackageClasses = {ObjectPrinterFactory.class, KvMapperFactory.class})
    @Configuration
    public static class Config {

        @Bean(name = EVENT_SOURCE)
        MessageBus<WithSeverity> bus() {
            return MessageBuses.create(EVENT_SOURCE, WithSeverity.class);
        }

        @Bean
        MailSender mailSender() {
            return new TestMailSender();
        }

        @Bean
        ObjectMapper objectMapper() {
            return JacksonUtils.objectMapperBuilder();
        }

        @Bean
        FormattingConversionService conversionService() {
            return new DefaultFormattingConversionService();
        }

        @Bean
        KeyValueStorage keyValueStorage() {
            return new InMemoryKeyValueStorage();
        }

        @Bean
        TextEncryptor textEncryptor() {
            return new TextEncryptor() {
                @Override
                public String encrypt(String text) { return text; }

                @Override
                public String decrypt(String encryptedText) { return encryptedText; }
            };
        }

        @Bean
        Validator validator() {
            return Validation.buildDefaultValidatorFactory().getValidator();
        }
    }

    @Autowired
    private MailNotificationsService mailService;

    @Autowired
    private TestMailSender mailSender;

    @Qualifier(EVENT_SOURCE)
    @Autowired
    private MessageBus<WithSeverity> bus;

    @Test
    public void testAdding() {
        assertThat(mailService.list(), hasSize(0));

        MailSubscription.Builder msb = MailSubscription.builder();
        msb.setEventSource(EVENT_SOURCE);
        msb.addEmailRecipient(RECIPIENT_FIRST);
        mailService.put(msb.build());
        assertThat(mailService.list(), hasItem(allOf(
          hasProperty("eventSource", is(EVENT_SOURCE)),
          hasProperty("emailRecipients", hasItem(RECIPIENT_FIRST))
        )));

        try {
            mailService.addSubscribers("nothing", Arrays.asList("wrong@ufo"));
            fail("Adding to nonexistent subscription");
        } catch (NotFoundException e) {
            //as expected
        }

        mailService.addSubscribers(EVENT_SOURCE, Arrays.asList(RECIPIENT_SECOND));
        assertThat(mailService.list(), hasItem(allOf(
          hasProperty("eventSource", is(EVENT_SOURCE)),
          hasProperty("emailRecipients", hasItems(RECIPIENT_FIRST, RECIPIENT_SECOND))
        )));

        mailService.removeSubscribers(EVENT_SOURCE, Arrays.asList(RECIPIENT_SECOND));
        assertThat(mailService.list(), hasItem(allOf(
          hasProperty("eventSource", is(EVENT_SOURCE)),
          hasProperty("emailRecipients", hasItem(RECIPIENT_FIRST))
        )));

    }

    @Test
    public void testSend() throws Exception {
        assertThat(mailService.list(), hasSize(0));

        MailSubscription.Builder msb = MailSubscription.builder();
        msb.setEventSource(EVENT_SOURCE);
        msb.setSeverity(Severity.INFO);
        msb.setTemplate("res:eventAlert");
        msb.addEmailRecipient(RECIPIENT_FIRST);
        mailService.put(msb.build());

        bus.accept(RegistryEvent.builder()
          .action("alarm")
          .message("киррилица попала в реестр!")
          .severity(Severity.ERROR)
          .build());

        //TODO add sync mail receiving
        Thread.sleep(5000L);

        SimpleMailMessage message = mailSender.getMailbox(RECIPIENT_FIRST).get(0);
        System.out.println(message);
        assertNotNull(message);

    }

}
