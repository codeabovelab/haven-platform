package com.codeabovelab.dm.common.format;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.Formatter;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.text.ParseException;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@EnableAutoConfiguration
@SpringBootTest(classes = {MetatypeFormatterRegistryConfiguration.class, MetatypeFormatterRegistryConfigurationTest.TestConfiguration.class})
public class MetatypeFormatterRegistryConfigurationTest {

    @Autowired
    private MetatypeFormatterRegistry metatypeFormatterRegistry;

    @Configuration
    public static class TestConfiguration {

        @Bean
        ValidableFormatter<Long> phoneValidableFormatter() {
            return new ValidableFormatter<>(new PhoneFormatter(), null, arg -> {
                if (!arg.matches("^[^\\d]*1[^\\d]*[38].*")) {
                    throw new FormatterException("'" + arg + "' must start with 13 or 18");
                }
            });
        }
    }


    @Test
    public void test() throws ParseException {
        Formatter<Long> formatter = metatypeFormatterRegistry.getFormatter(CommonMetatypeKeys.KEY_PHONE);
        for (Object[] phone : new Object[][]{
          {"+1 399 98-76-543", 13999876543L},
          {"18999876543", 18999876543L},
          {"139999999999999", 139999999999999L},
        }) {
            final String text = (String) phone[0];
            System.out.println("Parse: " + text);
            Long result = FormatterUtils.parse(formatter, text);
            assertEquals(phone[1], result);
            String str = FormatterUtils.print(formatter, result);
            //we cannot compare strings because its different
            assertNotNull(str);
            result = FormatterUtils.parse(formatter, text);
            // but can do check that print-parse cycle is correct
            assertEquals(phone[1], result);
        }

        for (String phone : new String[]{
          "+1 399 *-76-543",
          "8999876543",
          "13999999999999999",
        }) {
            try {
                System.out.println("Parse: " + phone);
                formatter.parse(phone, null);
                fail();
            } catch (FormatterException e) {
                // is ok
                System.out.println(e.getMessage());
            }
        }
    }
}