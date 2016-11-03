package com.codeabovelab.dm.common.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;
import org.springframework.util.MimeTypeUtils;

import static org.junit.Assert.assertEquals;

/**
 */
public class DmJacksonModuleTest {

    @Test
    public void test() throws Exception {
        ObjectMapper mapper = createMapper();
        SomeStrangeBean bean = new SomeStrangeBean();
        bean.getKeeperProp().accept("new val");
        bean.setMimeType(MimeTypeUtils.APPLICATION_FORM_URLENCODED);
        String s = mapper.writeValueAsString(bean);
        System.out.println(s);
        SomeStrangeBean readed = mapper.readValue(s, SomeStrangeBean.class);
        assertEquals(bean, readed);
    }

    private ObjectMapper createMapper() {
        return new ObjectMapper()
          .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .registerModules(new DmJacksonModule());
    }
}
