/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.common.kv;

import org.junit.Assert;
import org.junit.Test;

/**
 */
public class InMemoryKeyValueStorageTest {

    @Test
    public void test() {
        InMemoryKeyValueStorage kvs = new InMemoryKeyValueStorage();
        KvStorageEvent[] holder = new KvStorageEvent[1];
        kvs.subscriptions().subscribe(e -> {
            System.out.println(e);
            holder[0] = e;
        });

        kvs.set("/root", "rval");
        kvs.set("/root/23", "23");
        assertEvent(holder, KvStorageEvent.Crud.CREATE, "/root/23", "23");
        kvs.set("/root/23", "231");
        assertEvent(holder, KvStorageEvent.Crud.UPDATE, "/root/23", "231");

        kvs.set("/root/one/two/three", "something");
        kvs.delete("/root/one/two/three", null);
        assertEvent(holder, KvStorageEvent.Crud.DELETE, "/root/one/two/three", "something");
        kvs.deletedir("/root/one", null);
        assertEvent(holder, KvStorageEvent.Crud.DELETE, "/root/one", null);
    }

    private void assertEvent(KvStorageEvent[] holder, KvStorageEvent.Crud create, String key, String val) {
        KvStorageEvent e = holder[0];
        Assert.assertEquals(create, e.getAction());
        Assert.assertEquals(key, e.getKey());
        Assert.assertEquals(val, e.getValue());
    }
}