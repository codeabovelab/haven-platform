package com.codeabovelab.dm.mail.handler;

import com.codeabovelab.dm.mail.configuration.MailServiceConfiguration;
import com.codeabovelab.dm.mail.dto.MailHead;
import com.codeabovelab.dm.mail.dto.MailMessage;
import com.codeabovelab.dm.mail.dto.MailSourceImpl;
import com.codeabovelab.dm.mail.dto.MailTextBody;
import com.codeabovelab.dm.mail.service.MailSenderService;
import com.codeabovelab.dm.mail.template.MailSourceProcessorService;
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TemplateTest.TestConfiguration.class)
public class TemplateTest {

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

    @Autowired
    MailSourceProcessorService sourceProcessorService;


    @Test
    public void testTemplate() {
        final String name = "Tester";
        final String surname = "Testerov";
        final String patronymic = "Testerovich";
        final String email = "test@test.te";
        MailSourceImpl source = MailSourceImpl.builder()
          .templateUri("res:welcome")
          .addVariable("user", new UserObject(name, surname, patronymic, email))
          .build();

        MailMessage message = sourceProcessorService.process(source);
        String text = ((MailTextBody) message.getBody()).getText();
        System.out.println("Filled template:\n" + text);
        assertThat(text, containsString(name));
        MailHead head = message.getHead();
        assertThat(head.getSubject(), containsString(name));
        assertThat(head.getTo(), contains(is(email)));

    }

    public static class UserObject {
        private String name;
        private String surname;
        private String patronymic;
        private String email;

        public UserObject(String name, String surname, String patronymic, String email) {
            this.name = name;
            this.surname = surname;
            this.patronymic = patronymic;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSurname() {
            return surname;
        }

        public void setSurname(String surname) {
            this.surname = surname;
        }

        public String getPatronymic() {
            return patronymic;
        }

        public void setPatronymic(String patronymic) {
            this.patronymic = patronymic;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
