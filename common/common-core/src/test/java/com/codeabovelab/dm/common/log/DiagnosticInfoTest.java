package com.codeabovelab.dm.common.log;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DiagnosticInfoTest.Config.class)
public class DiagnosticInfoTest {

    @Configuration
    @ComponentScan(basePackageClasses = DiagnosticInfo.class,
            // we need only one bean
            resourcePattern = "**/DiagnosticInfo.class")
    public static class Config {
        @Bean
        UUIDGenerator uuidGenerator() {
            return new RandomUUIDGenerator();
        }
    }


    @Autowired
    DiagnosticInfo diagnosticInfo;

    @Test
    public void test() throws Exception {
        try (AutoCloseable ctx = diagnosticInfo.injectToContext(UUID.randomUUID().toString(), null)) {
            String object = MDC.get(DiagnosticInfo.REQUEST_UUID);
            System.out.println(DiagnosticInfo.REQUEST_UUID + ":\t" + object);
            Assert.assertNotNull(object);
            //some properties may be null therefore we cannot test it for null
        }
    }
}