package com.codeabovelab.dm.cluman.reconfig;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 */
@Component
@ReConfigurable
class SampleBean {

    @Data
    public static final class SomeData {
        private List<Object> objects;
        private String str;
        private int num;


    }

    public SampleBean() {
        data.setObjects(Arrays.asList("one", 2, 42));
        data.setNum(87);
        data.setStr("tttest");
    }

    @ReConfigObject
    private final SomeData data = new SomeData();
    private Object setted;

    @ReConfigObject
    private void set(Object o) {
        this.setted = o;
    }

    void check() {
        assertEquals(data, setted);
    }
}
