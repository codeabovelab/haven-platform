package com.codeabovelab.dm.cluman.ds.kv.etcd;

import com.codeabovelab.dm.common.kv.KeyValueStorage;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = EtcdClientWrapperTest.Config.class)
public class EtcdClientWrapperTest {

    @Autowired
    private KeyValueStorage etcdClientWrapper;

    @Test
    @Ignore
    public void test() throws Exception {
        final String key = "key";
        final String val = "val";
        etcdClientWrapper.set(key, val);
        String res = etcdClientWrapper.get(key).getValue();
        assertEquals(val, res);
        etcdClientWrapper.delete(key, null);
        String resNull = etcdClientWrapper.get(key).getValue();
        assertNull(resNull);


    }

    @Configuration
    @EnableAutoConfiguration
    @Import(EtcdConfiguration.class)
    public static class Config {

    }
}