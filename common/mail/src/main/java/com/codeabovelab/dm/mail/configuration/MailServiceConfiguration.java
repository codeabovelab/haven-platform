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

package com.codeabovelab.dm.mail.configuration;

import com.codeabovelab.dm.mail.service.MailSenderBackend;
import com.codeabovelab.dm.mail.service.MailSenderService;
import com.codeabovelab.dm.mail.service.SpringMailSenderBackend;
import com.codeabovelab.dm.mail.template.MailSourceProcessorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSender;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 */
@Configuration
@Import(MailTemplateConfiguration.class)
@ComponentScan(basePackageClasses = {
  MailSenderService.class,
  MailSourceProcessorService.class
})
public class MailServiceConfiguration {

    @Value("${mail.senderQueueCapacity:10}")
    private int queueCapacity;

    @Bean
    ExecutorService mailSenderExecutor() {
        return new ThreadPoolExecutor(2, 2,
          0L, TimeUnit.MILLISECONDS,
          new ArrayBlockingQueue<Runnable>(queueCapacity),
          new CustomizableThreadFactory("MailSender"));
    }

    @Bean
    MailSenderBackend mailSenderBackend(MailSender mailSender) {
        return new SpringMailSenderBackend(mailSender);
    }

}
