package com.codeabovelab.dm.cluman.reconfig;

import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.common.json.JacksonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.MimeTypeUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class ReConfigurableBeanTest {

    @Configuration
    @ComponentScan(basePackageClasses = AppConfigService.class)
    public static class Config {
        @Bean
        ObjectMapper objectMapper() {
            return JacksonUtils.objectMapperBuilder();
        }

        @Bean
        KvMapperFactory kvMapperFactory() {
            return Mockito.mock(KvMapperFactory.class);
        }
    }

    @Autowired
    private AppConfigService service;

    @Autowired
    private SampleBean sampleBean;

    @Test
    public void test() throws Exception {
        String config;
        try(StringWriter sw = new StringWriter();
            OutputStream osw = new WriterOutputStream(sw, StandardCharsets.UTF_8)) {
            service.write(MimeTypeUtils.APPLICATION_JSON_VALUE, osw);
            config = sw.toString();
        }
        System.out.println(config);
        try (StringReader sr = new StringReader(config);
             InputStream is = new ReaderInputStream(sr, StandardCharsets.UTF_8)) {
            service.read(MimeTypeUtils.APPLICATION_JSON_VALUE, is);
        }
        sampleBean.check();
    }

    private ConfigWriteContext makeContext() {
        return ConfigWriteContext.builder().build();
    }
}